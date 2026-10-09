package dev.ramon.kees.storage;

import java.io.InputStream;

public interface ObjectStorage {

    void store(
            String key,
            InputStream content,
            long contentLength,
            String contentType
    );

    StoredObject retrieve(String key);

    void delete(String key);
}
