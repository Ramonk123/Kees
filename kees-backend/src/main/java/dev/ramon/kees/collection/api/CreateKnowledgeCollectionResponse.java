package dev.ramon.kees.collection.api;

import dev.ramon.kees.collection.KnowledgeCollection;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record CreateKnowledgeCollectionResponse (
    UUID publicId,
    String name,
    @Nullable String description,
    Instant createdAt,
    Instant updatedAt
) {
    public static CreateKnowledgeCollectionResponse from(KnowledgeCollection collection) {
        return new CreateKnowledgeCollectionResponse(
            collection.getPublicId(),
            collection.getName(),
            collection.getDescription(),
            collection.getCreatedAt(),
            collection.getUpdatedAt()
        );
    }
}
