package com.example.miniotest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class MinioController {

    private final MinioService minioService;

    public MinioController(MinioService minioService) {
        this.minioService = minioService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam MultipartFile file) {
        String filename = minioService.uploadFile(file);
        return ResponseEntity.ok(filename);
    }

    @GetMapping("/url")
    public ResponseEntity<String> getUrl(@RequestParam String filename) {
        String url = minioService.getPresignedUrl(filename);
        System.out.println(url);
        return ResponseEntity.ok(url);
    }
}

