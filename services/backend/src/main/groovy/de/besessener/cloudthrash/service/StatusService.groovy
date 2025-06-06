package de.besessener.thrashbuddy.service

import org.springframework.stereotype.Service

@Service
class StatusService {
    enum ResponseStatus {
        IDLE, RUNNING, STOPPING, ERROR, INIT
    }

    ResponseStatus status = ResponseStatus.INIT
    String errorMessage = ""
}
