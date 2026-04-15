package com.ebikes.workforce.mappers;

import java.time.Instant;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ebikes.workforce.database.entities.Document;
import com.ebikes.workforce.dtos.internal.UploadUrlData;
import com.ebikes.workforce.dtos.responses.documents.DocumentPreviewResponse;
import com.ebikes.workforce.dtos.responses.documents.DocumentResponse;
import com.ebikes.workforce.dtos.responses.documents.DocumentSummaryResponse;
import com.ebikes.workforce.dtos.responses.documents.UploadInitiationResponse;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

  DocumentPreviewResponse toPreviewResponse(
      Document doc, String previewUrl, Instant previewUrlExpiresAt);

  DocumentResponse toResponse(Document document);

  DocumentSummaryResponse toSummaryResponse(Document document);

  @Mapping(target = "expiryTime", source = "uploadUrlData.expiryTime")
  @Mapping(target = "signedHeaders", source = "uploadUrlData.headers")
  @Mapping(target = "url", source = "uploadUrlData.url")
  UploadInitiationResponse toUploadInitiationResponse(
      UUID documentId, String key, UploadUrlData uploadUrlData);
}
