package org.nobuddy.thrashbuddy.service

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.EnvVarBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class TestExecutionService {

    // names already injected by K8sService for MinIO/job wiring - callers may not override these
    private static final Set<String> RESERVED_ENV_NAMES = [
            'MINIO_URL', 'MINIO_ACCESS_KEY', 'MINIO_SECRET_KEY', 'MINIO_BUCKET', 'K6_INSTANCE_ID'
    ] as Set<String>
    private static final int MAX_ENV_VARS = 50
    private static final def ENV_NAME_PATTERN = ~/^[A-Za-z_][A-Za-z0-9_]*$/
    private static final def K8S_QUANTITY_PATTERN = ~/^[0-9]+(\.[0-9]+)?(m|K|M|G|T|P|E|Ki|Mi|Gi|Ti|Pi|Ei)?$/

    @Autowired
    FileService fileService

    @Autowired
    KubernetesClient client

    @Autowired
    K8sService k8sService

    @Autowired
    StatusService statusService

    @Value('${MIN_LOAD_AGENTS:1}')
    int minLoadAgents = 1

    @Value('${MAX_LOAD_AGENTS:50}')
    int maxLoadAgents = 50

    TestExecutionService(FileService fileService, KubernetesClient client, K8sService k8sService, StatusService statusService) {
        this.fileService = fileService
        this.client = client
        this.k8sService = k8sService
        this.statusService = statusService
    }

    ResponseEntity<Map> startTest(Map<String, Object> payload) {
        if (statusService.getStatus() != StatusService.ResponseStatus.IDLE) {
            return buildResponse(HttpStatus.BAD_REQUEST, StatusService.ResponseStatus.ERROR, "Cannot start while not idle")
        }

        def uploadedFiles = fileService.listFiles()
        if (!uploadedFiles.any { it.filename == 'test.js' }) {
            return buildResponse(HttpStatus.BAD_REQUEST, StatusService.ResponseStatus.ERROR, "'test.js' file is required")
        }

        def cpu = payload.cpu as String
        def memory = payload.memory as String
        def loadAgents = payload.loadAgents as Integer

        def validationError = validateStartParams(cpu, memory, loadAgents)
        if (validationError) {
            return buildResponse(HttpStatus.BAD_REQUEST, StatusService.ResponseStatus.ERROR, validationError)
        }

        List<EnvVar> envVars
        try {
            envVars = toEnvVars(payload.envVars as List<Map<String, String>>)
        } catch (IllegalArgumentException e) {
            return buildResponse(HttpStatus.BAD_REQUEST, StatusService.ResponseStatus.ERROR, e.message)
        }

        if (!statusService.compareAndSet(StatusService.ResponseStatus.IDLE, StatusService.ResponseStatus.RUNNING)) {
            return buildResponse(HttpStatus.BAD_REQUEST, StatusService.ResponseStatus.ERROR, "Cannot start while not idle")
        }
        k8sService.start(cpu, memory, loadAgents, envVars)

        return buildResponse(HttpStatus.OK, StatusService.ResponseStatus.RUNNING, "K6 test started with $loadAgents agents")
    }

    private String validateStartParams(String cpu, String memory, Integer loadAgents) {
        if (loadAgents == null || loadAgents < minLoadAgents || loadAgents > maxLoadAgents) {
            return "'loadAgents' must be between $minLoadAgents and $maxLoadAgents" as String
        }
        if (!cpu || !(cpu ==~ K8S_QUANTITY_PATTERN)) {
            return "'cpu' must be a valid Kubernetes CPU quantity (e.g. '500m' or '1')" as String
        }
        if (!memory || !(memory ==~ K8S_QUANTITY_PATTERN)) {
            return "'memory' must be a valid Kubernetes memory quantity (e.g. '512Mi')" as String
        }
        return null
    }

    ResponseEntity<Map> stopTest() {
        if (statusService.getStatus() != StatusService.ResponseStatus.RUNNING) {
            return buildResponse(HttpStatus.BAD_REQUEST, StatusService.ResponseStatus.ERROR, "Cannot stop while not running")
        }

        if (!statusService.compareAndSet(StatusService.ResponseStatus.RUNNING, StatusService.ResponseStatus.STOPPING)) {
            return buildResponse(HttpStatus.BAD_REQUEST, StatusService.ResponseStatus.ERROR, "Cannot stop while not running")
        }
        k8sService.stop()

        return buildResponse(HttpStatus.OK, StatusService.ResponseStatus.STOPPING, "Stopping all Kubernetes jobs...")
    }

    ResponseEntity<Map> getStatus() {
        statusService.setStatus(k8sService.getStatus())
        statusService.setErrorMessage(k8sService.getErrorMessage())

        def files = fileService.listFiles()
        if (statusService.getStatus() == StatusService.ResponseStatus.IDLE && !files.any { it.filename == 'test.js' }) {
            statusService.setStatus(StatusService.ResponseStatus.INIT)
            return buildResponse(HttpStatus.OK, StatusService.ResponseStatus.INIT, statusService.getStatus().name())
        }

        if (statusService.getStatus() == StatusService.ResponseStatus.INIT && files.any { it.filename == 'test.js' }) {
            statusService.setStatus(StatusService.ResponseStatus.IDLE)
            return buildResponse(HttpStatus.OK, StatusService.ResponseStatus.IDLE, statusService.getStatus().name())
        }

        if (statusService.getStatus() == StatusService.ResponseStatus.ERROR) {
            statusService.setStatus(StatusService.ResponseStatus.IDLE)
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, StatusService.ResponseStatus.ERROR, "Internal error: ${statusService.getErrorMessage()}", )
        }

        return buildResponse(HttpStatus.OK, statusService.getStatus(), statusService.getStatus().name())
    }

    private static List<EnvVar> toEnvVars(List<Map<String, String>> rawVars) {
        if (!rawVars) return []
        if (rawVars.size() > MAX_ENV_VARS) {
            throw new IllegalArgumentException("At most $MAX_ENV_VARS environment variables are allowed" as String)
        }
        rawVars.collect {
            def name = it.name as String
            if (!name || !(name ==~ ENV_NAME_PATTERN)) {
                throw new IllegalArgumentException("Invalid environment variable name: $name" as String)
            }
            if (RESERVED_ENV_NAMES.contains(name.toUpperCase())) {
                throw new IllegalArgumentException("Environment variable name '$name' is reserved" as String)
            }
            new EnvVarBuilder().withName(name).withValue(it.value as String).build()
        }
    }

    private ResponseEntity<Map> buildResponse(HttpStatus statusCode,StatusService.ResponseStatus status, String msg) {
        return ResponseEntity.status(statusCode).body([message   : msg,
                                                       httpStatus: statusCode.reasonPhrase,
                                                       status    : status.name(),
                                                       data      : fileService.listFiles()])
    }

}
