package dev.ramon.kees.chunk.api;

import dev.ramon.kees.chunk.Chunk;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record ChunkResponse(
        UUID publicId,
        @Nullable String heading,
        String content,
        int sequenceNumber,
        String contentHash
) {
    public static ChunkResponse from(Chunk chunk) {
        return new ChunkResponse(
                chunk.getPublicId(),
                chunk.getHeadingPath(),
                chunk.getContent(),
                chunk.getSequenceNumber(),
                chunk.getContentHash()
        );
    }
}
