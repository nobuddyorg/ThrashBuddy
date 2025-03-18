package de.besessener.cloudthrash

import io.fabric8.kubernetes.api.model.*
import io.fabric8.kubernetes.api.model.batch.v1.*
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

import java.util.concurrent.Executors

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api")
class CloudthrashController {

    private static final Logger log = LoggerFactory.getLogger(CloudthrashController)
    private enum Status {
        IDLE, RUNNING, STOPPING, ERROR
    }
    private volatile Status status = Status.IDLE
    private String errorMessage = ""

    private static final String NAMESPACE = "default"
    private final executorService = Executors.newCachedThreadPool()
    private final KubernetesClient client = new KubernetesClientBuilder().build()

    @PostMapping("/start")
    ResponseEntity<Map<String, String>> start(@RequestBody Map<String, Object> payload) {
        if (status != Status.IDLE) return badRequest("Cannot start while not idle")

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
        okResponse("K6 test started with ${payload.loadAgents} agents in parallel...")
    }

    @PostMapping("/stop")
    ResponseEntity<Map<String, String>> stop() {
        if (status != Status.RUNNING) return badRequest("Cannot stop while not running")

        status = Status.STOPPING
        executorService.submit {
            deleteAllK6Jobs()
            status = Status.IDLE
        }

        okResponse("Stopping all Kubernetes jobs...")
    }

    private void deleteAllK6Jobs() {
        try {
            client.batch().v1().jobs()
                    .inNamespace(NAMESPACE)
                    .withLabel("app", "cloudthrash-job")
                    .list().items.each { job ->
                log.info "Deleting job: ${job.metadata.name}"
                client.batch().v1().jobs().inNamespace(NAMESPACE).withName(job.metadata.name).delete()
            }
            log.info "All cloudthrash jobs stopped."
        } catch (Exception e) {
            handleError("Error deleting Kubernetes jobs", e)
        }
    }

    @GetMapping("/status")
    ResponseEntity<Map<String, String>> getStatus() {
        status == Status.ERROR ? failedRequest() : okResponse(status.name())
    }

    private Job createK6Job(String cpu, String memory, List<EnvVar> envVars, int index) {
        try {
            def jobName = "cloudthrash-job-${index}"
            log.info "Creating Job in Kubernetes: $jobName"

            def job = new JobBuilder()
                    .withNewMetadata()
                    .withName(jobName)
                    .withNamespace(NAMESPACE)
                    .addToLabels("app", "cloudthrash-job")
                    .endMetadata()
                    .withNewSpec()
                    .withNewTemplate()
                    .withNewMetadata()
                    .addToLabels("app", "cloudthrash-job")
                    .endMetadata()
                    .withNewSpec()
                    .withServiceAccountName("cloudthrash-runner-sa")
                    .addNewContainer()
                    .withName("cloudthrash")
                    .withImage("cloud-thrash/k6-test:latest")
                    .withImagePullPolicy("IfNotPresent")
                    .withEnv(envVars)
                    .addNewEnv()
                    .withName("K6_INFLUXDB_TOKEN")
                    .withNewValueFrom()
                    .withNewSecretKeyRef()
                    .withName("cloudthrash-secrets")
                    .withKey("INFLUXDB_API_TOKEN")
                    .endSecretKeyRef()
                    .endValueFrom()
                    .endEnv()
                    .addNewEnv()
                    .withName("K6_INSTANCE_ID")
                    .withValue("${jobName}")
                    .endEnv()
                    .withResources(new ResourceRequirementsBuilder()
                            .addToLimits("cpu", new Quantity(cpu))
                            .addToLimits("memory", new Quantity(memory))
                            .addToRequests("cpu", new Quantity(cpu))
                            .addToRequests("memory", new Quantity(memory))
                            .build())
                    .endContainer()
                    .withRestartPolicy("Never")
                    .endSpec()
                    .endTemplate()
                    .withBackoffLimit(0)
                    .endSpec()
                    .build()

            def createdJob = client.batch().v1().jobs().inNamespace(NAMESPACE).create(job)
            log.info "Job created in Kubernetes: $jobName"
            return createdJob
        } catch (Exception e) {
            handleError("Error creating Kubernetes job", e)
            return null
        }
    }

    private void waitForJobCompletion(job) {
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
                return
            }
            if (jobStatus?.failed > 0) {
                log.error "Job $jobName failed!"
                return
            }

            log.debug "Job $jobName still running..."
            sleep(5000)
        }
    }

    private void handleError(String message, Exception e = null) {
        status = Status.ERROR
        errorMessage = e ? "$message: ${e.message}" : message
        log.error(errorMessage, e)
    }

    private ResponseEntity<Map<String, String>> okResponse(String message) {
        ResponseEntity.ok([message: message, status: status.name()])
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        ResponseEntity.badRequest().body([message: message, status: status.name()])
    }
}
