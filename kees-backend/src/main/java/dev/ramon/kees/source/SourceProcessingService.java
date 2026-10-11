package dev.ramon.kees.source;

import dev.ramon.kees.chunk.ChunkPersistenceService;
import dev.ramon.kees.chunk.MarkdownChunker;
import dev.ramon.kees.shared.util.DecodeUtf8;
import dev.ramon.kees.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class SourceProcessingService {

    private final SourceRepository sourceRepository;
    private final ChunkPersistenceService chunkPersistenceService;
    private final SourcePersistenceService sourcePersistenceService;
    private final ObjectStorage s3ObjectStorage;
    private final MarkdownChunker chunker;

    public void process(Long sourceId) {
        var source = sourceRepository.findById(sourceId)
                .orElseThrow();

        sourcePersistenceService.markProcessing(source);

            try (var object = s3ObjectStorage.retrieve(source.getObjectKey())) {
                var bytes = object.content().readAllBytes();
                var decoded = DecodeUtf8.decode(bytes);
                var chunkDrafts = chunker.chunk(decoded);

                var chunks = chunkPersistenceService
                        .replaceForSource(source, chunkDrafts);

                var readiedSource = sourcePersistenceService.markReady(source);
            } catch (IOException exception) {
                sourcePersistenceService.markFailed(source.getPublicId());
                throw new RuntimeException(exception);
        }
    }
}
