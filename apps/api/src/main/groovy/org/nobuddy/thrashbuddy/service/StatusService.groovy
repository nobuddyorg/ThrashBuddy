package org.nobuddy.thrashbuddy.service

import org.springframework.stereotype.Service

@Service
class StatusService {
    enum ResponseStatus {
        IDLE, RUNNING, STOPPING, ERROR, INIT
    }

    private volatile ResponseStatus status = ResponseStatus.INIT
    private volatile String errorMessage = ""

    synchronized ResponseStatus getStatus() {
        status
    }

    synchronized void setStatus(ResponseStatus newStatus) {
        status = newStatus
    }

    synchronized String getErrorMessage() {
        errorMessage
    }

    synchronized void setErrorMessage(String message) {
        errorMessage = message
    }

    // Atomically applies newStatus only if the current status is still expected,
    // closing the check-then-act race between reading status and transitioning it.
    synchronized boolean compareAndSet(ResponseStatus expected, ResponseStatus newStatus) {
        if (status != expected) {
            return false
        }
        status = newStatus
        return true
    }
}
