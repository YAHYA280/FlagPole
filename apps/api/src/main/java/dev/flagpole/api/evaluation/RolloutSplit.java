package dev.flagpole.api.evaluation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** One slice of a percentage rollout. Weights of all splits in a rollout must sum to 100. */
public record RolloutSplit(
        @NotBlank
        String variation,
        @Min(0) @Max(100)
        int weight) {
}
