package dev.ramon.kees.source.api;

import dev.ramon.kees.source.Source;
import dev.ramon.kees.source.SourceContentType;
import dev.ramon.kees.source.SourceStatus;

import java.time.Instant;
import java.util.UUID;

public record SourceResponse(
        UUID publicId,
        String fileName,
        SourceContentType contentType,
        long size,
        String sha256,
        SourceStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static SourceResponse from(Source source) {
        return new SourceResponse(
                source.getPublicId(),
                source.getFileName(),
                source.getContentType(),
                source.getSize(),
                source.getSha256(),
                source.getStatus(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }
}