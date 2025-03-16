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

    private enum Status { IDLE, RUNNING, STOPPING }

    private volatile Status status = Status.IDLE
    private static final String NAMESPACE = "default"
    private final executorService = Executors.newCachedThreadPool()
    private final KubernetesClient client = new KubernetesClientBuilder().build()

    @PostMapping("/start")
    ResponseEntity<Map<String, String>> start() {
        if (status != Status.IDLE) return badRequest("Cannot start while not idle")

        status = Status.RUNNING
        executorService.submit {
            createK6Job()
            status = Status.IDLE
        }

        return okResponse("K6 test started...")
    }

    @PostMapping("/stop")
    ResponseEntity<Map<String, String>> stop() {
        if (status != Status.RUNNING) return badRequest("Cannot stop while not running")

        status = Status.STOPPING
        executorService.submit {
            deleteAllK6Jobs()
            status = Status.IDLE
        }

        return okResponse("Stopping all Kubernetes jobs...")
    }

    private void deleteAllK6Jobs() {
        try {
            def jobs = client.batch().v1().jobs()
                .inNamespace(NAMESPACE)
                .withLabel("app", "cloudthrash-job")
                .list()

            jobs.items.each { job ->
                println "Deleting job: ${job.metadata.name}"
                client.batch().v1().jobs()
                    .inNamespace(NAMESPACE)
                    .withName(job.metadata.name)
                    .delete()
            }

            println "All cloudthrash jobs stopped."
        } catch (Exception e) {
            println "Error deleting Kubernetes jobs: ${e.message}"
        }
    }

    @GetMapping("/status")
    ResponseEntity<Map<String, String>> getStatus() {
        ResponseEntity.ok([status: status.name()])
    }

    private void createK6Job() {
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
                            .addNewContainer()
                                .withName("cloudthrash")
                                .withImage("cloud-thrash/k6-test:latest")
                                .withImagePullPolicy("IfNotPresent")
                            .endContainer()
                            .withRestartPolicy("Never")
                        .endSpec()
                    .endTemplate()
                    .withBackoffLimit(0)
                .endSpec()
                .build()

            client.batch().v1().jobs().inNamespace(NAMESPACE).create(job)
            println "Job Created: $jobName"
        } catch (Exception e) {
            println "Error creating Kubernetes Job: ${e.message}"
        } finally {
            status = Status.IDLE
        }
    }

    private ResponseEntity<Map<String, String>> okResponse(String message) {
        ResponseEntity.ok([message: message, status: status.name()])
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        ResponseEntity.badRequest().body([message: message, status: status.name()])
    }
}
