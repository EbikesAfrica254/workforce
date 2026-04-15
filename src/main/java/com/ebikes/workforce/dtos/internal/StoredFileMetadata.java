package com.ebikes.workforce.dtos.internal;

import java.time.Instant;

public record StoredFileMetadata(
    Long size, String contentType, Instant lastModified, boolean exists) {}
