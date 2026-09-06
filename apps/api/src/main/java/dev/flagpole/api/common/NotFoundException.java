package dev.flagpole.api.common;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }

    public static NotFoundException of(String entity, String key) {
        return new NotFoundException(entity + " '" + key + "' not found");
    }
}
