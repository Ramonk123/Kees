package dev.ramon.kees.shared.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String name;
    private final String identifier;

    public ResourceNotFoundException(String name, String identifier) {
        super("%s with identifier '%s' was not found"
                .formatted(name, identifier));

        this.name = name;
        this.identifier = identifier;
    }
}
