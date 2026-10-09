package dev.ramon.kees.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public record StoredObject(
        InputStream content,
        long contentLength,
        String contentType
) implements AutoCloseable {

    public StoredObject {
        Objects.requireNonNull(content, "Content cannot be null");
        Objects.requireNonNull(contentType, "Content type cannot be null");

        if (contentLength < 0) {
            throw new IllegalArgumentException("Content length cannot be negative");
        }
    }

    @Override
    public void close() throws IOException {
        content.close();
    }
}