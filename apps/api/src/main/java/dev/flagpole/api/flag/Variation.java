package dev.flagpole.api.flag;

import dev.flagpole.api.common.Keys;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * One possible value a flag can serve. Stored as JSON inside the flag row.
 * {@code value} is Boolean / String / Number / Map depending on {@link FlagType}.
 */
public record Variation(
        @NotBlank @Size(max = 64) @Pattern(regexp = Keys.PATTERN, message = Keys.PATTERN_MESSAGE)
        String key,
        @NotNull
        Object value) {
}
