package de.besessener.cloudthrash.controller

import de.besessener.cloudthrash.service.FileService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class CloudthrashFileControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    FileService fileService = Stub()

    def "should upload file successfully"() {
        given:
            def mockFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".bytes)
            def response = [message: "Upload successful"]

        and:
            fileService.handleUpload(_) >> ResponseEntity.ok(response)

        expect:
            mockMvc.perform(multipart("/api/upload").file(mockFile))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath('$.message').value("Upload successful"))
    }

    def "should download file successfully"() {
        given:
            def fileName = "example.txt"
            fileService.handleDownload(fileName) >> ResponseEntity.ok("mock content")

        expect:
            mockMvc.perform(get("/api/download").param("fileName", fileName))
                    .andExpect(status().isOk())
                    .andExpect(content().string("mock content"))
    }

    def "should delete file successfully"() {
        given:
            def fileName = "to-delete.txt"
            def response = [message: "Deleted"]
            fileService.handleDelete(fileName) >> ResponseEntity.ok(response)

        expect:
            mockMvc.perform(delete("/api/delete").param("fileName", fileName))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath('$.message').value("Deleted"))
    }
}
