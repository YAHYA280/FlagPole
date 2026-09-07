package dev.flagpole.api.evaluation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/**
 * Who is asking. {@code key} is the stable identity used for percentage bucketing (user id, device id,
 * session id...). {@code attributes} are free-form and matched by rule conditions.
 */
public record EvaluationContext(
        @NotBlank @Size(max = 256)
        String key,
        Map<String, Object> attributes) {

    public static final String KEY_ATTRIBUTE = "key";

    public EvaluationContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public static EvaluationContext of(String key) {
        return new EvaluationContext(key, Map.of());
    }

    public Object attribute(String name) {
        return KEY_ATTRIBUTE.equals(name) ? key : attributes.get(name);
    }
}
