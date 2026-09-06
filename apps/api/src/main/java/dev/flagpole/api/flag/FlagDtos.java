package dev.flagpole.api.flag;

import dev.flagpole.api.common.Keys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FlagDtos {

    private FlagDtos() {
    }

    public record CreateFlagRequest(
            @NotBlank @Size(min = 2, max = 128) @Pattern(regexp = Keys.PATTERN, message = Keys.PATTERN_MESSAGE)
            String key,
            @NotBlank @Size(max = 128)
            String name,
            @Size(max = 2000)
            String description,
            @NotNull
            FlagType type,
            /** Optional for BOOLEAN flags (defaults to on/off). Required otherwise. */
            List<@Valid Variation> variations) {
    }

    public record UpdateFlagRequest(
            @NotBlank @Size(max = 128)
            String name,
            @Size(max = 2000)
            String description) {
    }

    /** Per-environment summary embedded in flag responses so the dashboard list needs one call. */
    public record FlagEnvironmentStatus(
            String environmentKey,
            boolean enabled,
            String onVariation,
            String offVariation,
            long version) {

        static FlagEnvironmentStatus from(FlagEnvironmentConfig config) {
            return new FlagEnvironmentStatus(
                    config.getEnvironment().getKey(),
                    config.isEnabled(),
                    config.getOnVariation(),
                    config.getOffVariation(),
                    config.getVersion());
        }
    }

    public record FlagResponse(
            UUID id,
            String key,
            String name,
            String description,
            FlagType type,
            List<Variation> variations,
            boolean archived,
            Instant createdAt,
            Instant updatedAt,
            List<FlagEnvironmentStatus> environments) {

        static FlagResponse from(FeatureFlag flag, List<FlagEnvironmentConfig> configs) {
            return new FlagResponse(
                    flag.getId(),
                    flag.getKey(),
                    flag.getName(),
                    flag.getDescription(),
                    flag.getType(),
                    flag.getVariations(),
                    flag.isArchived(),
                    flag.getCreatedAt(),
                    flag.getUpdatedAt(),
                    configs.stream().map(FlagEnvironmentStatus::from).toList());
        }
    }

    public record UpdateFlagConfigRequest(
            @NotNull
            Boolean enabled,
            @NotBlank
            String onVariation,
            @NotBlank
            String offVariation) {
    }

    public record FlagConfigResponse(
            String flagKey,
            String environmentKey,
            boolean enabled,
            String onVariation,
            String offVariation,
            List<Map<String, Object>> rules,
            long version,
            Instant updatedAt) {

        static FlagConfigResponse from(FlagEnvironmentConfig config) {
            return new FlagConfigResponse(
                    config.getFlag().getKey(),
                    config.getEnvironment().getKey(),
                    config.isEnabled(),
                    config.getOnVariation(),
                    config.getOffVariation(),
                    config.getRules(),
                    config.getVersion(),
                    config.getUpdatedAt());
        }
    }
}
