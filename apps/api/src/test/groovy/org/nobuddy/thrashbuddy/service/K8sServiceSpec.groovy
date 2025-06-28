package org.nobuddy.thrashbuddy.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.task.SyncTaskExecutor
import spock.lang.Specification

@SpringBootTest
class K8sServiceSpec extends Specification {

    K8sService k8sService

    @Autowired
    StatusService statusService

    def setup() {
        statusService.setStatus(StatusService.ResponseStatus.IDLE)
    }

    def "start - sets error due to null client"() {
        given:
            def taskExecutor = new SyncTaskExecutor()
            k8sService = new K8sService(null, statusService, taskExecutor)

        when:
            k8sService.start("500m", "500Mi", 2, [])

        then:
            statusService.getStatus() == StatusService.ResponseStatus.IDLE
    }

    def "stop - still results in IDLE even with null client"() {
        given:
            def taskExecutor = new SyncTaskExecutor()
            k8sService = new K8sService(null, statusService, taskExecutor)

        when:
            k8sService.stop()

        then:
            statusService.getStatus() == StatusService.ResponseStatus.IDLE
    }
}
