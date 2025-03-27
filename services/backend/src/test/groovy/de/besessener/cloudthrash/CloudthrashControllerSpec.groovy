package de.besessener.cloudthrash

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.mock.DetachedMockFactory

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest(classes = CloudthrashApplication)
@AutoConfigureMockMvc
@Import(CloudthrashControllerTest.MockConfig)
class CloudthrashControllerTest extends Specification {

    @Autowired
    MockMvc mvc

    @Autowired
    MinioService minioService

    @TestConfiguration
    static class MockConfig {
        def mockFactory = new DetachedMockFactory()

        @Bean
        MinioService minioService() { mockFactory.Mock(MinioService) }
    }

    def "uploads file successfully"() {
        given:
            def file = new MockMultipartFile("file", "test.txt", "text/plain", "mock".bytes)

        when:
            def result = mvc.perform(multipart("/api/upload").file(file))

        then:
            1 * minioService.uploadFile("test.txt", _ as InputStream)
            result.andExpect(status().isOk())
                    .andExpect(jsonPath('$.message').value("Upload successful"))
    }

    def "handles upload failure"() {
        given:
            def file = new MockMultipartFile("file", "fail.txt", "text/plain", "mock".bytes)
            minioService.uploadFile(_, _) >> { throw new IOException("boom") }

        when:
            def result = mvc.perform(multipart("/api/upload").file(file))

        then:
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath('$.message').value("Error uploading file: boom"))
    }

    def "downloads file successfully"() {
        given:
            def contentBytes = "hello".bytes
            minioService.downloadFile("file.txt") >> new ByteArrayInputStream(contentBytes)

        when:
            def result = mvc.perform(get("/api/download").param("fileName", "file.txt"))

        then:
            result.andExpect(status().isOk())
                    .andExpect(header().string("Content-Disposition", "attachment; filename=file.txt"))
                    .andExpect(content().bytes(contentBytes))
    }

    def "returns 404 if download file not found"() {
        given:
            minioService.downloadFile("missing.txt") >> null

        when:
            def result = mvc.perform(get("/api/download").param("fileName", "missing.txt"))

        then:
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath('$.message').value("File not found: missing.txt"))
    }

    def "handles download failure"() {
        given:
            minioService.downloadFile(_) >> { throw new IOException("read fail") }

        when:
            def result = mvc.perform(get("/api/download").param("fileName", "any.txt"))

        then:
            result.andExpect(status().isInternalServerError())
                    .andExpect(jsonPath('$.message').value("Error downloading file: read fail"))
    }

    def "deletes file successfully"() {
        when:
            def result = mvc.perform(delete("/api/delete").param("fileName", "file.txt"))

        then:
            1 * minioService.deleteFile("file.txt")
            result.andExpect(status().isOk())
                    .andExpect(jsonPath('$.message').value("File deleted successfully"))
    }

    def "returns 404 when file to delete not found"() {
        given:
            minioService.deleteFile("missing.txt") >> { throw new FileNotFoundException("not found") }

        when:
            def result = mvc.perform(delete("/api/delete").param("fileName", "missing.txt"))

        then:
            result.andExpect(status().isNotFound())
                    .andExpect(jsonPath('$.message').value("File not found: not found"))
    }

    def "handles unexpected delete error"() {
        given:
            minioService.deleteFile(_) >> { throw new RuntimeException("boom") }

        when:
            def result = mvc.perform(delete("/api/delete").param("fileName", "fail.txt"))

        then:
            result.andExpect(status().isInternalServerError())
                    .andExpect(jsonPath('$.message').value("Error deleting file: boom"))
    }
}
