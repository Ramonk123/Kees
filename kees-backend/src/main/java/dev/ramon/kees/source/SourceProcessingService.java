package dev.ramon.kees.source;

import dev.ramon.kees.chunk.ChunkPersistenceService;
import dev.ramon.kees.chunk.MarkdownChunker;
import dev.ramon.kees.shared.util.DecodeUtf8;
import dev.ramon.kees.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SourceProcessingService {

    private final ChunkPersistenceService chunkPersistenceService;
    private final SourcePersistenceService sourcePersistenceService;
    private final ObjectStorage objectStorage;
    private final MarkdownChunker chunker;

    public Source process(UUID sourcePublicId) {
        var source = sourcePersistenceService.markProcessing(sourcePublicId);

        try (var object = objectStorage.retrieve(source.getObjectKey())) {
            var bytes = object.content().readAllBytes();
            var markdown = DecodeUtf8.decode(bytes);
            var drafts = chunker.chunk(markdown);

            chunkPersistenceService.replaceForSource(source, drafts);

            return sourcePersistenceService.markReady(sourcePublicId);
        } catch (IOException exception) {
            sourcePersistenceService.markProcessingFailed(sourcePublicId);
            throw new SourceFileReadException(
                    "Could not read stored source file",
                    exception
            );
        } catch (RuntimeException exception) {
            sourcePersistenceService.markProcessingFailed(sourcePublicId);
            throw exception;
        }
    }
}
