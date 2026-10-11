package dev.ramon.kees.chunk;

import dev.ramon.kees.source.Source;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChunkPersistenceService {

    private final ChunkRepository repository;


    @Transactional
    public List<Chunk> replaceForSource(Source source, List<ChunkDraft> drafts) {
        repository.deleteAllBySource(source);
        repository.flush();

        var chunks = drafts
                .stream()
                .sorted(Comparator.comparingInt(ChunkDraft::sequenceNumber))
                .map((d) -> new Chunk(
                        source,
                        d.sequenceNumber(),
                        d.content(),
                        d.headingPath(),
                        d.contentHash()
                )).toList();

        return repository.saveAll(chunks);
    }
}
