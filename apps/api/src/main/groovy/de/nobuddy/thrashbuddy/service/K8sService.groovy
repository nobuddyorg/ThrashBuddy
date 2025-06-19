package de.besessener.thrashbuddy.service

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Service
class K8sService {

    private static final log = LoggerFactory.getLogger(K8sService)
    private static final String NAMESPACE = "default"

    @Autowired
    KubernetesClient client

    @Autowired
    StatusService statusService

    def executor = Executors.newFixedThreadPool(100)

    K8sService(KubernetesClient client, StatusService statusService) {
        this.client = client
        this.statusService = statusService
    }

    @Async
    void executeJobs(String cpu, String memory, int count, List<EnvVar> envVars) {
        try {
            List<CompletableFuture<StatusService.ResponseStatus>> futures = (0..<count).collect { index ->
                CompletableFuture.supplyAsync({
                    return createK6Job(cpu, memory, envVars, index)
                }, executor)
            }

            CompletableFuture.allOf(*futures).join()
            def results = futures*.get()
            deleteAllK6Jobs()

            if (results.any { it == StatusService.ResponseStatus.ERROR }) {
                setError("One or more K6 jobs failed.")
            } else {
                statusService.setStatus(StatusService.ResponseStatus.IDLE)
                log.info("All jobs completed successfully.")
            }

        } catch (Exception e) {
            setError("Failed to start jobs", e)
        }
    }

    @Async
    void deleteAllK6Jobs() {
        if (client) {
            client.batch().v1().jobs().inNamespace(NAMESPACE)
                    .withLabel("app", "thrashbuddy-job")
                    .list()
                    .items
                    .each { job ->
                        client.batch().v1().jobs().inNamespace(NAMESPACE).withName(job.metadata.name).delete()
                    }
        }

        statusService.setStatus(StatusService.ResponseStatus.IDLE)
    }

    private StatusService.ResponseStatus createK6Job(String cpu, String memory, List<EnvVar> envVars, int index) {
        def jobName = "thrashbuddy-job-${index}"
        log.info("Creating Kubernetes Job: $jobName")

        def fullEnv = new ArrayList<>(envVars) + [
                new EnvVar("MINIO_URL", System.getenv("MINIO_URL"), null),
                new EnvVar("MINIO_ACCESS_KEY", System.getenv("MINIO_ACCESS_KEY"), null),
                new EnvVar("MINIO_SECRET_KEY", System.getenv("MINIO_SECRET_KEY"), null),
                new EnvVar("MINIO_BUCKET", System.getenv("MINIO_BUCKET"), null),
                new EnvVar("K6_INSTANCE_ID", jobName, null),
                new EnvVar("K6_INFLUXDB_TOKEN", System.getenv("K6_INFLUXDB_TOKEN"), null)
        ]

        def job = new JobBuilder()
                .withNewMetadata().withName(jobName).withNamespace(NAMESPACE).addToLabels("app", "thrashbuddy-job").endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewMetadata().addToLabels("app", "thrashbuddy-job").endMetadata()
                .withNewSpec()
                .withServiceAccountName("thrashbuddy-runner-sa")
                .addNewContainer()
                .withName("thrashbuddy")
                .withImage(System.getenv("IMAGE_REPO_PREFIX") + "thrash-buddy/k6-test:latest")
                .withImagePullPolicy("IfNotPresent")
                .withEnv(fullEnv)
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

        if (client) {
            client.batch().v1().jobs()
                    .inNamespace(NAMESPACE)
                    .resource(job)
                    .createOr(existing -> client.batch().v1().jobs()
                            .inNamespace(NAMESPACE)
                            .resource(job)
                            .update()
                    )
        }

        jobName = job?.metadata?.name
        while (true) {
            sleep(5000)
            def j
            if (client) {
                j = client.batch().v1().jobs().inNamespace(NAMESPACE).withName(jobName).get()
            }

            if (!j) {
                return StatusService.ResponseStatus.ERROR
            }

            if (j.status?.succeeded > 0) {
                return StatusService.ResponseStatus.IDLE
            }

            if (j.status?.failed > 0) {
                return StatusService.ResponseStatus.ERROR
            }
        }
    }

    private void setError(String msg, Exception e = null) {
        statusService.setStatus(StatusService.ResponseStatus.ERROR)
        statusService.setErrorMessage("$msg: ${e.message}")
        log.error(statusService.getErrorMessage(), e)
    }
}
