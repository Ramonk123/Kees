package dev.ramon.kees.source;

import dev.ramon.kees.storage.ObjectStorage;
import dev.ramon.kees.storage.UploadFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceUploadServiceTest {

    @Mock
    private SourceUploadProperties properties;

    @Mock
    private SourcePersistenceService sourcePersistenceService;

    @Mock
    private ObjectStorage objectStorage;

    @InjectMocks
    private SourceUploadService service;

    @Test
    void uploadsMarkdownAndMarksSourceAsStored() throws Exception {
        var collectionId = UUID.randomUUID();
        var sourceId = UUID.randomUUID();
        var content = "hello world".getBytes(StandardCharsets.UTF_8);
        var sha256 = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9";

        var file = new MockMultipartFile(
                "file",
                "test-file.md",
                "text/plain",
                content
        );

        var pendingSource = mock(Source.class);
        var storedSource = mock(Source.class);

        when(properties.maxFileSizeInBytes()).thenReturn(5L * 1024 * 1024);
        when(pendingSource.getPublicId()).thenReturn(sourceId);

        when(sourcePersistenceService.createPending(
                any(UUID.class),
                eq(collectionId),
                eq("test-file.md"),
                anyString(),
                eq((long) content.length),
                eq(sha256)
        )).thenReturn(pendingSource);

        when(sourcePersistenceService.markStored(sourceId)).thenReturn(storedSource);

        var uploadedContent = new AtomicReference<byte[]>();

        doAnswer(invocation -> {
            var inputStream = invocation.getArgument(1, InputStream.class);
            uploadedContent.set(inputStream.readAllBytes());
            return null;
        }).when(objectStorage).store(
                anyString(),
                any(InputStream.class),
                anyLong(),
                anyString()
        );

        var result = service.upload(collectionId, file);

        assertThat(result).isSameAs(storedSource);
        assertThat(uploadedContent.get()).containsExactly(content);

        verify(objectStorage).store(
                anyString(),
                any(InputStream.class),
                eq((long) content.length),
                eq("text/markdown; charset=utf-8")
        );

        verify(sourcePersistenceService).markStored(sourceId);
        verify(sourcePersistenceService, never()).markFailed(any());
    }

    @ParameterizedTest
    @MethodSource("validFileNames")
    void normalizedFileNames(String submittedFilename, String expectedFilename) {
        var collectionId = UUID.randomUUID();
        var sourceId = UUID.randomUUID();

        var content = "Test content".getBytes(StandardCharsets.UTF_8);

        var file = new MockMultipartFile(
                "file",
                submittedFilename,
                "text/plain",
                content
        );

        var pendingSource = mock(Source.class);

        when(properties.maxFileSizeInBytes()).thenReturn(5L * 1024 * 1024);
        when(pendingSource.getPublicId()).thenReturn(sourceId);
        when(sourcePersistenceService.createPending(
                any(UUID.class),
                eq(collectionId),
                eq(expectedFilename),
                anyString(),
                eq((long) content.length),
                anyString()
        )).thenReturn(pendingSource);

        service.upload(collectionId, file);

        var filenameCaptor = ArgumentCaptor.forClass(String.class);

        verify(sourcePersistenceService).createPending(
                any(UUID.class),
                eq(collectionId),
                filenameCaptor.capture(),
                anyString(),
                eq((long) content.length),
                anyString());

        assertThat(filenameCaptor.getValue()).isEqualTo(expectedFilename);
    }

    @Test
    void marksSourceAsFailedWhenObjectStorageUploadFails() throws IOException {
        var collectionId = UUID.randomUUID();
        var sourceId = UUID.randomUUID();
        var content = "hello world".getBytes(StandardCharsets.UTF_8);
        var pendingSource = mock(Source.class);

        var file = new MockMultipartFile(
                "file",
                "file-name.md",
                "text/plain",
                content
        );

        when(properties.maxFileSizeInBytes()).thenReturn(5L * 1024 * 1024);
        when(pendingSource.getPublicId()).thenReturn(sourceId);

        when(sourcePersistenceService.createPending(
                any(UUID.class),
                eq(collectionId),
                eq("file-name.md"),
                anyString(),
                eq((long) content.length),
                anyString()
        )).thenReturn(pendingSource);

        var storageFailure = new UploadFailedException("object-key", new RuntimeException("Storage failure"));
        doThrow(storageFailure).when(objectStorage).store(
                anyString(),
                any(InputStream.class),
                eq((long) content.length),
                eq("text/markdown; charset=utf-8")
        );

        assertThatThrownBy(() -> service.upload(collectionId, file))
                .isSameAs(storageFailure);

        verify(sourcePersistenceService).markFailed(sourceId);
        verify(sourcePersistenceService, never()).markStored(any());
        verify(objectStorage, never()).delete(anyString());
    }

    @Test
    void rejectsMalformedUtf8BeforeCreatingSource() {
        var collectionId = UUID.randomUUID();

        var malformedBytes = new byte[] {
                (byte) 0xC3,
                (byte) 0x28,
        };

        var file = new MockMultipartFile(
                "file",
                "file-name.md",
                "text/markdown",
                malformedBytes
        );

        when(properties.maxFileSizeInBytes()).thenReturn(5L * 1024 * 1024);

        assertThatThrownBy(() -> service.upload(collectionId, file))
                .isInstanceOf(InvalidSourceFileException.class)
                .hasMessage("File must contain valid UTF-8 text");

        verifyNoInteractions(
                sourcePersistenceService,
                objectStorage
        );
    }

    @Test
    void rejectsEmptyFileWhenUploading() {
        var collectionId = UUID.randomUUID();

        when(properties.maxFileSizeInBytes()).thenReturn(5L * 1024 * 1024);
        var file = new MockMultipartFile(
                "file",
                "file-name.md",
                "text/markdown",
                new byte[0]
        );

        assertThatThrownBy(() -> service.upload(collectionId, file))
                .isInstanceOf(InvalidSourceFileException.class)
                .hasMessage("File cannot be empty");

    }

    private static Stream<Arguments> validFileNames() {
        return Stream.of(
                Arguments.of(
                        "notes.md",
                        "notes.md"
                ),
                Arguments.of(
                        "/home/user/documents/file.md",
                        "file.md"
                ),
                Arguments.of(
                        "C:\\Users\\user\\Documents\\file.md",
                        "file.md"
                ),
                Arguments.of(
                        "   TEST.md",
                        "TEST.md"
                )
        );
    }
}