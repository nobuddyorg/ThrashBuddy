package org.nobuddy.thrashbuddy.service

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

    // no path separators, no leading dot, plain-ish filenames only
    private static final def SAFE_FILE_NAME = ~/^[A-Za-z0-9][A-Za-z0-9._-]{0,254}$/

    @Autowired
    MinioService minioService

    ResponseEntity<Map> handleUpload(MultipartFile file) {
        try {
            validateFileName(file.originalFilename)
            minioService.uploadFile(file.originalFilename, file.inputStream)
            return buildResponse(HttpStatus.OK, "File uploaded")
        } catch (IllegalArgumentException e) {
            return buildResponse(HttpStatus.BAD_REQUEST, e.message)
        } catch (Exception e) {
            return buildResponse(HttpStatus.BAD_REQUEST, "Upload error: ${e.message}")
        }
    }

    ResponseEntity<?> handleDownload(String fileName) {
        try {
            validateFileName(fileName)
            def stream = minioService.downloadFile(fileName)
            if (!stream) return buildResponse(HttpStatus.NOT_FOUND, "File not found")

            def resource = new ByteArrayResource(stream.readAllBytes())
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$fileName")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource)
        } catch (IllegalArgumentException e) {
            return buildResponse(HttpStatus.BAD_REQUEST, e.message)
        } catch (Exception e) {
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Download error: ${e.message}")
        }
    }

    ResponseEntity<Map> handleDelete(String fileName) {
        try {
            validateFileName(fileName)
            minioService.deleteFile(fileName)
            return buildResponse(HttpStatus.OK, "File deleted")
        } catch (IllegalArgumentException e) {
            return buildResponse(HttpStatus.BAD_REQUEST, e.message)
        } catch (FileNotFoundException e) {
            return buildResponse(HttpStatus.NOT_FOUND, "Not found: ${e.message}")
        } catch (Exception e) {
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Delete error: ${e.message}")
        }
    }

    private static void validateFileName(String fileName) {
        if (!fileName || !(fileName ==~ SAFE_FILE_NAME)) {
            throw new IllegalArgumentException("Invalid file name: $fileName" as String)
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

    private static ResponseEntity<Map> buildResponse(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body([message   : msg,
                                                   httpStatus: status.reasonPhrase])
    }
}
