package de.besessener.cloudthrash;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/test")
public class CloudthrashController {

    private enum Status {
        IDLE, RUNNING, STOPPING
    }

    private Status status = Status.IDLE;

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> start() {
        if (status != Status.IDLE) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot start while not idle", "status", status.name()));
        }
        status = Status.RUNNING;
        new Thread(() -> {
            try {
                Thread.sleep(120000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            status = Status.IDLE;
        }).start();
        return ResponseEntity.ok(Map.of("message", "Starting...", "status", status.name()));
    }

    @PostMapping("/stop")
    public ResponseEntity<Map<String, String>> stop() {
        if (status != Status.RUNNING) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot stop while not running", "status", status.name()));
        }
        status = Status.STOPPING;
        new Thread(() -> {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            status = Status.IDLE;
        }).start();
        return ResponseEntity.ok(Map.of("message", "Stopping...", "status", status.name()));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", status.name()));
    }
}
