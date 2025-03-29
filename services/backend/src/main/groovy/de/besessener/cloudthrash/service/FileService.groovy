package de.besessener.cloudthrash.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class FileService {

    private static final log = LoggerFactory.getLogger(FileService)

    @Autowired
    MinioService minioService

    ResponseEntity<Map> handleUpload(MultipartFile file) {
        try {
            minioService.uploadFile(file.originalFilename, file.inputStream)
            return buildResponse(HttpStatus.OK, "File uploaded")
        } catch (Exception e) {
            return buildResponse(HttpStatus.BAD_REQUEST, "Upload error: ${e.message}")
        }
    }

    ResponseEntity<?> handleDownload(String fileName) {
        try {
            def stream = minioService.downloadFile(fileName)
            if (!stream) return buildResponse(HttpStatus.NOT_FOUND, "File not found")

            def resource = new ByteArrayResource(stream.readAllBytes())
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$fileName")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource)
        } catch (Exception e) {
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Download error: ${e.message}")
        }
    }

    ResponseEntity<Map> handleDelete(String fileName) {
        try {
            minioService.deleteFile(fileName)
            return buildResponse(HttpStatus.OK, "File deleted")
        } catch (FileNotFoundException e) {
            return buildResponse(HttpStatus.NOT_FOUND, "Not found: ${e.message}")
        } catch (Exception e) {
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Delete error: ${e.message}")
        }
    }

    List<Map> listFiles() {
        try {
            return minioService.listFiles()
        } catch (Exception e) {
            log.warn("Failed to list files", e)
            return []
        }
    }

    private ResponseEntity<Map> buildResponse(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body([
                message   : msg,
                httpStatus: status.reasonPhrase
        ])
    }
}
