package de.besessener.cloudthrash

import io.minio.MinioClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnMissingBean(MinioClient)
class MinioConfig {

    @Bean
    MinioClient minioClient() {
        def endpoint = System.getenv('MINIO_URL') ?: 'http://localhost:9000/'
        def accessKey = System.getenv('MINIO_ACCESS_KEY') ?: 'minioadmin'
        def secretKey = System.getenv('MINIO_SECRET_KEY') ?: 'minioadmin'

        MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build()
    }

    @Bean
    String bucketName() {
        'cloud-thrash'
    }
}

