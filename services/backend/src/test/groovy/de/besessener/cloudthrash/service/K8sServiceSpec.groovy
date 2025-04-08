package de.besessener.cloudthrash.service


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification

@SpringBootTest
class K8sServiceSpec extends Specification {

    K8sService k8sService

    @Autowired
    StatusService statusService

    def "executeJobs - failing due to null client"() {
        given:
            statusService.setStatus(StatusService.ResponseStatus.IDLE)
            k8sService = new K8sService(null, statusService)

        when:
            k8sService.executeJobs("500m", "500Mi", 2, [])

        then:
            statusService.getStatus() == StatusService.ResponseStatus.ERROR
    }

    def "deleteAllK6Jobs - always IDLE, even after failure"() {
        given:
            statusService.setStatus(StatusService.ResponseStatus.IDLE)
            k8sService = new K8sService(null, statusService)

        when:
            k8sService.deleteAllK6Jobs()

        then:
            statusService.getStatus() == StatusService.ResponseStatus.IDLE
    }
}
