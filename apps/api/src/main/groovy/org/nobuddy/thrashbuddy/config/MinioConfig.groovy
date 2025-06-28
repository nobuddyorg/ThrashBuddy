package org.nobuddy.thrashbuddy.config

import io.minio.MinioClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnMissingBean(MinioClient)
class MinioConfig {

    @Bean
    MinioClient minioClient(@Value('${MINIO_URL:http://localhost:9000/}') String endpoint,
                            @Value('${MINIO_ACCESS_KEY:minioadmin}') String accessKey,
                            @Value('${MINIO_SECRET_KEY:minioadmin}') String secretKey) {
        MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build()
    }

    @Bean
    String bucketName() {
        'buddy-bucket'
    }
}
