package dev.ramon.kees.chunk;

import org.jspecify.annotations.Nullable;

public record ChunkDraft(
    int sequenceNumber,
    String content,
    @Nullable String headingPath,
    String contentHash
) { }
