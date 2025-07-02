package org.nobuddy.thrashbuddy.controller

import org.nobuddy.thrashbuddy.service.TestExecutionService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
class ThrashBuddyTestController {

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
