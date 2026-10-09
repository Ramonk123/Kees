package dev.ramon.kees.collection;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "knowledge_collections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KnowledgeCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public KnowledgeCollection(String name, String description) {
        this.name = requireName(name);
        this.description = normalizeDescription(description);
    }

    @PrePersist
    private void setTimestamps() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void updateTimestamps() {
        updatedAt = Instant.now();
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Collection name cannot be null or blank");
        }

        var normalizedName = name.trim();

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException("Collection name cannot exceed 100 characters");
        }

        return normalizedName;
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        var normalizedDescription = description.trim();

        if (normalizedDescription.length() > 500) {
            throw new IllegalArgumentException("Collection description cannot exceed 500 characters");
        }

        return normalizedDescription;
    }
}