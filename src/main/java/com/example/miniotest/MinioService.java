package com.example.miniotest;

import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.Objects;
import java.util.UUID;

@Service
public class MinioService {

    private final MinioConfig config;

    private MinioClient client;

    public MinioService(MinioConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        client = MinioClient.builder()
                .endpoint(config.getUrl())
                .credentials(config.getAccessKey(), config.getSecretKey())
                .build();

        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder()
                    .bucket(config.getBucket()).build());

            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(config.getBucket()).build());
            }

        } catch (Exception e) {
            throw new RuntimeException("Error initializing MinIO", e);
        }
    }

    public String uploadFile(MultipartFile file) {
        String normalized = Normalizer.normalize(Objects.requireNonNull(file.getOriginalFilename()), Normalizer.Form.NFD);

        String withoutDiacritics = normalized.replaceAll("\\p{M}", "");

        withoutDiacritics = withoutDiacritics.replace("đ", "d").replace("Đ", "D");

        // Bước 4: Chuyển các ký tự không hợp lệ thành "_"
        String sanitized = withoutDiacritics.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Bước 5: Ghép với UUID
        String fileName = UUID.randomUUID() + "-" + sanitized;

        try (InputStream is = file.getInputStream()) {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(fileName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }

        return fileName;
    }

    public String getPresignedUrl(String fileName) {
        try {
            // Check if object exists first
            client.statObject(
                    StatObjectArgs.builder()
                            .bucket(config.getBucket())
                            .object(fileName)
                            .build()
            );

            // Only create URL if it exists
            return client.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(config.getBucket())
                            .object(fileName)
                            .method(Method.GET)
                            .expiry(60 * 60)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                throw new RuntimeException("File not found: " + fileName);
            }
            throw new RuntimeException("Error checking object existence", e);
        } catch (Exception e) {
            throw new RuntimeException("Get URL failed", e);
        }
    }

}

