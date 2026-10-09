package dev.ramon.kees.source;

import dev.ramon.kees.collection.KnowledgeCollection;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "sources")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "original_filename", nullable = false, updatable = false, length = 255)
    private String fileName;

    @Column(name = "object_key", nullable = false, updatable = false, length = 1024)
    private String objectKey;

    @Column(name = "content_type", nullable = false, updatable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SourceContentType contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long size;

    @Column(name = "sha256", nullable = false, updatable = false, length = 64)
    private String sha256;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SourceStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "collection_id", nullable = false, updatable = false)
    private KnowledgeCollection collection;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Source(
            KnowledgeCollection collection,
            String fileName,
            String objectKey,
            SourceContentType contentType,
            long size,
            String sha256
    ) {
        this.publicId = UUID.randomUUID();
        this.collection = Objects.requireNonNull(
                collection,
                "Collection cannot be null"
        );
        this.fileName = requireFileName(fileName);
        this.objectKey = requireObjectKey(objectKey);
        this.contentType = Objects.requireNonNull(
                contentType,
                "Content type cannot be null"
        );
        this.size = requirePositiveSize(size);
        this.sha256 = requireSha256(sha256);
        this.status = SourceStatus.PENDING_UPLOAD;
    }

    @PrePersist
    private void setTimestamps() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void updateTimestamp() {
        updatedAt = Instant.now();
    }

    private static String requireFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "File name cannot be null or blank"
            );
        }

        var normalized = fileName.trim();

        if (normalized.length() > 255) {
            throw new IllegalArgumentException(
                    "File name cannot exceed 255 characters"
            );
        }

        return normalized;
    }

    private static String requireObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Object key cannot be null or blank"
            );
        }

        var normalized = objectKey.trim();

        if (normalized.length() > 1024) {
            throw new IllegalArgumentException(
                    "Object key cannot exceed 1024 characters"
            );
        }

        return normalized;
    }

    private static long requirePositiveSize(long size) {
        if (size <= 0) {
            throw new IllegalArgumentException(
                    "Source size must be greater than zero"
            );
        }

        return size;
    }

    private static String requireSha256(String sha256) {
        if (sha256 == null || !sha256.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalArgumentException(
                    "SHA-256 must contain exactly 64 hexadecimal characters"
            );
        }

        return sha256.toLowerCase(Locale.ROOT);
    }
}
