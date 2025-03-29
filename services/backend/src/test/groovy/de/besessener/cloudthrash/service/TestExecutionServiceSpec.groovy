package de.besessener.cloudthrash.service


import io.fabric8.kubernetes.api.model.ObjectMeta
import io.fabric8.kubernetes.api.model.batch.v1.Job
import io.fabric8.kubernetes.api.model.batch.v1.JobStatus
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.dsl.*
import org.springframework.http.HttpStatus
import spock.lang.Specification

class TestExecutionServiceSpec extends Specification {

    def fileService = Mock(FileService)
    def k8sClient = Mock(KubernetesClient)
    def service = new TestExecutionService(fileService, k8sClient)

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

        and:
            fileService.listFiles() >>> [
                    [[filename: "abc.txt"]],
                    [[filename: "test.js"]]
            ]

        when:
            def response = service.buildResponse(HttpStatus.OK, "msg")

        then:
            response.body.status == "INIT"

        when:
            def response2 = service.buildResponse(HttpStatus.OK, "msg")

        then:
            response2.body.status == "IDLE"
    }

    def "executeJobs - triggers setError on exception"() {
        given:
            def fileService = Mock(FileService)
            def client = Mock(KubernetesClient)
            def serviceSpy = Spy(TestExecutionService, constructorArgs: [fileService, client])

            serviceSpy.@status = TestExecutionService.Status.IDLE
            fileService.listFiles() >> [[filename: "test.js"]]

            def payload = [
                    cpu       : "1",
                    memory    : "1Gi",
                    loadAgents: 1,
                    envVars   : []
            ]

            serviceSpy.createK6Job(_, _, _, _) >> { throw new RuntimeException("Job error") }

        when:
            def response = serviceSpy.startTest(payload)
            sleep(2000)

        then:
            response.statusCode == HttpStatus.OK
            eventually {
                serviceSpy.@status == TestExecutionService.Status.IDLE
            }
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

    def "waitForCompletion - returns IDLE on job success"() {
        given:
            def fileService = Mock(FileService)
            def client = Mock(KubernetesClient)
            def batchAPI = Mock(BatchAPIGroupDSL)
            def v1BatchAPI = Mock(V1BatchAPIGroupDSL)
            def jobsOp = Mock(MixedOperation)
            def namespacedOp = Mock(NonNamespaceOperation)
            def jobResource = Mock(Resource)

            client.batch() >> batchAPI
            batchAPI.v1() >> v1BatchAPI
            v1BatchAPI.jobs() >> jobsOp
            jobsOp.inNamespace(_) >> namespacedOp
            namespacedOp.withName("test-job") >> jobResource

            def job = new Job()
            job.metadata = new ObjectMeta(name: "test-job")

            def successJob = new Job()
            successJob.metadata = new ObjectMeta(name: "test-job")
            successJob.status = new JobStatus(succeeded: 1)
            jobResource.get() >> successJob

            def service = new TestExecutionService(fileService, client) {
                void sleep(long millis) {}
            }

            def method = TestExecutionService.getDeclaredMethod("waitForCompletion", Job)
            method.accessible = true

        when:
            def result = method.invoke(service, job)

        then:
            result == TestExecutionService.Status.IDLE
    }

    def "waitForCompletion - returns ERROR on job failure"() {
        given:
            def fileService = Mock(FileService)
            def client = Mock(KubernetesClient)
            def batchAPI = Mock(BatchAPIGroupDSL)
            def v1BatchAPI = Mock(V1BatchAPIGroupDSL)
            def jobsOp = Mock(MixedOperation)
            def namespacedOp = Mock(NonNamespaceOperation)
            def jobResource = Mock(Resource)

            client.batch() >> batchAPI
            batchAPI.v1() >> v1BatchAPI
            v1BatchAPI.jobs() >> jobsOp
            jobsOp.inNamespace(_) >> namespacedOp
            namespacedOp.withName("test-job") >> jobResource

            def job = new Job()
            job.metadata = new ObjectMeta(name: "test-job")

            def failedJob = new Job()
            failedJob.metadata = new ObjectMeta(name: "test-job")
            failedJob.status = new JobStatus(failed: 1)
            jobResource.get() >> failedJob

            def service = new TestExecutionService(fileService, client) {
                void sleep(long millis) {}
            }

            def method = TestExecutionService.getDeclaredMethod("waitForCompletion", Job)
            method.accessible = true

        when:
            def result = method.invoke(service, job)

        then:
            result == TestExecutionService.Status.ERROR
    }

    def "waitForCompletion - returns ERROR when job not found"() {
        given:
            def fileService = Mock(FileService)
            def client = Mock(KubernetesClient)
            def batchAPI = Mock(BatchAPIGroupDSL)
            def v1BatchAPI = Mock(V1BatchAPIGroupDSL)
            def jobsOp = Mock(MixedOperation)
            def namespacedOp = Mock(NonNamespaceOperation)
            def jobResource = Mock(Resource)

            client.batch() >> batchAPI
            batchAPI.v1() >> v1BatchAPI
            v1BatchAPI.jobs() >> jobsOp
            jobsOp.inNamespace(_) >> namespacedOp
            namespacedOp.withName("test-job") >> jobResource

            def job = new Job()
            job.metadata = new ObjectMeta(name: "test-job")

            jobResource.get() >> null

            def service = new TestExecutionService(fileService, client) {
                void sleep(long millis) {}
            }

            def method = TestExecutionService.getDeclaredMethod("waitForCompletion", Job)
            method.accessible = true

        when:
            def result = method.invoke(service, job)

        then:
            result == TestExecutionService.Status.ERROR
    }
}
