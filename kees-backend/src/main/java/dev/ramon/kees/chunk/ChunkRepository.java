package dev.ramon.kees.chunk;

import dev.ramon.kees.source.Source;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChunkRepository extends JpaRepository<Chunk, Long> {

    List<Chunk> findAllBySourceOrderBySequenceNumberAsc(Source source);

    void deleteAllBySource(Source source);
}
