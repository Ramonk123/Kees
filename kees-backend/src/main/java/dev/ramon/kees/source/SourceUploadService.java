package dev.ramon.kees.source;

import dev.ramon.kees.shared.util.DecodeUtf8;
import dev.ramon.kees.shared.util.HashContent;
import dev.ramon.kees.storage.ObjectStorage;
import dev.ramon.kees.storage.ObjectStorageException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SourceUploadService {

    private final SourceUploadProperties properties;
    private final SourcePersistenceService sourcePersistenceService;
    private final SourceProcessingService sourceProcessingService;
    private final ObjectStorage objectStorage;

    public Source upload(
            UUID collectionId,
            MultipartFile file
    ) {
        var MAX_FILE_SIZE = properties.maxFileSizeInBytes();

        if (file.isEmpty()) {
            throw new InvalidSourceFileException("File cannot be empty");
        } else if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidSourceFileException("File size exceeds the maximum allowed size");
        }

        var fileName = normalizeFileName(file.getOriginalFilename());
        var content = readBytes(file);

        if (content.length == 0) {
            throw new InvalidSourceFileException("File cannot be empty");
        } else if (content.length > MAX_FILE_SIZE) {
            throw new InvalidSourceFileException("File size exceeds the maximum allowed size");
        }

        validateTextContent(content);

        var sha256 = HashContent.hash(content);

        var sourcePublicId = UUID.randomUUID();

        var key = generateStorageKey(collectionId, sourcePublicId);

        var pendingSource = sourcePersistenceService.createPending(
                sourcePublicId,
                collectionId,
                fileName,
                key,
                content.length,
                sha256
        );

        try {
            objectStorage.store(
                    key,
                    new ByteArrayInputStream(content),
                    content.length,
                    "text/markdown; charset=utf-8"
            );

        } catch (ObjectStorageException exception) {
            sourcePersistenceService.markFailed(pendingSource.getPublicId());
            throw exception;
        }

        sourcePersistenceService.markStored(pendingSource.getPublicId());
        return sourceProcessingService.process(pendingSource.getPublicId());
    }

    private String normalizeFileName(@Nullable String originalFilename) {
        var fileName = getFilename(originalFilename);

        if (fileName.length() > 255) {
            throw new InvalidSourceFileException("Filename cannot exceed 255 characters");
        }

        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".md")) {
            throw new InvalidSourceFileException("Only markdown files ending with .md extension are allowed");
        }

        return fileName;
    }

    private static String getFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidSourceFileException("A filename is required");
        }

        var normalizedPath = originalFilename.replace('\\', '/');
        var fileName = normalizedPath
                .substring(normalizedPath.lastIndexOf('/') + 1)
                .strip();

        if (fileName.isBlank() || fileName.equals(".") || fileName.equals("..")) {
            throw new InvalidSourceFileException("A filename is required");
        }
        return fileName;
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new SourceFileReadException("Could not read uploaded file", exception);
        }
    }

    private static void validateTextContent(byte[] bytes) {
        var content = DecodeUtf8.decode(bytes);
        if (content.isBlank()) {
            throw new InvalidSourceFileException("File content cannot be empty");
        }

        var text = content.startsWith("\uFEFF")
                ? content.substring(1)
                : content;
        if (text.isBlank()) {
            throw new InvalidSourceFileException("File content cannot be empty");
        }

        if (text.contains("\u0000")) {
            throw new InvalidSourceFileException("File content cannot contain null characters");
        }
    }

    private static String generateStorageKey(UUID collectionId, UUID sourceId) {
        return String.format("collections/%s/sources/$%s/original.md", collectionId.toString(), sourceId.toString());
    }
}
