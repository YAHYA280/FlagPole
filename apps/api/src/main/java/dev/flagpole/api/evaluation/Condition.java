package dev.flagpole.api.evaluation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * One test against the evaluation context: {@code attribute <operator> values}.
 * {@code attribute} "key" refers to the context key; anything else is looked up in {@code context.attributes}.
 */
public record Condition(
        @NotBlank @Size(max = 64)
        String attribute,
        @NotNull
        Operator operator,
        @NotEmpty @Size(max = 500)
        List<Object> values) {
}
