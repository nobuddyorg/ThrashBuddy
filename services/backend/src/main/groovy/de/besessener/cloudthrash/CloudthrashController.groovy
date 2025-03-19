package de.besessener.cloudthrash

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.*
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

import java.io.IOException
import java.io.InputStream
import java.util.concurrent.Executors

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api")
class CloudthrashController {

    private static final Logger log = LoggerFactory.getLogger(CloudthrashController)

    private enum Status {
        IDLE, RUNNING, STOPPING, ERROR, INIT
    }

    private volatile Status status = Status.INIT
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
        if (fileList.isEmpty() || !fileList.any { it.filename == 'test.js' }) return buildResponse(HttpStatus.BAD_REQUEST, "A 'test.js' file is required")

        executorService.submit {
            try {
                def (cpu, memory, loadAgents) = [payload.cpu as String, payload.memory as String, payload.loadAgents as Integer]
                def envVars = (payload.envVars as List<Map<String, String>>).collect { new EnvVarBuilder().withName(it.name).withValue(it.value).build() }

                log.info "Starting K6 test with $loadAgents agents (CPU: $cpu, Memory: $memory)"

                def statusList = []
                def futures = (1..loadAgents).collect { index ->
                    executorService.submit {
                        log.info "Starting K6 job for agent #$index"
                        def job = createK6Job(cpu, memory, envVars, index)
                        statusList << waitForJobCompletion(job)
                        log.info "K6 job for agent #$index finished"
                    }
                }

                executorService.submit {
                    futures*.get()
                    deleteAllK6Jobs()
                    if (statusList.any { Status.ERROR }) {
                        handleError("Error running K6 jobs")
                    } else {
                        status = Status.IDLE
                        log.info "All K6 jobs completed, status reset to IDLE"
                    }
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
        String jobName = "cloudthrash-job-${index}"
        log.info("Creating Job in Kubernetes: " + jobName)

        List<EnvVar> updatedEnvVars = new ArrayList<>(envVars)
        updatedEnvVars.add(new EnvVar("MINIO_URL", System.getenv("MINIO_URL"), null))
        updatedEnvVars.add(new EnvVar("MINIO_ACCESS_KEY", System.getenv("MINIO_ACCESS_KEY"), null))
        updatedEnvVars.add(new EnvVar("MINIO_SECRET_KEY", System.getenv("MINIO_SECRET_KEY"), null))
        updatedEnvVars.add(new EnvVar("MINIO_BUCKET", System.getenv("MINIO_BUCKET"), null))
        updatedEnvVars.add(new EnvVar("K6_INSTANCE_ID", jobName, null))
        updatedEnvVars.add(new EnvVar("K6_INFLUXDB_TOKEN", System.getenv("K6_INFLUXDB_TOKEN"), null))

        Job job = new JobBuilder()
                .withNewMetadata().withName(jobName).withNamespace(NAMESPACE).addToLabels("app", "cloudthrash-job").endMetadata()
                .withNewSpec().withNewTemplate().withNewMetadata().addToLabels("app", "cloudthrash-job").endMetadata()
                .withNewSpec().withServiceAccountName("cloudthrash-runner-sa")
                .addNewContainer().withName("cloudthrash").withImage("cloud-thrash/k6-test:latest").withImagePullPolicy("IfNotPresent")
                .withEnv(updatedEnvVars)
                .withResources(new ResourceRequirementsBuilder()
                        .addToLimits("cpu", new Quantity(cpu)).addToLimits("memory", new Quantity(memory))
                        .addToRequests("cpu", new Quantity(cpu)).addToRequests("memory", new Quantity(memory))
                        .build())
                .endContainer()
                .withRestartPolicy("Never").endSpec().endTemplate().withBackoffLimit(0).endSpec().build()

        client.batch().v1().jobs().inNamespace(NAMESPACE).create(job)
    }

    private Status waitForJobCompletion(job) {
        if (!job) {
            handleError("Error waiting for Kubernetes jobs: Job is null")
            return
        }

        def jobName = job.metadata.name

        while (true) {
            def updatedJob = client.batch().v1().jobs().inNamespace(NAMESPACE).withName(jobName).get()

            if (!updatedJob) {
                log.warn "Job $jobName not found. Assuming it was deleted or completed."
                return
            }

            def jobStatus = updatedJob.status
            if (jobStatus?.succeeded > 0) {
                log.info "Job $jobName completed successfully!"
                return Status.IDLE
            }
            if (jobStatus?.failed > 0) {
                handleError("Error running K6 job $jobName")
                return Status.ERROR
            }

            log.debug "Job $jobName still running..."
            sleep(5000)
        }
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
        def fileList = listFiles()
        def responseContent = [message: message, httpStatus: httpStatus.reasonPhrase, status: status.name(), data: fileList]
        if (status == Status.ERROR) {
            status = Status.IDLE
            message = ""
        }

        if (status == Status.IDLE && !fileList.any { it.filename == 'test.js' }) {
            status = Status.INIT
        } else if (status == Status.INIT && fileList.any { it.filename == 'test.js' }) {
            status = Status.IDLE
        }

        ResponseEntity.status(httpStatus).body(responseContent)
    }
}
