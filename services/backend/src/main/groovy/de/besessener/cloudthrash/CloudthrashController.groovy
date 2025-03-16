package de.besessener.cloudthrash

import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api")
class CloudthrashController {

    private enum Status {
        IDLE, RUNNING, STOPPING
    }

    private Status status = Status.IDLE

    @PostMapping("/start")
    ResponseEntity<Map<String, String>> start() {
        if (status != Status.IDLE) {
            return badRequest("Cannot start while not idle")
        }

        status = Status.RUNNING
        runAsync(120000) { status = Status.IDLE }

        return okResponse("Starting...")
    }

    @PostMapping("/stop")
    ResponseEntity<Map<String, String>> stop() {
        if (status != Status.RUNNING) {
            return badRequest("Cannot stop while not running")
        }

        status = Status.STOPPING
        runAsync(60000) { status = Status.IDLE }

        return okResponse("Stopping...")
    }

    @GetMapping("/status")
    ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok([status: status.name()])
    }

    private void runAsync(long delay, Closure action) {
        Thread.start {
            try {
                Thread.sleep(delay)
                action.call()
            } catch (InterruptedException ignored) {}
        }
    }

    private ResponseEntity<Map<String, String>> okResponse(String message) {
        ResponseEntity.ok([message: message, status: status.name()])
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        ResponseEntity.badRequest().body([message: message, status: status.name()])
    }
}
