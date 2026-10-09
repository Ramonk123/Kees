package dev.ramon.kees.storage;

public class UploadFailedException extends ObjectStorageException {
    public UploadFailedException(String key, Throwable cause) {
        super("Could not upload object with key: %s"
                .formatted(key), cause);
    }
}
