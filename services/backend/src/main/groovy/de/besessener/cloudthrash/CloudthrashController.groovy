package de.besessener.cloudthrash

import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.api.model.batch.v1.*
import io.fabric8.kubernetes.api.model.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.Executors

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api")
class CloudthrashController {

    private enum Status { IDLE, RUNNING, STOPPING, ERROR }
    private String errorMessage = ""
    private volatile Status status = Status.IDLE

    private static final String NAMESPACE = "default"
    private final executorService = Executors.newCachedThreadPool()
    private final KubernetesClient client = new KubernetesClientBuilder().build()

    @PostMapping("/start")
    ResponseEntity<Map<String, String>> start() {
        if (status != Status.IDLE) return badRequest("Cannot start while not idle")

        status = Status.RUNNING
        executorService.submit {
            def job = createK6Job()
            waitForJobCompletion(job)
            deleteAllK6Jobs()
            status = Status.IDLE
        }

        okResponse("K6 test started...")
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
                .list()
                .items
                .each { job ->
                    println "Deleting job: ${job.metadata.name}"
                    client.batch().v1().jobs()
                        .inNamespace(NAMESPACE)
                        .withName(job.metadata.name)
                        .delete()
                }

            println "All cloudthrash jobs stopped."
        } catch (Exception e) {
            handleError("Error deleting Kubernetes jobs", e)
        }
    }

    @GetMapping("/status")
    ResponseEntity<Map<String, String>> getStatus() {
        status == Status.ERROR ? failedRequest() : okResponse(status.name())
    }

    private Job createK6Job() {
        try {
            def jobName = "cloudthrash-job-${System.currentTimeMillis()}"

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
                                .addNewEnv()
                                    .withName("K6_INFLUXDB_TOKEN")
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef()
                                            .withName("cloudthrash-secrets")
                                            .withKey("INFLUXDB_API_TOKEN")
                                        .endSecretKeyRef()
                                    .endValueFrom()
                                .endEnv()
                            .endContainer()
                            .withRestartPolicy("Never")
                        .endSpec()
                    .endTemplate()
                    .withBackoffLimit(0)
                .endSpec()
                .build()

            println "Job object before creation: $job"
            def createdJob = client.batch().v1().jobs().inNamespace(NAMESPACE).create(job)
            println "Job created in Kubernetes: $createdJob"
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
            def updatedJob = client.batch().v1().jobs()
                .inNamespace(NAMESPACE)
                .withName(jobName)
                .get()

            if (!updatedJob) {
                println "Job $jobName not found. Assuming it was deleted or completed."
                return
            }

            def jobStatus = updatedJob.status
            if (jobStatus?.succeeded > 0) {
                println "Job $jobName completed successfully!"
                return
            }
            if (jobStatus?.failed > 0) {
                println "Job $jobName failed!"
                return
            }

            println "Job $jobName still running..."
            sleep(5000)
        }
    }

    private void handleError(String message, Exception e = null) {
        status = Status.ERROR
        errorMessage = e ? "$message: ${e.message}" : message
        println errorMessage
        e?.printStackTrace()
    }

    private ResponseEntity<Map<String, String>> okResponse(String message) {
        ResponseEntity.ok([message: message, status: status.name()])
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        ResponseEntity.badRequest().body([message: message, status: status.name()])
    }

    private ResponseEntity<Map<String, String>> failedRequest() {
        def response = ResponseEntity.status(500).body([message: errorMessage, status: status.name()])
        status = Status.IDLE
        errorMessage = ""
        response
    }
}
