package org.nobuddy.thrashbuddy.service

import io.minio.*
import io.minio.messages.Item
import org.nobuddy.thrashbuddy.ThrashBuddyApplication
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import spock.lang.Specification
import spock.mock.DetachedMockFactory

import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@SpringBootTest(classes = ThrashBuddyApplication)
@Import(MockMinioConfig)
class MinioServiceSpec extends Specification {

    @Autowired
    MinioClient minioClient
    @Autowired
    MinioService minioService

    def "uploads file successfully"() {
        given:
            def content = "Test content"
            def stream = new ByteArrayInputStream(content.bytes)

        when:
            minioService.uploadFile("test.txt", stream)

        then:
            1 * minioClient.putObject(_ as PutObjectArgs)
    }

    def "downloads file successfully"() {
        given:
            def expectedContent = "Downloaded content"
            def sourceStream = new ByteArrayInputStream(expectedContent.bytes)

            def response = Mock(GetObjectResponse) {
                read(_) >> { byte[] b -> sourceStream.read(b) }
                read(_, _, _) >> { byte[] b, int off, int len -> sourceStream.read(b, off, len) }
            }

        when:
            def result = minioService.downloadFile("test.txt")

        then:
            1 * minioClient.getObject(_ as GetObjectArgs) >> response
            result.text == expectedContent
    }

    def "deletes file successfully"() {
        when:
            minioService.deleteFile("test.txt")

        then:
            1 * minioClient.removeObject(_ as RemoveObjectArgs)
    }

    def "list files successfully"() {
        given:
            def zonedTime = ZonedDateTime.ofInstant(Instant.parse("2024-01-01T10:00:00Z"), ZoneOffset.UTC)

            def itemMock = Mock(Item) {
                objectName() >> "file1.txt"
                lastModified() >> zonedTime
            }

            def resultMock = Mock(Result) {
                get() >> itemMock
            }

            def iterable = [resultMock]

        when:
            def result = minioService.listFiles()

        then:
            1 * minioClient.listObjects({
                it instanceof ListObjectsArgs && it.bucket() == 'buddy-bucket3'
            }) >> iterable

        and:
            result == [[filename: "file1.txt", lastModified: zonedTime]]
    }
}

@TestConfiguration
class MockMinioConfig {

    DetachedMockFactory mockFactory = new DetachedMockFactory()

    @Bean
    MinioClient minioClient() {
        return mockFactory.Mock(MinioClient)
    }

    @Bean
    String bucketName() {
        return 'buddy-bucket3'
    }
}
