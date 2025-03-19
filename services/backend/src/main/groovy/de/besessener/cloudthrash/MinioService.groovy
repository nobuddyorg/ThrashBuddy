package de.besessener.cloudthrash

import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.GetObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.messages.Item
import io.minio.ListObjectsArgs
import io.minio.Result
import io.minio.errors.MinioException
import org.springframework.stereotype.Service

@Service
class MinioService {

    private final MinioClient minioClient
    private final String bucketName

    MinioService() {
        def endpoint = System.getenv('MINIO_URL') ?: 'http://localhost:9000/'
        def accessKey = System.getenv('MINIO_ACCESS_KEY') ?: 'minioadmin'
        def secretKey = System.getenv('MINIO_SECRET_KEY') ?: 'minioadmin'
        bucketName = System.getenv('MINIO_BUCKET') ?: 'cloud-thrash'
        
        this.minioClient = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()
    }

    void uploadFile(String objectName, InputStream fileStream) {
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(fileStream, fileStream.available(), -1)
                .build()
        )
    }

    InputStream downloadFile(String objectName) {
        minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build()
        )
    }

    void deleteFile(String objectName) {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .build()
        )
    }

    List<Map<String, Object>> listFiles() {
        minioClient.listObjects(ListObjectsArgs.builder().bucket(bucketName).build())
            .collect { result ->
                def item = result.get()
                [
                    filename: item.objectName(),
                    lastModified: item.lastModified()
                ]
            }
    }
}
