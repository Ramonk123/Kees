package dev.ramon.kees.chunk;

import dev.ramon.kees.source.Source;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "chunks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false, updatable = false)
    private Source source;

    @Column(name = "sequence_number", nullable = false, updatable = false)
    private int sequenceNumber;

    @Column(name = "content", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "heading_path", updatable = false, length = 1000)
    private @Nullable String headingPath;

    @Column(name = "content_hash", updatable = false, nullable = false, length = 64)
    private String contentHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Chunk(Source source, int sequenceNumber, String content, @Nullable String headingPath, String contentHash) {
        this.publicId = UUID.randomUUID();
        this.source = Objects.requireNonNull(source);
        this.sequenceNumber = sequenceNumber;
        this.content = content;
        this.headingPath = headingPath;
        this.contentHash = contentHash;
    }

    @PrePersist
    private void setTimestamps() {
        createdAt = Instant.now();
    }

}
