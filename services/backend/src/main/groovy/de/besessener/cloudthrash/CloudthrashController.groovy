package de.besessener.cloudthrash

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.*
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.web.multipart.MultipartFile
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.HttpStatus
import java.io.InputStream
import java.io.IOException
import java.util.concurrent.Executors

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api")
class CloudthrashController {

    private static final Logger log = LoggerFactory.getLogger(CloudthrashController)

    private enum Status { IDLE, RUNNING, STOPPING, ERROR }

    private volatile Status status = Status.IDLE
    private String errorMessage = ""

    private static final String NAMESPACE = "default"
    private final executorService = Executors.newCachedThreadPool()
    private final KubernetesClient client = new KubernetesClientBuilder().build()

    @Autowired
    MinioService minioService

    @PostMapping("/upload")
    ResponseEntity<Map> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            minioService.uploadFile(file.originalFilename, file.inputStream)
            return buildResponse(HttpStatus.OK, "Upload successful")
        } catch (IOException e) {
            return buildResponse(HttpStatus.BAD_REQUEST, "Error uploading file: ${e.message}")
        }
    }

    @GetMapping("/download")
    ResponseEntity<?> downloadFile(@RequestParam("fileName") String fileName) {
        try {
            InputStream fileStream = minioService.downloadFile(fileName)
            if (!fileStream) {
                return buildResponse(HttpStatus.NOT_FOUND, "File not found: $fileName")
            }

            def resource = new ByteArrayResource(fileStream.readAllBytes())
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$fileName")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource)
        } catch (IOException e) {
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error downloading file: ${e.message}")
        }
    }

    @DeleteMapping("/delete")
    ResponseEntity<Map> deleteFile(@RequestParam("fileName") String fileName) {
        try {
            minioService.deleteFile(fileName)
            return buildResponse(HttpStatus.OK, "File deleted successfully")
        } catch (FileNotFoundException e) {
            return buildResponse(HttpStatus.NOT_FOUND, "File not found: ${e.message}")
        } catch (Exception e) {
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting file: ${e.message}")
        }
    }

    @PostMapping("/start")
    ResponseEntity<Map> start(@RequestBody Map<String, Object> payload) {
        if (status != Status.IDLE) return buildResponse(HttpStatus.BAD_REQUEST, "Cannot start while not idle")

        def fileList = listFiles()
        if (fileList.isEmpty() || !fileList.contains('test.js')) return buildResponse(HttpStatus.BAD_REQUEST, "A 'test.js' file is required")

        executorService.submit {
            try {
                def (cpu, memory, loadAgents) = [payload.cpu as String, payload.memory as String, payload.loadAgents as Integer]
                def envVars = (payload.envVars as List<Map<String, String>>).collect { new EnvVarBuilder().withName(it.name).withValue(it.value).build() }

                log.info "Starting K6 test with $loadAgents agents (CPU: $cpu, Memory: $memory)"

                def futures = (1..loadAgents).collect { index ->
                    executorService.submit {
                        log.info "Starting K6 job for agent #$index"
                        def job = createK6Job(cpu, memory, envVars, index)
                        waitForJobCompletion(job)
                        log.info "K6 job for agent #$index completed"
                    }
                }

                executorService.submit {
                    futures*.get()
                    deleteAllK6Jobs()
                    status = Status.IDLE
                    log.info "All K6 jobs completed, status reset to IDLE"
                }
            } catch (Exception e) {
                handleError("Error starting K6 jobs", e)
            }
        }

        status = Status.RUNNING
        buildResponse(HttpStatus.OK, "K6 test started with ${payload.loadAgents} agents in parallel...")
    }

    @PostMapping("/stop")
    ResponseEntity<Map> stop() {
        if (status != Status.RUNNING) return buildResponse(HttpStatus.BAD_REQUEST, "Cannot stop while not running")

        status = Status.STOPPING
        executorService.submit {
            deleteAllK6Jobs()
            status = Status.IDLE
        }

        buildResponse(HttpStatus.OK, "Stopping all Kubernetes jobs...")
    }

    @GetMapping("/status")
    ResponseEntity<Map> getStatus() {
        return status == Status.ERROR ? 
            buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Service internal error: $errorMessage") : 
            buildResponse(HttpStatus.OK, status.name())
    }

    private Job createK6Job(String cpu, String memory, List<EnvVar> envVars, int index) {
        def jobName = "cloudthrash-job-${index}"
        log.info "Creating Job in Kubernetes: $jobName"

        def job = new JobBuilder()
                .withNewMetadata().withName(jobName).withNamespace(NAMESPACE).addToLabels("app", "cloudthrash-job").endMetadata()
                .withNewSpec().withNewTemplate().withNewMetadata().addToLabels("app", "cloudthrash-job").endMetadata()
                .withNewSpec().withServiceAccountName("cloudthrash-runner-sa")
                .addNewContainer().withName("cloudthrash").withImage("cloud-thrash/k6-test:latest").withImagePullPolicy("IfNotPresent")
                .withEnv(envVars)
                .addNewEnv().withName("K6_INSTANCE_ID").withValue("${jobName}").endEnv()
                .withResources(new ResourceRequirementsBuilder()
                        .addToLimits("cpu", new Quantity(cpu)).addToLimits("memory", new Quantity(memory))
                        .addToRequests("cpu", new Quantity(cpu)).addToRequests("memory", new Quantity(memory))
                        .build())
                .endContainer()
                .withRestartPolicy("Never").endSpec().endTemplate().withBackoffLimit(0).endSpec().build()

        client.batch().v1().jobs().inNamespace(NAMESPACE).create(job)
    }

    private void deleteAllK6Jobs() {
        client.batch().v1().jobs().inNamespace(NAMESPACE).withLabel("app", "cloudthrash-job").list().items.each {
            client.batch().v1().jobs().inNamespace(NAMESPACE).withName(it.metadata.name).delete()
        }
    }

    private void handleError(String message, Exception e = null) {
        status = Status.ERROR
        errorMessage = e ? "$message: ${e.message}" : message
        log.error(errorMessage, e)
    }

    private List listFiles() {
        try {
            return minioService.listFiles()
        } catch (Exception e) {
            return []
        }
    }

    private ResponseEntity<Map<String, String>> buildResponse(HttpStatus httpStatus, String message) {
        println minioService.listFiles()
        def responseContent = [message: message, httpStatus: httpStatus.reasonPhrase, status: status.name(), data: minioService.listFiles()]
        if (status == Status.ERROR) {
            status = Status.IDLE
            message = ""
        }
        
        ResponseEntity.status(httpStatus).body(responseContent)
    }
}
