package dev.flagpole.api.environment;

import dev.flagpole.api.common.ConflictException;
import dev.flagpole.api.common.NotFoundException;
import dev.flagpole.api.environment.EnvironmentDtos.CreateEnvironmentRequest;
import dev.flagpole.api.environment.EnvironmentDtos.EnvironmentResponse;
import dev.flagpole.api.flag.FlagConfigService;
import dev.flagpole.api.project.Project;
import dev.flagpole.api.project.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final ProjectService projectService;
    private final FlagConfigService flagConfigService;
    private final SdkKeyGenerator sdkKeyGenerator;

    public EnvironmentResponse create(String projectKey, CreateEnvironmentRequest request) {
        Project project = projectService.requireByKey(projectKey);
        if (environmentRepository.existsByProjectAndKey(project, request.key())) {
            throw ConflictException.duplicate("Environment", request.key());
        }
        Environment environment = environmentRepository.saveAndFlush(
                new Environment(project, request.key(), request.name(), sdkKeyGenerator.generate()));
        // every existing flag needs a config row for the new environment
        flagConfigService.initForEnvironment(environment);
        return EnvironmentResponse.from(environment);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentResponse> list(String projectKey) {
        Project project = projectService.requireByKey(projectKey);
        return environmentRepository.findAllByProjectOrderByCreatedAtAsc(project).stream()
                .map(EnvironmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EnvironmentResponse get(String projectKey, String environmentKey) {
        return EnvironmentResponse.from(requireByKey(projectService.requireByKey(projectKey), environmentKey));
    }

    public EnvironmentResponse rotateSdkKey(String projectKey, String environmentKey) {
        Environment environment = requireByKey(projectService.requireByKey(projectKey), environmentKey);
        environment.setSdkKey(sdkKeyGenerator.generate());
        return EnvironmentResponse.from(environment);
    }

    public void delete(String projectKey, String environmentKey) {
        environmentRepository.delete(requireByKey(projectService.requireByKey(projectKey), environmentKey));
    }

    public Environment requireByKey(Project project, String environmentKey) {
        return environmentRepository.findByProjectAndKey(project, environmentKey)
                .orElseThrow(() -> NotFoundException.of("Environment", environmentKey));
    }
}
