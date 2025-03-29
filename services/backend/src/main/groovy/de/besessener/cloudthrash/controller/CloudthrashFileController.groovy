package de.besessener.cloudthrash.controller

import de.besessener.cloudthrash.service.FileService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
class CloudthrashFileController {

    @Autowired
    FileService fileService

    @PostMapping("/upload")
    ResponseEntity<Map> uploadFile(@RequestParam("file") MultipartFile file) {
        return fileService.handleUpload(file)
    }

    @GetMapping("/download")
    ResponseEntity<?> downloadFile(@RequestParam("fileName") String fileName) {
        return fileService.handleDownload(fileName)
    }

    @DeleteMapping("/delete")
    ResponseEntity<Map> deleteFile(@RequestParam("fileName") String fileName) {
        return fileService.handleDelete(fileName)
    }
}
