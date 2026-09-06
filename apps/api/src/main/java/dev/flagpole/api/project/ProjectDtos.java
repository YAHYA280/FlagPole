package dev.flagpole.api.project;

import dev.flagpole.api.common.Keys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
            @NotBlank @Size(min = 2, max = 64) @Pattern(regexp = Keys.PATTERN, message = Keys.PATTERN_MESSAGE)
            String key,
            @NotBlank @Size(max = 128)
            String name) {
    }

    public record UpdateProjectRequest(
            @NotBlank @Size(max = 128)
            String name) {
    }

    public record ProjectResponse(UUID id, String key, String name, Instant createdAt, Instant updatedAt) {

        static ProjectResponse from(Project project) {
            return new ProjectResponse(
                    project.getId(),
                    project.getKey(),
                    project.getName(),
                    project.getCreatedAt(),
                    project.getUpdatedAt());
        }
    }
}
