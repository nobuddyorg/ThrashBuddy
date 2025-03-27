package de.besessener.cloudthrash

import io.minio.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MinioService {

    private final MinioClient minioClient
    private final String bucketName

    @Autowired
    MinioService(MinioClient minioClient, String bucketName) {
        this.minioClient = minioClient
        this.bucketName = bucketName
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
                            filename    : item.objectName(),
                            lastModified: item.lastModified()
                    ]
                }
    }
}
