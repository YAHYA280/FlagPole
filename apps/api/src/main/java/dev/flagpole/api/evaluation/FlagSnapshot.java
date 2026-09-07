package dev.flagpole.api.evaluation;

import dev.flagpole.api.flag.FeatureFlag;
import dev.flagpole.api.flag.FlagEnvironmentConfig;
import dev.flagpole.api.flag.FlagType;
import dev.flagpole.api.flag.Variation;

import java.util.List;

/**
 * Everything needed to evaluate one flag in one environment, detached from JPA.
 * This is the unit that gets cached (M3), streamed to SDKs (M3) and evaluated locally by server SDKs (M5).
 */
public record FlagSnapshot(
        String key,
        FlagType type,
        List<Variation> variations,
        boolean enabled,
        String onVariation,
        String offVariation,
        List<TargetingRule> rules,
        long version,
        boolean archived) {

    public static FlagSnapshot from(FlagEnvironmentConfig config) {
        FeatureFlag flag = config.getFlag();
        return new FlagSnapshot(
                flag.getKey(),
                flag.getType(),
                flag.getVariations(),
                config.isEnabled(),
                config.getOnVariation(),
                config.getOffVariation(),
                config.getRules(),
                config.getVersion(),
                flag.isArchived());
    }

    public Object valueOf(String variationKey) {
        return variations.stream()
                .filter(v -> v.key().equals(variationKey))
                .map(Variation::value)
                .findFirst()
                .orElse(null);
    }
}
