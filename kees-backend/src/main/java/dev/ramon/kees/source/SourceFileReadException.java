package dev.ramon.kees.source;

import java.io.IOException;

public class SourceFileReadException extends RuntimeException {
    public SourceFileReadException(String message, IOException exception) {
        super(message, exception);
    }
}
