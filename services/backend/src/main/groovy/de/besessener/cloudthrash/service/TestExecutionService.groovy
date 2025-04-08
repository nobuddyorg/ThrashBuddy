package de.besessener.cloudthrash.service

import io.fabric8.kubernetes.api.model.EnvVar
import io.fabric8.kubernetes.api.model.EnvVarBuilder
import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class TestExecutionService {
    @Autowired
    FileService fileService

    @Autowired
    KubernetesClient client

    @Autowired
    K8sService k8sService

    @Autowired
    StatusService statusService

    TestExecutionService(FileService fileService, KubernetesClient client, K8sService k8sService, StatusService statusService) {
        this.fileService = fileService
        this.client = client
        this.k8sService = k8sService
        this.statusService = statusService
    }

    ResponseEntity<Map> startTest(Map<String, Object> payload) {
        if (statusService.getStatus() != StatusService.ResponseStatus.IDLE) {
            return buildResponse(HttpStatus.BAD_REQUEST, "Cannot start while not idle")
        }

        def uploadedFiles = fileService.listFiles()
        if (!uploadedFiles.any { it.filename == 'test.js' }) {
            return buildResponse(HttpStatus.BAD_REQUEST, "'test.js' file is required")
        }

        def cpu = payload.cpu as String
        def memory = payload.memory as String
        def loadAgents = payload.loadAgents as Integer
        def envVars = toEnvVars(payload.envVars)

        statusService.setStatus(StatusService.ResponseStatus.RUNNING)
        k8sService.executeJobs(cpu, memory, loadAgents, envVars)

        return buildResponse(HttpStatus.OK, "K6 test started with $loadAgents agents")
    }

    ResponseEntity<Map> stopTest() {
        if (statusService.getStatus() != StatusService.ResponseStatus.RUNNING) {
            return buildResponse(HttpStatus.BAD_REQUEST, "Cannot stop while not running")
        }

        statusService.setStatus(StatusService.ResponseStatus.STOPPING)
        k8sService.deleteAllK6Jobs()

        return buildResponse(HttpStatus.OK, "Stopping all Kubernetes jobs...")
    }

    ResponseEntity<Map> getStatus() {
        return (statusService.getStatus() == StatusService.ResponseStatus.ERROR) ?
                buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error: ${statusService.getErrorMessage()}") :
                buildResponse(HttpStatus.OK, statusService.getStatus().name())
    }

    private List<EnvVar> toEnvVars(List<Map<String, String>> rawVars) {
        rawVars?.collect {
            new EnvVarBuilder().withName(it.name).withValue(it.value).build()
        } ?: []
    }

    private ResponseEntity<Map> buildResponse(HttpStatus statusCode, String msg) {
        def files = fileService.listFiles()
        if (statusService.getStatus() == StatusService.ResponseStatus.ERROR) {
            statusService.setStatus(StatusService.ResponseStatus.IDLE)
        }

        if (statusService.getStatus() == StatusService.ResponseStatus.IDLE && !files.any { it.filename == 'test.js' }) {
            statusService.setStatus(StatusService.ResponseStatus.INIT)
        } else if (statusService.getStatus() == StatusService.ResponseStatus.INIT && files.any { it.filename == 'test.js' }) {
            statusService.setStatus(StatusService.ResponseStatus.IDLE)
        }

        return ResponseEntity.status(statusCode).body([
                message   : msg,
                httpStatus: statusCode.reasonPhrase,
                status    : statusService.getStatus().name(),
                data      : files
        ])
    }
}
