package dev.flagpole.api.sdk;

import dev.flagpole.api.environment.Environment;
import dev.flagpole.api.environment.EnvironmentRepository;
import dev.flagpole.api.evaluation.EvaluationContext;
import dev.flagpole.api.evaluation.EvaluationResult;
import dev.flagpole.api.evaluation.FlagEvaluator;
import dev.flagpole.api.evaluation.FlagSnapshot;
import dev.flagpole.api.flag.FlagEnvironmentConfigRepository;
import dev.flagpole.api.sdk.SdkDtos.EvaluateResponse;
import dev.flagpole.api.sdk.SdkDtos.SdkConfigResponse;
import dev.flagpole.api.security.SdkPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SdkService {

    private final EnvironmentRepository environmentRepository;
    private final FlagEnvironmentConfigRepository configRepository;
    private final FlagEvaluator evaluator;

    public SdkConfigResponse config(SdkPrincipal sdk) {
        return new SdkConfigResponse(sdk.projectKey(), sdk.environmentKey(), loadSnapshots(sdk));
    }

    public EvaluateResponse evaluate(SdkPrincipal sdk, EvaluationContext context, List<String> flagKeys) {
        Map<String, FlagSnapshot> snapshots = loadSnapshots(sdk).stream()
                .collect(Collectors.toMap(FlagSnapshot::key, Function.identity()));

        List<String> keys = flagKeys == null || flagKeys.isEmpty() ? List.copyOf(snapshots.keySet()) : flagKeys;
        Map<String, EvaluationResult> results = new LinkedHashMap<>();
        for (String key : keys) {
            FlagSnapshot snapshot = snapshots.get(key);
            results.put(key, snapshot == null
                    ? EvaluationResult.notFound(key)
                    : evaluator.evaluate(snapshot, context));
        }
        return new EvaluateResponse(sdk.projectKey(), sdk.environmentKey(), results);
    }

    public EvaluationResult evaluate(SdkPrincipal sdk, String flagKey, EvaluationContext context) {
        return evaluate(sdk, context, List.of(flagKey)).flags().get(flagKey);
    }

    /** All non-archived flags of the environment as detached snapshots. M3 caches this per environment. */
    private List<FlagSnapshot> loadSnapshots(SdkPrincipal sdk) {
        Environment environment = environmentRepository.getReferenceById(sdk.environmentId());
        return configRepository.findAllByEnvironmentOrderByFlagKeyAsc(environment).stream()
                .filter(config -> !config.getFlag().isArchived())
                .map(FlagSnapshot::from)
                .toList();
    }
}
