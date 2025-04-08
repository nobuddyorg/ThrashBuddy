package de.besessener.cloudthrash.config

import io.minio.MinioClient
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.TestPropertySource

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull

class MinioConfigSpec {

    @Nested
    @SpringBootTest(classes = [MinioConfig])
    @TestPropertySource(properties = [
            "MINIO_URL=http://test-minio:9000",
            "MINIO_ACCESS_KEY=testkey",
            "MINIO_SECRET_KEY=testsecret"
    ])
    class WithCustomEnv {

        @Autowired
        ApplicationContext context

        @Test
        void "MinioClient and bucketName beans should be created with custom values"() {
            def minioClient = context.getBean(MinioClient)
            assertNotNull(minioClient)

            def bucketName = context.getBean(String)
            assertEquals("cloud-thrash", bucketName)
        }
    }

    @Nested
    @SpringBootTest(classes = [MinioConfig])
    class WithDefaultEnv {

        @Autowired
        ApplicationContext context

        @Test
        void "MinioClient and bucketName beans should be created with default values"() {
            def minioClient = context.getBean(MinioClient)
            assertNotNull(minioClient)

            def bucketName = context.getBean(String)
            assertEquals("cloud-thrash", bucketName)
        }
    }
}
