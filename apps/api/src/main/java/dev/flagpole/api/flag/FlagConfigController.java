package dev.flagpole.api.flag;

import dev.flagpole.api.environment.Environment;
import dev.flagpole.api.environment.EnvironmentService;
import dev.flagpole.api.flag.FlagDtos.FlagConfigResponse;
import dev.flagpole.api.flag.FlagDtos.UpdateFlagConfigRequest;
import dev.flagpole.api.project.Project;
import dev.flagpole.api.project.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/v1/projects/{projectKey}/environments/{environmentKey}/flags/{flagKey}/config")
@RequiredArgsConstructor
public class FlagConfigController {

    private final ProjectService projectService;
    private final EnvironmentService environmentService;
    private final FlagService flagService;
    private final FlagConfigService flagConfigService;

    @GetMapping
    @Transactional(readOnly = true)
    public FlagConfigResponse get(@PathVariable String projectKey,
                                  @PathVariable String environmentKey,
                                  @PathVariable String flagKey) {
        Project project = projectService.requireByKey(projectKey);
        Environment environment = environmentService.requireByKey(project, environmentKey);
        FeatureFlag flag = flagService.requireByKey(project, flagKey);
        return flagConfigService.get(flag, environment);
    }

    @PutMapping
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public FlagConfigResponse update(@PathVariable String projectKey,
                                     @PathVariable String environmentKey,
                                     @PathVariable String flagKey,
                                     @Valid @RequestBody UpdateFlagConfigRequest request) {
        Project project = projectService.requireByKey(projectKey);
        Environment environment = environmentService.requireByKey(project, environmentKey);
        FeatureFlag flag = flagService.requireByKey(project, flagKey);
        return flagConfigService.update(flag, environment, request);
    }
}
