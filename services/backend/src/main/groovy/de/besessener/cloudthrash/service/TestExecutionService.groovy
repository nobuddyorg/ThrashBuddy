package de.besessener.cloudthrash.service

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.EnvVarBuilder
import io.fabric8.kubernetes.api.model.Quantity
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

import java.util.concurrent.Executors

@Service
class TestExecutionService {

    enum Status {
        IDLE, RUNNING, STOPPING, ERROR, INIT
    }

    protected final executorService = Executors.newCachedThreadPool()

    private static final log = LoggerFactory.getLogger(TestExecutionService)
    private static final String NAMESPACE = "default"

    private volatile Status status = Status.INIT
    private String errorMessage = ""

    @Autowired
    FileService fileService

    @Autowired
    KubernetesClient client

    TestExecutionService(FileService fileService, KubernetesClient client) {
        this.fileService = fileService
        this.client = client
    }


    ResponseEntity<Map> startTest(Map<String, Object> payload) {
        if (status != Status.IDLE)
            return buildResponse(HttpStatus.BAD_REQUEST, "Cannot start while not idle")

        def fileList = fileService.listFiles()
        if (!fileList.any { it.filename == 'test.js' })
            return buildResponse(HttpStatus.BAD_REQUEST, "'test.js' file is required")

        def cpu = payload.cpu as String
        def memory = payload.memory as String
        def loadAgents = payload.loadAgents as Integer
        def envVars = toEnvVars(payload.envVars)

        status = Status.RUNNING
        executorService.submit {
            executeJobs(cpu, memory, loadAgents, envVars)
        }

        return buildResponse(HttpStatus.OK, "K6 test started with $loadAgents agents")
    }

    ResponseEntity<Map> stopTest() {
        if (status != Status.RUNNING)
            return buildResponse(HttpStatus.BAD_REQUEST, "Cannot stop while not running")

        status = Status.STOPPING
        executorService.submit {
            deleteAllK6Jobs()
            status = Status.IDLE
        }

        return buildResponse(HttpStatus.OK, "Stopping all Kubernetes jobs...")
    }

    ResponseEntity<Map> getStatus() {
        return (status == Status.ERROR) ?
                buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error: $errorMessage") :
                buildResponse(HttpStatus.OK, status.name())
    }

    private void executeJobs(String cpu, String memory, int count, List<EnvVar> envVars) {
        try {
            def futures = (1..count).collect { index ->
                executorService.submit {
                    def job = createK6Job(cpu, memory, envVars, index)
                    return waitForCompletion(job)
                }
            }

            executorService.submit {
                def results = futures*.get()
                deleteAllK6Jobs()

                if (results.any { it == Status.ERROR }) {
                    setError("One or more K6 jobs failed.")
                } else {
                    status = Status.IDLE
                    log.info("All jobs completed successfully.")
                }
            }

        } catch (Exception e) {
            setError("Failed to start jobs", e)
        }
    }

    private Job createK6Job(String cpu, String memory, List<EnvVar> envVars, int index) {
        def jobName = "cloudthrash-job-${index}"
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
                .withNewMetadata().withName(jobName).withNamespace(NAMESPACE).addToLabels("app", "cloudthrash-job").endMetadata()
                .withNewSpec()
                .withNewTemplate()
                .withNewMetadata().addToLabels("app", "cloudthrash-job").endMetadata()
                .withNewSpec()
                .withServiceAccountName("cloudthrash-runner-sa")
                .addNewContainer()
                .withName("cloudthrash")
                .withImage(System.getenv("IMAGE_REPO_PREFIX") + "cloud-thrash/k6-test:latest")
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

        return client.batch().v1().jobs().inNamespace(NAMESPACE).create(job)
    }

    private Status waitForCompletion(Job job) {
        def jobName = job?.metadata?.name
        while (true) {
            def j = client.batch().v1().jobs().inNamespace(NAMESPACE).withName(jobName).get()
            if (!j) return Status.ERROR

            if (j.status?.succeeded > 0) return Status.IDLE
            if (j.status?.failed > 0) return Status.ERROR
            sleep(5000)
        }
    }

    private void deleteAllK6Jobs() {
        client.batch().v1().jobs().inNamespace(NAMESPACE)
                .withLabel("app", "cloudthrash-job")
                .list()
                .items
                .each { job ->
                    client.batch().v1().jobs().inNamespace(NAMESPACE).withName(job.metadata.name).delete()
                }
    }

    private List<EnvVar> toEnvVars(List<Map<String, String>> rawVars) {
        rawVars?.collect {
            new EnvVarBuilder().withName(it.name).withValue(it.value).build()
        } ?: []
    }

    private void setError(String msg, Exception e = null) {
        status = Status.ERROR
        errorMessage = e ? "$msg: ${e.message}" : msg
        log.error(errorMessage, e)
    }

    private ResponseEntity<Map> buildResponse(HttpStatus statusCode, String msg) {
        def files = fileService.listFiles()
        if (status == Status.ERROR) status = Status.IDLE

        if (status == Status.IDLE && !files.any { it.filename == 'test.js' }) status = Status.INIT
        else if (status == Status.INIT && files.any { it.filename == 'test.js' }) status = Status.IDLE

        return ResponseEntity.status(statusCode).body([
                message   : msg,
                httpStatus: statusCode.reasonPhrase,
                status    : status.name(),
                data      : files
        ])
    }
}
