package de.besessener.cloudthrash.service


import org.springframework.http.HttpStatus
import spock.lang.Specification

class TestExecutionServiceSpec extends Specification {

    def fileService = Mock(FileService)
    def service = new TestExecutionService(fileService: fileService)

    def setup() {
        service.@status = TestExecutionService.Status.IDLE
    }

    def "startTest - success"() {
        given:
            def payload = [
                    cpu       : "500m",
                    memory    : "256Mi",
                    loadAgents: 2,
                    envVars   : [[name: "FOO", value: "BAR"]]
            ]
            fileService.listFiles() >> [[filename: "test.js"]]

        when:
            def response = service.startTest(payload)

        then:
            response.statusCode == HttpStatus.OK
            response.body.message == "K6 test started with 2 agents"
            response.body.status == "RUNNING"
    }

    def "startTest - already running"() {
        given:
            service.@status = TestExecutionService.Status.RUNNING

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
            def response = service.startTest([
                    cpu: "1", memory: "1Gi", loadAgents: 1, envVars: []
            ])

        then:
            response.statusCode == HttpStatus.BAD_REQUEST
            response.body.message == "'test.js' file is required"
    }

    def "stopTest - success"() {
        given:
            service.@status = TestExecutionService.Status.RUNNING

        when:
            def response = service.stopTest()

        then:
            response.statusCode == HttpStatus.OK
            response.body.message == "Stopping all Kubernetes jobs..."
            response.body.status == "STOPPING"
    }

    def "stopTest - when not running"() {
        given:
            service.@status = TestExecutionService.Status.IDLE

        when:
            def response = service.stopTest()

        then:
            response.statusCode == HttpStatus.BAD_REQUEST
            response.body.message == "Cannot stop while not running"
    }

    def "getStatus - normal and error"() {
        given:
            fileService.listFiles() >> [[filename: "test.js"]]
            service.@status = TestExecutionService.Status.IDLE

        when:
            def response1 = service.getStatus()

        then:
            response1.body.status == "IDLE"

        when:
            service.@status = TestExecutionService.Status.ERROR
            service.@errorMessage = "Boom"
            def response2 = service.getStatus()

        then:
            response2.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
            response2.body.message.contains("Boom")
    }


    def "buildResponse - updates state based on file presence"() {
        given:
            service.@status = TestExecutionService.Status.ERROR

        and: "mock returns different results per call"
            fileService.listFiles() >>> [
                    [[filename: "abc.txt"]],
                    [[filename: "test.js"]]
            ]

        when: "no test.js"
            def response = service.buildResponse(HttpStatus.OK, "msg")

        then:
            response.body.status == "INIT"

        when: "test.js now exists"
            def response2 = service.buildResponse(HttpStatus.OK, "msg")

        then:
            response2.body.status == "IDLE"
    }
}
