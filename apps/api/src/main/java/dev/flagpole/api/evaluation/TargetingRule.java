package dev.flagpole.api.evaluation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Rules are evaluated in order, first match wins. All conditions of a rule must match (AND).
 * A rule with no conditions matches everyone.
 */
public record TargetingRule(
        /** Stable identifier, generated when absent. Shows up in evaluation results and audit logs. */
        @Size(max = 64)
        String id,
        @Size(max = 200)
        String description,
        @NotNull
        List<@Valid Condition> conditions,
        @NotNull @Valid
        Serve serve) {
}
