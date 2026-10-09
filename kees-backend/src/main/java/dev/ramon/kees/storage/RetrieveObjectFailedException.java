package dev.ramon.kees.storage;

public class RetrieveObjectFailedException extends ObjectStorageException {
    public RetrieveObjectFailedException(String key, Throwable cause) {
        super("Failed to retrieve file with key: %s".formatted(key), cause);
    }
}
