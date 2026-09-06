package dev.flagpole.api.common;

/**
 * Shared constraints for human-readable keys (project, environment, flag, variation).
 * Kebab-case only: "my-project", "checkout-v2". Keys are immutable identifiers used in URLs and SDK calls.
 */
public final class Keys {

    public static final String PATTERN = "^[a-z0-9]+(?:[-_][a-z0-9]+)*$";
    public static final String PATTERN_MESSAGE = "must be lowercase letters, digits, '-' or '_' (e.g. checkout-v2)";

    private Keys() {
    }
}
