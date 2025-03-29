package de.besessener.cloudthrash.controller

import de.besessener.cloudthrash.service.TestExecutionService
import groovy.json.JsonOutput
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class CloudthrashTestControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    TestExecutionService testExecutionService = Stub()

    def "should start test execution"() {
        given:
            def requestPayload = [param1: "value1", param2: 123]
            def response = [message: "Test started"]

        and:
            testExecutionService.startTest(_) >> ResponseEntity.ok(response)

        expect:
            mockMvc.perform(post("/api/start")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JsonOutput.toJson(requestPayload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath('$.message').value("Test started"))
    }

    def "should stop test execution"() {
        given:
            def response = [message: "Test stopped"]

        and:
            testExecutionService.stopTest() >> ResponseEntity.ok(response)

        expect:
            mockMvc.perform(post("/api/stop"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath('$.message').value("Test stopped"))
    }

    def "should return test execution status"() {
        given:
            def response = [status: "RUNNING"]

        and:
            testExecutionService.getStatus() >> ResponseEntity.ok(response)

        expect:
            mockMvc.perform(get("/api/status"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath('$.status').value("RUNNING"))
    }
}
