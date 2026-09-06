package dev.flagpole.api.environment;

import dev.flagpole.api.common.Keys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class EnvironmentDtos {

    private EnvironmentDtos() {
    }

    public record CreateEnvironmentRequest(
            @NotBlank @Size(min = 2, max = 64) @Pattern(regexp = Keys.PATTERN, message = Keys.PATTERN_MESSAGE)
            String key,
            @NotBlank @Size(max = 128)
            String name) {
    }

    public record EnvironmentResponse(UUID id, String key, String name, String sdkKey, Instant createdAt) {

        static EnvironmentResponse from(Environment environment) {
            return new EnvironmentResponse(
                    environment.getId(),
                    environment.getKey(),
                    environment.getName(),
                    environment.getSdkKey(),
                    environment.getCreatedAt());
        }
    }
}
