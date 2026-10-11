package dev.ramon.kees.source;

import dev.ramon.kees.chunk.ChunkDraft;
import dev.ramon.kees.chunk.ChunkPersistenceService;
import dev.ramon.kees.chunk.MarkdownChunker;
import dev.ramon.kees.storage.ObjectStorage;
import dev.ramon.kees.storage.StoredObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceProcessingServiceTest {

    @Mock
    private ChunkPersistenceService chunkPersistenceService;

    @Mock
    private SourcePersistenceService sourcePersistenceService;

    @Mock
    private ObjectStorage objectStorage;

    @Mock
    private MarkdownChunker chunker;

    @InjectMocks
    private SourceProcessingService service;

    @Test
    void processesStoredSourceAndMarksItReady() {
        var sourcePublicId = UUID.randomUUID();
        var objectKey = "collections/collection/sources/source/original.md";
        var markdown = "# Java\n\nJava content";
        var bytes = markdown.getBytes(StandardCharsets.UTF_8);
        var source = mock(Source.class);
        var readySource = mock(Source.class);
        var drafts = List.of(
                new ChunkDraft(0, "Java content", "Java", "a".repeat(64))
        );

        when(sourcePersistenceService.markProcessing(sourcePublicId)).thenReturn(source);
        when(source.getObjectKey()).thenReturn(objectKey);
        when(objectStorage.retrieve(objectKey)).thenReturn(new StoredObject(
                new ByteArrayInputStream(bytes),
                bytes.length,
                "text/markdown; charset=utf-8"
        ));
        when(chunker.chunk(markdown)).thenReturn(drafts);
        when(sourcePersistenceService.markReady(sourcePublicId)).thenReturn(readySource);

        var result = service.process(sourcePublicId);

        assertThat(result).isSameAs(readySource);
        verify(chunkPersistenceService).replaceForSource(source, drafts);
        verify(sourcePersistenceService).markReady(sourcePublicId);
        verify(sourcePersistenceService, never()).markProcessingFailed(any());
    }

    @Test
    void marksSourceAsFailedWhenProcessingFails() {
        var sourcePublicId = UUID.randomUUID();
        var objectKey = "collections/collection/sources/source/original.md";
        var bytes = "content".getBytes(StandardCharsets.UTF_8);
        var source = mock(Source.class);
        var processingFailure = new IllegalStateException("Chunking failed");

        when(sourcePersistenceService.markProcessing(sourcePublicId)).thenReturn(source);
        when(source.getObjectKey()).thenReturn(objectKey);
        when(objectStorage.retrieve(objectKey)).thenReturn(new StoredObject(
                new ByteArrayInputStream(bytes),
                bytes.length,
                "text/markdown; charset=utf-8"
        ));
        when(chunker.chunk("content")).thenThrow(processingFailure);

        assertThatThrownBy(() -> service.process(sourcePublicId))
                .isSameAs(processingFailure);

        verify(sourcePersistenceService).markProcessingFailed(sourcePublicId);
        verify(chunkPersistenceService, never()).replaceForSource(any(), any());
        verify(sourcePersistenceService, never()).markReady(any());
    }
}
