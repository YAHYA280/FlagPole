package dev.flagpole.api.sdk;

import dev.flagpole.api.evaluation.EvaluationContext;
import dev.flagpole.api.evaluation.EvaluationResult;
import dev.flagpole.api.evaluation.FlagSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public final class SdkDtos {

    private SdkDtos() {
    }

    /** Evaluate flags for one context. {@code flagKeys} null or empty means all flags in the environment. */
    public record EvaluateRequest(
            @NotNull @Valid
            EvaluationContext context,
            @Size(max = 500)
            List<String> flagKeys) {
    }

    public record EvaluateResponse(
            String project,
            String environment,
            Map<String, EvaluationResult> flags) {
    }

    /** Raw configuration for server-side SDKs that evaluate locally. Contains rules: never send to browsers. */
    public record SdkConfigResponse(
            String project,
            String environment,
            List<FlagSnapshot> flags) {
    }
}
