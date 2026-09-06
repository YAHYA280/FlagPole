package dev.flagpole.api.common;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public static ConflictException duplicate(String entity, String key) {
        return new ConflictException(entity + " '" + key + "' already exists");
    }
}
