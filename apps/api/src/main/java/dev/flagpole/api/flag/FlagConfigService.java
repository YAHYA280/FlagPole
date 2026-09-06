package dev.flagpole.api.flag;

import dev.flagpole.api.common.NotFoundException;
import dev.flagpole.api.environment.Environment;
import dev.flagpole.api.environment.EnvironmentRepository;
import dev.flagpole.api.flag.FlagDtos.FlagConfigResponse;
import dev.flagpole.api.flag.FlagDtos.UpdateFlagConfigRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Owns {@link FlagEnvironmentConfig} rows. Kept separate from FlagService and EnvironmentService because
 * both need it (creating a flag or an environment must bootstrap the missing config rows) and a
 * direct dependency between those two would be circular.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FlagConfigService {

    private final FlagEnvironmentConfigRepository configRepository;
    private final FeatureFlagRepository flagRepository;
    private final EnvironmentRepository environmentRepository;

    /** New flag: create a disabled config in every environment of its project. */
    public void initForFlag(FeatureFlag flag) {
        List<FlagEnvironmentConfig> configs = environmentRepository
                .findAllByProjectOrderByCreatedAtAsc(flag.getProject()).stream()
                .map(environment -> defaultConfig(flag, environment))
                .toList();
        configRepository.saveAll(configs);
    }

    /** New environment: create a disabled config for every flag of its project. */
    public void initForEnvironment(Environment environment) {
        List<FlagEnvironmentConfig> configs = flagRepository
                .findAllByProjectOrderByCreatedAtAsc(environment.getProject()).stream()
                .map(flag -> defaultConfig(flag, environment))
                .toList();
        configRepository.saveAll(configs);
    }

    @Transactional(readOnly = true)
    public FlagConfigResponse get(FeatureFlag flag, Environment environment) {
        return FlagConfigResponse.from(require(flag, environment));
    }

    public FlagConfigResponse update(FeatureFlag flag, Environment environment, UpdateFlagConfigRequest request) {
        if (!flag.hasVariation(request.onVariation())) {
            throw new IllegalArgumentException("Unknown variation '" + request.onVariation() + "'");
        }
        if (!flag.hasVariation(request.offVariation())) {
            throw new IllegalArgumentException("Unknown variation '" + request.offVariation() + "'");
        }
        FlagEnvironmentConfig config = require(flag, environment);
        config.setEnabled(request.enabled());
        config.setOnVariation(request.onVariation());
        config.setOffVariation(request.offVariation());
        // flush now so @Version and @UpdateTimestamp are bumped before we build the response
        return FlagConfigResponse.from(configRepository.saveAndFlush(config));
    }

    private FlagEnvironmentConfig require(FeatureFlag flag, Environment environment) {
        return configRepository.findByFlagAndEnvironment(flag, environment)
                .orElseThrow(() -> NotFoundException.of("Flag config", flag.getKey() + "@" + environment.getKey()));
    }

    /** Defaults: disabled, serve first variation when on, last variation when off. */
    private static FlagEnvironmentConfig defaultConfig(FeatureFlag flag, Environment environment) {
        List<Variation> variations = flag.getVariations();
        String on = variations.getFirst().key();
        String off = variations.getLast().key();
        return new FlagEnvironmentConfig(flag, environment, on, off);
    }
}
