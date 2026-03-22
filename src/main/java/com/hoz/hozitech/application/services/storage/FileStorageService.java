package com.hoz.hozitech.application.services.storage;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${AWS_ACCESS_KEY:}")
    private String accessKey;

    @Value("${AWS_SECRET_KEY:}")
    private String secretKey;

    @Value("${AWS_REGION:ap-southeast-1}")
    private String region;

    @Value("${AWS_BUCKET_NAME:}")
    private String bucketName;

    private AmazonS3 s3Client;

    @PostConstruct
    public void init() {
        if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
            BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            s3Client = AmazonS3ClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(credentials))
                    .withRegion(region)
                    .build();
        }
    }

    public String storeProductImage(MultipartFile file) {
        if (s3Client == null) {
            throw new RuntimeException("AWS S3 is not configured. Please set AWS_ACCESS_KEY, AWS_SECRET_KEY, and AWS_BUCKET_NAME environment variables.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = "products/" + UUID.randomUUID().toString() + extension;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            s3Client.putObject(new PutObjectRequest(bucketName, key, file.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + key, e);
        }

        return s3Client.getUrl(bucketName, key).toString();
    }

    /**
     * Generic file upload to a specified S3 folder (e.g. "avatars", "banners").
     */
    public String uploadFile(MultipartFile file, String folder) {
        if (s3Client == null) {
            throw new RuntimeException("AWS S3 is not configured.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = folder + "/" + UUID.randomUUID().toString() + extension;

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            s3Client.putObject(new PutObjectRequest(bucketName, key, file.getInputStream(), metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3: " + key, e);
        }

        return s3Client.getUrl(bucketName, key).toString();
    }

    public void deleteFile(String fileUrl) {
        if (s3Client == null || fileUrl == null || fileUrl.isEmpty()) return;

        try {
            // Extract the S3 key from the full URL
            // URL format: https://bucket-name.s3.region.amazonaws.com/products/uuid.ext
            String key = fileUrl.substring(fileUrl.indexOf("products/"));
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, key));
        } catch (Exception e) {
            System.err.println("Failed to delete file from S3: " + fileUrl + " - " + e.getMessage());
        }
    }
}
