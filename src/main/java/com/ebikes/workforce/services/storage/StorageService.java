package com.ebikes.workforce.services.storage;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ebikes.workforce.configurations.properties.AwsProperties;
import com.ebikes.workforce.dtos.internal.StoredFileMetadata;
import com.ebikes.workforce.dtos.internal.UploadUrlData;
import com.ebikes.workforce.enums.DocumentType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@RequiredArgsConstructor
@Service
@Slf4j
public class StorageService {

  private final AwsProperties awsProperties;
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  public String generateDownloadUrl(String key, String fileName) {
    Duration ttl = Duration.ofMinutes(awsProperties.getS3().getPresignedUrlExpiryMinutes());
    String disposition = "attachment; filename=\"" + sanitizeFilename(fileName) + "\"";
    return buildPresignedGetUrl(key, ttl, disposition);
  }

  public String generateKey(DocumentType documentType, UUID documentId, String fileName) {
    String sanitized = sanitizeFilename(fileName);
    return String.format(
        "documents/%s/%s-%s-%s", documentType, documentId, System.currentTimeMillis(), sanitized);
  }

  public String generatePreviewUrl(String key) {
    Duration ttl = Duration.ofMinutes(awsProperties.getS3().getPreviewUrlExpiryMinutes());
    return buildPresignedGetUrl(key, ttl, "inline");
  }

  public UploadUrlData generateUploadUrl(
      String key, String contentType, Long contentLength, Map<String, String> metadata) {

    String bucketName = awsProperties.getS3().getBucketName();
    Integer expiryMinutes = awsProperties.getS3().getPresignedUrlExpiryMinutes();

    log.info(
        "Generating upload URL: bucket={}, key={}, contentType={}, expiryMinutes={}",
        bucketName,
        key,
        contentType,
        expiryMinutes);

    PresignedPutObjectRequest presignedRequest =
        s3Presigner.presignPutObject(
            p ->
                p.signatureDuration(Duration.ofMinutes(expiryMinutes))
                    .putObjectRequest(
                        r -> {
                          r.bucket(bucketName)
                              .key(key)
                              .contentType(contentType)
                              .contentLength(contentLength);
                          if (metadata != null && !metadata.isEmpty()) {
                            r.metadata(metadata);
                          }
                        }));

    log.debug("Upload URL generated: key={}, expiresAt={}", key, presignedRequest.expiration());

    return new UploadUrlData(
        presignedRequest.url().toString(),
        presignedRequest.signedHeaders(),
        presignedRequest.expiration());
  }

  public StoredFileMetadata getStoredFileMetadata(String key) {
    String bucketName = awsProperties.getS3().getBucketName();
    log.info("Retrieving file metadata: bucket={}, key={}", bucketName, key);

    try {
      HeadObjectResponse response =
          s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());

      log.debug(
          "File metadata retrieved: key={}, size={}, contentType={}",
          key,
          response.contentLength(),
          response.contentType());

      return new StoredFileMetadata(
          response.contentLength(), response.contentType(), response.lastModified(), true);

    } catch (NoSuchKeyException e) {
      log.warn("File not found: bucket={}, key={}", bucketName, key);
      return new StoredFileMetadata(null, null, null, false);
    }
  }

  public Instant previewUrlExpiresAt() {
    return Instant.now()
        .plus(Duration.ofMinutes(awsProperties.getS3().getPreviewUrlExpiryMinutes()));
  }

  public String sanitizeFilename(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      throw new IllegalArgumentException("Filename cannot be null or blank");
    }
    return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  private String buildPresignedGetUrl(String key, Duration ttl, String contentDisposition) {
    String bucketName = awsProperties.getS3().getBucketName();

    log.info(
        "Generating presigned GET URL: bucket={}, key={}, ttl={}m, disposition={}",
        bucketName,
        key,
        ttl.toMinutes(),
        contentDisposition);

    PresignedGetObjectRequest presignedRequest =
        s3Presigner.presignGetObject(
            p ->
                p.signatureDuration(ttl)
                    .getObjectRequest(
                        r ->
                            r.bucket(bucketName)
                                .key(key)
                                .responseContentDisposition(contentDisposition)));

    log.debug(
        "Presigned GET URL generated: key={}, expiresAt={}", key, presignedRequest.expiration());

    return presignedRequest.url().toString();
  }
}
