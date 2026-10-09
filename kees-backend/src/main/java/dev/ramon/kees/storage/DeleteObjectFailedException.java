package dev.ramon.kees.storage;

import software.amazon.awssdk.core.exception.SdkException;

public class DeleteObjectFailedException extends ObjectStorageException {
    public DeleteObjectFailedException(String key, Throwable cause) {
        super("Failed to delete object with key: %s".formatted(key), cause);
    }
}
