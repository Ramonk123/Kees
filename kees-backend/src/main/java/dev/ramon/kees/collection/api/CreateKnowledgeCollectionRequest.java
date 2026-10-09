package dev.ramon.kees.collection.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateKnowledgeCollectionRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        String name,

        @Nullable
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
) { }
