package org.nobuddy.thrashbuddy.service

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

import java.util.concurrent.TimeUnit

@Service
class K8sService {

    private static final log = LoggerFactory.getLogger(K8sService)
    private static final String NAMESPACE = System.getenv("NAMESPACE")
    private static final String JOB_NAME = "${System.getenv("APP_NAME")}-job"

    StatusService.ResponseStatus status = StatusService.ResponseStatus.IDLE
    String errorMessage = null

    @Autowired
    KubernetesClient client

    @Autowired
    StatusService statusService

    K8sService(KubernetesClient client, StatusService statusService) {
        this.client = client
        this.statusService = statusService
    }

    @Async("blockingExecutor")
    void start(String cpu, String memory, int count, List<EnvVar> envVars) {
        status = StatusService.ResponseStatus.RUNNING
        try {
            createK6Jobs(cpu, memory, envVars, count)
            monitorPodsAndStopIfLow(count)
            status = StatusService.ResponseStatus.IDLE
        } catch (Exception e) {
            status = StatusService.ResponseStatus.ERROR
            errorMessage = "Failed to start jobs: ${e.message}"
        }
    }

    @Async("blockingExecutor")
    void stop() {
        status = StatusService.ResponseStatus.STOPPING
        try {
            deleteK6Jobs()
            status = StatusService.ResponseStatus.IDLE
        } catch (Exception e) {
            status = StatusService.ResponseStatus.ERROR
            errorMessage = "Failed to stop jobs: ${e.message}"
        }
    }

    void createK6Jobs(String cpu, String memory, List<EnvVar> envVars, int parallelism) {
        log.info("Creating Kubernetes Job: $JOB_NAME")
        def fullEnv = new ArrayList<>(envVars) + [new EnvVar("MINIO_URL", System.getenv("MINIO_URL"), null),
                                                  new EnvVar("MINIO_ACCESS_KEY", System.getenv("MINIO_ACCESS_KEY"), null),
                                                  new EnvVar("MINIO_SECRET_KEY", System.getenv("MINIO_SECRET_KEY"), null),
                                                  new EnvVar("MINIO_BUCKET", System.getenv("MINIO_BUCKET"), null),
                                                  new EnvVar("K6_INSTANCE_ID", JOB_NAME, null)]

        def job = new JobBuilder()
                .withNewMetadata()
                .withName(JOB_NAME)
                .withNamespace(NAMESPACE)
                .addToLabels("app", JOB_NAME)
                .endMetadata()
                .withNewSpec()
                .withParallelism(parallelism)
                .withCompletions(parallelism)
                .withBackoffLimit(3)
                .withTtlSecondsAfterFinished(60)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels("app", JOB_NAME)
                .endMetadata()
                .withNewSpec()
                .withServiceAccountName("${System.getenv("APP_NAME")}-runner-sa")
                .addNewContainer()
                .withName("${System.getenv("APP_NAME")}")
                .withImage(System.getenv("IMAGE_REPO_PREFIX") + "${System.getenv("APP_NAME")}/k6-test:latest")
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
                .endSpec()
                .build()

        if (client) {
            client.batch().v1().jobs()
                    .inNamespace(NAMESPACE)
                    .resource(job)
                    .createOr(existing -> client.batch().v1().jobs()
                            .inNamespace(NAMESPACE)
                            .resource(job)
                            .update())
        }

        log.info("Kubernetes Job $JOB_NAME created with parallelism $parallelism")
    }

    private void monitorPodsAndStopIfLow(int parallelism) {
        log.info("Starting pod monitor for $JOB_NAME with parallelism $parallelism")
        int threshold = (int) (parallelism * 0.25)
        boolean isRunning = false
        def noRunningSince = System.currentTimeMillis()
        while (true) {
            try {
                def pods = client.pods()
                        .inNamespace(NAMESPACE)
                        .withLabel("app", JOB_NAME)
                        .list().items
                Number running = pods.count { it.status?.phase == 'Running' }
                log.info("Currently $running/$parallelism pods running for $JOB_NAME")
                if (isRunning && running <= threshold) {
                    log.info("Running pods ($running) ≤ threshold ($threshold) → stopping")
                    try {
                        log.info("Deleting jobs for $JOB_NAME")
                        deleteK6Jobs()
                    } catch (Exception e) {
                        log.error("Failed deleting jobs, no more available?", e)
                    }
                    break
                } else if (!isRunning && running > threshold) {
                    log.info("Running pods ($running) > threshold ($threshold) → starting")
                    isRunning = true
                } else if (!isRunning && (System.currentTimeMillis() - noRunningSince > 2 * 60 * 1000)) {
                    log.warn("No running pods for 2 minutes, stopping monitor and canceling request")
                    break
                }

                TimeUnit.SECONDS.sleep(5)
            } catch (InterruptedException ie) {
                log.warn("Monitor loop interrupted", ie)
                Thread.currentThread().interrupt()
                break
            } catch (Exception ex) {
                log.error("Monitor loop error", ex)
                break
            }
        }

        log.info("Pod monitor for $JOB_NAME finished")
    }

    private void deleteK6Jobs() {
        log.info("Deleting Kubernetes Job: $JOB_NAME")
        if (client) {
            client.batch().v1().jobs().inNamespace(NAMESPACE)
                    .withLabel("app", JOB_NAME)
                    .list()
                    .items
                    .each { job -> client.batch().v1().jobs().inNamespace(NAMESPACE).withName(job.metadata.name).delete()
                    }
        }

        log.info("Kubernetes Job $JOB_NAME deleted")
    }
}
