package de.besessener.cloudthrash.controller

import de.besessener.cloudthrash.service.TestExecutionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
class CloudthrashTestController {

    @Autowired
    TestExecutionService testExecutionService

    @PostMapping("/start")
    ResponseEntity<Map> start(@RequestBody Map<String, Object> payload) {
        return testExecutionService.startTest(payload)
    }

    @PostMapping("/stop")
    ResponseEntity<Map> stop() {
        return testExecutionService.stopTest()
    }

    @GetMapping("/status")
    ResponseEntity<Map> getStatus() {
        return testExecutionService.getStatus()
    }
}
