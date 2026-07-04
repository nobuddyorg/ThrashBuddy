package org.nobuddy.thrashbuddy.config

import io.minio.MinioClient
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.test.context.TestPropertySource

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNotNull
import static org.junit.jupiter.api.Assertions.assertThrows

class MinioConfigSpec {

    @Test
    void "fails to start when MinIO credentials are not configured"() {
        def context = new AnnotationConfigApplicationContext()
        context.register(MinioConfig)
        assertThrows(BeanCreationException) {
            context.refresh()
        }
    }

    @Nested
    @SpringBootTest(classes = [MinioConfig])
    @TestPropertySource(properties = ["MINIO_URL=http://test-minio:9000",
            "MINIO_ACCESS_KEY=testkey",
            "MINIO_SECRET_KEY=testsecret"])
    class WithCustomEnv {

        @Autowired
        ApplicationContext context

        @Test
        void "MinioClient and bucketName beans should be created with custom values"() {
            def minioClient = context.getBean(MinioClient)
            assertNotNull(minioClient)

            def bucketName = context.getBean(String)
            assertEquals("buddy-bucket", bucketName)
        }
    }

}
