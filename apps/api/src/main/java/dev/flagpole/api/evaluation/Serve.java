package dev.flagpole.api.evaluation;

import jakarta.validation.Valid;

import java.util.List;

/**
 * What a matching rule serves: exactly one of a fixed {@code variation} or a percentage {@code rollout}.
 * Exclusivity is enforced by {@link RuleValidator} (bean validation cannot express "one of").
 */
public record Serve(
        String variation,
        List<@Valid RolloutSplit> rollout) {

    public static Serve variation(String variation) {
        return new Serve(variation, null);
    }

    public static Serve rollout(List<RolloutSplit> splits) {
        return new Serve(null, splits);
    }

    /** Deliberately not named isRollout(): Jackson would treat that as a property and clobber {@code rollout}. */
    public boolean usesRollout() {
        return rollout != null && !rollout.isEmpty();
    }
}
