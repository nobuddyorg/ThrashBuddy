package de.besessener.thrashbuddy.service

import spock.lang.Specification

class StatusServiceSpec extends Specification {

    StatusService statusService

    def setup() {
        statusService = new StatusService()
    }

    def "test initial status"() {
        expect:
            statusService.status == StatusService.ResponseStatus.INIT
            statusService.errorMessage == ""
    }

    def "test status update"() {
        when:
            statusService.status = StatusService.ResponseStatus.RUNNING

        then:
            statusService.status == StatusService.ResponseStatus.RUNNING

        when:
            statusService.status = StatusService.ResponseStatus.STOPPING

        then:
            statusService.status == StatusService.ResponseStatus.STOPPING
    }

    def "test error message update"() {
        when:
            statusService.errorMessage = "An error occurred"

        then:
            statusService.errorMessage == "An error occurred"

        when:
            statusService.errorMessage = "New error message"

        then:
            statusService.errorMessage == "New error message"
    }

    def "test status ERROR and error message"() {
        when:
            statusService.status = StatusService.ResponseStatus.ERROR
            statusService.errorMessage = "Critical failure"

        then:
            statusService.status == StatusService.ResponseStatus.ERROR
            statusService.errorMessage == "Critical failure"
    }
}
