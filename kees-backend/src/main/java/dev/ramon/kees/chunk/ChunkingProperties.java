package dev.ramon.kees.chunk;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "kees.chunking")
public record ChunkingProperties(
        @Positive int maxCharacters
) { }
