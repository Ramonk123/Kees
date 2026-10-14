package dev.ramon.kees.source;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SourceRepository extends JpaRepository<Source, Long> {

    Optional<Source> findByPublicId(UUID publicId);

    boolean existsByCollection_PublicIdAndSha256AndStatusNot(
            UUID collectionPublicId,
            String sha256,
            SourceStatus status
    );
}
