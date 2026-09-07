package dev.flagpole.api.sdk;

import dev.flagpole.api.evaluation.EvaluationContext;
import dev.flagpole.api.evaluation.EvaluationResult;
import dev.flagpole.api.sdk.SdkDtos.EvaluateRequest;
import dev.flagpole.api.sdk.SdkDtos.EvaluateResponse;
import dev.flagpole.api.sdk.SdkDtos.SdkConfigResponse;
import dev.flagpole.api.security.SdkPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints called by SDKs. Authenticated with an environment SDK key
 * ({@code Authorization: Bearer fp_...}), never with a user JWT. The key fixes project + environment,
 * so nothing in these URLs names either.
 */
@RestController
@RequestMapping("/api/v1/sdk")
@RequiredArgsConstructor
@Tag(name = "SDK", description = "Flag evaluation for SDKs (SDK key auth)")
public class SdkController {

    private final SdkService sdkService;

    @GetMapping("/flags")
    @Operation(summary = "All flag configurations of the environment, for local evaluation by server-side SDKs")
    public SdkConfigResponse config(@AuthenticationPrincipal SdkPrincipal sdk) {
        return sdkService.config(sdk);
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate all (or selected) flags for a context")
    public EvaluateResponse evaluate(@AuthenticationPrincipal SdkPrincipal sdk,
                                     @Valid @RequestBody EvaluateRequest request) {
        return sdkService.evaluate(sdk, request.context(), request.flagKeys());
    }

    @PostMapping("/evaluate/{flagKey}")
    @Operation(summary = "Evaluate one flag for a context")
    public EvaluationResult evaluateOne(@AuthenticationPrincipal SdkPrincipal sdk,
                                        @PathVariable String flagKey,
                                        @Valid @RequestBody EvaluationContext context) {
        return sdkService.evaluate(sdk, flagKey, context);
    }
}
