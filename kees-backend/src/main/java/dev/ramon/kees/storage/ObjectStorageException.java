package dev.ramon.kees.storage;

public abstract class ObjectStorageException extends RuntimeException {
    protected ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
