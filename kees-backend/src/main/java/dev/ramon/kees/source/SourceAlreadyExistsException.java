package dev.ramon.kees.source;

public class SourceAlreadyExistsException extends RuntimeException {
    public SourceAlreadyExistsException() {
        super("A source with identical content already exists in this collection");
    }
}
