package org.nobuddy.thrashbuddy.service


import io.fabric8.kubernetes.client.KubernetesClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import spock.lang.Specification

@SpringBootTest
class TestExecutionServiceSpec extends Specification {

    TestExecutionService service

    @Autowired
    StatusService statusService

    def fileService = Mock(FileService)
    def k8sService = Mock(K8sService)
    def k8sClient = Mock(KubernetesClient)

    def setup() {
        statusService.setStatus(StatusService.ResponseStatus.IDLE)
        service = new TestExecutionService(fileService, k8sClient, k8sService, statusService)
    }

    def "startTest - success"() {
        given:
            def payload = [cpu       : "500m",
                           memory    : "256Mi",
                           loadAgents: 2,
                           envVars   : [[name: "FOO", value: "BAR"]]]
            fileService.listFiles() >> [[filename: "test.js"]]
            k8sClient.executeJobs(_, _, _, _) >> { void }

        when:
            def response = service.startTest(payload)

        then:
            response.statusCode == HttpStatus.OK
            response.body.message == "K6 test started with 2 agents"
            response.body.status == "RUNNING"
    }

    def "startTest - already running"() {
        given:
            statusService.setStatus(StatusService.ResponseStatus.RUNNING)

        when:
            def response = service.startTest([:])

        then:
            response.statusCode == HttpStatus.BAD_REQUEST
            response.body.message == "Cannot start while not idle"
    }

    def "'test.js' missing"() {
        given:
            fileService.listFiles() >> [[filename: "other.txt"]]

        when:
            def response = service.startTest([cpu: "1", memory: "1Gi", loadAgents: 1, envVars: []])

        then:
            response.statusCode == HttpStatus.BAD_REQUEST
            response.body.message == "'test.js' file is required"
    }

    def "stopTest - success"() {
        given:
            statusService.setStatus(StatusService.ResponseStatus.RUNNING)

        when:
            def response = service.stopTest()

        then:
            response.statusCode == HttpStatus.OK
            response.body.message == "Stopping all Kubernetes jobs..."
            response.body.status == "STOPPING"
    }

    def "stopTest - when not running"() {
        given:
            fileService.listFiles() >> []
            statusService.setStatus(StatusService.ResponseStatus.IDLE)

        when:
            def response = service.stopTest()

        then:
            response.statusCode == HttpStatus.BAD_REQUEST
            response.body.message == "Cannot stop while not running"
    }

    def "getStatus - normal and error"() {
        given:
            fileService.listFiles() >> [[filename: "test.js"], [filename: "test.js"]]
            k8sService.getStatus() >>> [StatusService.ResponseStatus.IDLE, StatusService.ResponseStatus.ERROR]
            k8sService.getErrorMessage() >>> ["", "Boom"]

        when:
            def response1 = service.getStatus()

        then:
            response1.body.status == "IDLE"

        when:
            statusService.setStatus(StatusService.ResponseStatus.ERROR)
            def response2 = service.getStatus()

        then:
            response2.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
            response2.body.message.contains("Boom")
    }

    def "executeJobs - triggers setError on exception"() {
        given:
            def fileService = Mock(FileService)
            def client = Mock(KubernetesClient)
            def serviceSpy = Spy(TestExecutionService, constructorArgs: [fileService, client, k8sService, statusService])

            statusService.setStatus(StatusService.ResponseStatus.IDLE)
            fileService.listFiles() >> [[filename: "test.js"]]

            def payload = [cpu       : "1",
                           memory    : "1Gi",
                           loadAgents: 1,
                           envVars   : []]

            serviceSpy.createK6Job(_, _, _, _) >> { throw new RuntimeException("Job error") }

        when:
            def response = serviceSpy.startTest(payload)
            sleep(2000)

        then:
            response.statusCode == HttpStatus.OK
            eventually {
                statusService.getStatus() == StatusService.ResponseStatus.IDLE
            }
    }

    def "getStatus - transitions between INIT and IDLE"() {
        given:
            fileService.listFiles() >>> [[], [[filename: "test.js"]]]
            k8sService.getStatus() >>> [StatusService.ResponseStatus.INIT, StatusService.ResponseStatus.IDLE]
            k8sService.getErrorMessage() >>> ["", ""]

        when:
            def response1 = service.getStatus()

        then:
            response1.statusCode == HttpStatus.OK
            response1.body.status == "INIT"

        when:
            def response2 = service.getStatus()

        then:
            response2.statusCode == HttpStatus.OK
            response2.body.status == "IDLE"
    }

    private static void eventually(Closure assertion, int timeoutMs = 3000, int intervalMs = 100) {
        long start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                assertion.call()
                return
            } catch (AssertionError ignored) {
                sleep(intervalMs)
            }
        }
        assertion.call()
    }
}
