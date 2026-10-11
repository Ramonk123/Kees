package dev.ramon.kees.chunk;

import dev.ramon.kees.shared.exception.ResourceNotFoundException;
import dev.ramon.kees.source.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChunkService {

    private final ChunkRepository repository;
    private final SourceRepository sourceRepository;

    public List<Chunk> getChunksFromSource(UUID sourceId) {
        var source = sourceRepository.findByPublicId(sourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Source", sourceId.toString()));

        return repository.findAllBySourceOrderBySequenceNumberAsc(source);
    }
}
