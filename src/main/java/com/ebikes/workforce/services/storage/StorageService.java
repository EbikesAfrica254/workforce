package com.ebikes.workforce.services.storage;

import com.ebikes.workforce.configurations.properties.AwsProperties;
import com.ebikes.workforce.dtos.internal.UploadUrlData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.Map;

@RequiredArgsConstructor
@Service
@Slf4j
public class StorageService {

  private final AwsProperties awsProperties;
  private final S3Presigner s3Presigner;

  public String generateDownloadUrl(String key, String fileName, Duration ttl) {
    String disposition = "attachment; filename=\"" + sanitizeFilename(fileName) + "\"";
    return buildPresignedGetUrl(key, ttl, disposition);
  }

  public String generatePreviewUrl(String key, Duration ttl) {
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
