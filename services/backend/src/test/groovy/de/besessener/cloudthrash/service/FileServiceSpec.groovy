package de.besessener.cloudthrash.service

import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import spock.lang.Specification

class FileServiceSpec extends Specification {

    def minioService = Mock(MinioService)
    def fileService = new FileService(minioService: minioService)

    def "handleUpload - success"() {
        given:
            def file = new MockMultipartFile("file", "test.txt", "text/plain", "Hello".bytes)

        when:
            def response = fileService.handleUpload(file)

        then:
            1 * minioService.uploadFile("test.txt", _)
            response.statusCode == HttpStatus.OK
            response.body.message == "File uploaded"
    }

    def "handleUpload - failure"() {
        given:
            def file = new MockMultipartFile("file", "fail.txt", "text/plain", "Oops".bytes)
            minioService.uploadFile(_, _) >> { throw new RuntimeException("Something went wrong") }

        when:
            def response = fileService.handleUpload(file)

        then:
            response.statusCode == HttpStatus.BAD_REQUEST
            response.body.message.contains("Something went wrong")
    }

    def "handleDownload - success"() {
        given:
            def fileName = "hello.txt"
            def content = "Hello, World!".bytes
            minioService.downloadFile(fileName) >> new ByteArrayInputStream(content)

        when:
            def response = fileService.handleDownload(fileName)

        then:
            response.statusCode == HttpStatus.OK
            response.body instanceof ByteArrayResource
            ((ByteArrayResource) response.body).byteArray == content
            response.headers.get("Content-Disposition")[0] == "attachment; filename=hello.txt"
    }

    def "handleDownload - file not found"() {
        given:
            def fileName = "missing.txt"
            minioService.downloadFile(fileName) >> null

        when:
            def response = fileService.handleDownload(fileName)

        then:
            response.statusCode == HttpStatus.NOT_FOUND
            response.body.message == "File not found"
    }

    def "handleDownload - exception"() {
        given:
            minioService.downloadFile(_) >> { throw new RuntimeException("Oops") }

        when:
            def response = fileService.handleDownload("error.txt")

        then:
            response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
            response.body.message.contains("Oops")
    }

    def "handleDelete - success"() {
        when:
            def response = fileService.handleDelete("test.txt")

        then:
            1 * minioService.deleteFile("test.txt")
            response.statusCode == HttpStatus.OK
            response.body.message == "File deleted"
    }

    def "handleDelete - file not found"() {
        given:
            minioService.deleteFile(_) >> { throw new FileNotFoundException("Not there") }

        when:
            def response = fileService.handleDelete("ghost.txt")

        then:
            response.statusCode == HttpStatus.NOT_FOUND
            response.body.message.contains("Not there")
    }

    def "handleDelete - general exception"() {
        given:
            minioService.deleteFile(_) >> { throw new RuntimeException("Boom") }

        when:
            def response = fileService.handleDelete("explode.txt")

        then:
            response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
            response.body.message.contains("Boom")
    }

    def "listFiles - success"() {
        given:
            def files = [[name: "file1.txt"], [name: "file2.txt"]]
            minioService.listFiles() >> files

        expect:
            fileService.listFiles() == files
    }

    def "listFiles - failure returns empty list"() {
        given:
            minioService.listFiles() >> { throw new RuntimeException("fail") }

        expect:
            fileService.listFiles() == []
    }
}
