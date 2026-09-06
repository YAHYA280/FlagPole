package dev.flagpole.api.flag;

import dev.flagpole.api.common.ConflictException;
import dev.flagpole.api.common.NotFoundException;
import dev.flagpole.api.flag.FlagDtos.CreateFlagRequest;
import dev.flagpole.api.flag.FlagDtos.FlagResponse;
import dev.flagpole.api.flag.FlagDtos.UpdateFlagRequest;
import dev.flagpole.api.project.Project;
import dev.flagpole.api.project.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FlagService {

    private static final List<Variation> BOOLEAN_DEFAULTS = List.of(
            new Variation("on", Boolean.TRUE),
            new Variation("off", Boolean.FALSE));

    private final FeatureFlagRepository flagRepository;
    private final FlagEnvironmentConfigRepository configRepository;
    private final FlagConfigService flagConfigService;
    private final ProjectService projectService;

    public FlagResponse create(String projectKey, CreateFlagRequest request) {
        Project project = projectService.requireByKey(projectKey);
        if (flagRepository.existsByProjectAndKey(project, request.key())) {
            throw ConflictException.duplicate("Flag", request.key());
        }
        List<Variation> variations = resolveVariations(request.type(), request.variations());
        FeatureFlag flag = flagRepository.saveAndFlush(new FeatureFlag(
                project, request.key(), request.name(), request.description(), request.type(), variations));
        flagConfigService.initForFlag(flag);
        return toResponse(flag);
    }

    @Transactional(readOnly = true)
    public List<FlagResponse> list(String projectKey, boolean includeArchived) {
        Project project = projectService.requireByKey(projectKey);
        List<FeatureFlag> flags = includeArchived
                ? flagRepository.findAllByProjectOrderByCreatedAtAsc(project)
                : flagRepository.findAllByProjectAndArchivedFalseOrderByCreatedAtAsc(project);
        if (flags.isEmpty()) {
            return List.of();
        }
        // one query for all configs, grouped in memory, instead of one query per flag
        Map<UUID, List<FlagEnvironmentConfig>> configsByFlag = configRepository.findAllByFlagIn(flags).stream()
                .collect(Collectors.groupingBy(config -> config.getFlag().getId()));
        return flags.stream()
                .map(flag -> FlagResponse.from(flag, configsByFlag.getOrDefault(flag.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public FlagResponse get(String projectKey, String flagKey) {
        return toResponse(requireByKey(projectService.requireByKey(projectKey), flagKey));
    }

    public FlagResponse update(String projectKey, String flagKey, UpdateFlagRequest request) {
        FeatureFlag flag = requireByKey(projectService.requireByKey(projectKey), flagKey);
        flag.setName(request.name());
        flag.setDescription(request.description());
        return toResponse(flagRepository.saveAndFlush(flag));
    }

    /** Flags are archived, never hard-deleted: SDK code in the wild may still reference the key. */
    public void archive(String projectKey, String flagKey) {
        FeatureFlag flag = requireByKey(projectService.requireByKey(projectKey), flagKey);
        flag.setArchived(true);
    }

    public FeatureFlag requireByKey(Project project, String flagKey) {
        return flagRepository.findByProjectAndKey(project, flagKey)
                .orElseThrow(() -> NotFoundException.of("Flag", flagKey));
    }

    private FlagResponse toResponse(FeatureFlag flag) {
        return FlagResponse.from(flag, configRepository.findAllByFlagIn(List.of(flag)));
    }

    private static List<Variation> resolveVariations(FlagType type, List<Variation> requested) {
        if (requested == null || requested.isEmpty()) {
            if (type == FlagType.BOOLEAN) {
                return BOOLEAN_DEFAULTS;
            }
            throw new IllegalArgumentException("variations are required for " + type + " flags");
        }
        if (requested.size() < 2) {
            throw new IllegalArgumentException("a flag needs at least 2 variations");
        }
        Set<String> keys = new HashSet<>();
        for (Variation variation : requested) {
            if (!keys.add(variation.key())) {
                throw new IllegalArgumentException("duplicate variation key '" + variation.key() + "'");
            }
            if (!matchesType(type, variation.value())) {
                throw new IllegalArgumentException(
                        "variation '" + variation.key() + "' value does not match flag type " + type);
            }
        }
        return List.copyOf(requested);
    }

    private static boolean matchesType(FlagType type, Object value) {
        return switch (type) {
            case BOOLEAN -> value instanceof Boolean;
            case STRING -> value instanceof String;
            case NUMBER -> value instanceof Number;
            case JSON -> value instanceof Map || value instanceof List;
        };
    }
}
