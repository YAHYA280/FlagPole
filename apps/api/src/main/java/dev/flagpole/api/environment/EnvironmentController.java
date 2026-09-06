package dev.flagpole.api.environment;

import dev.flagpole.api.environment.EnvironmentDtos.CreateEnvironmentRequest;
import dev.flagpole.api.environment.EnvironmentDtos.EnvironmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectKey}/environments")
@RequiredArgsConstructor
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public EnvironmentResponse create(@PathVariable String projectKey,
                                      @Valid @RequestBody CreateEnvironmentRequest request) {
        return environmentService.create(projectKey, request);
    }

    @GetMapping
    public List<EnvironmentResponse> list(@PathVariable String projectKey) {
        return environmentService.list(projectKey);
    }

    @GetMapping("/{environmentKey}")
    public EnvironmentResponse get(@PathVariable String projectKey, @PathVariable String environmentKey) {
        return environmentService.get(projectKey, environmentKey);
    }

    @PostMapping("/{environmentKey}/rotate-sdk-key")
    @PreAuthorize("hasRole('ADMIN')")
    public EnvironmentResponse rotateSdkKey(@PathVariable String projectKey, @PathVariable String environmentKey) {
        return environmentService.rotateSdkKey(projectKey, environmentKey);
    }

    @DeleteMapping("/{environmentKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable String projectKey, @PathVariable String environmentKey) {
        environmentService.delete(projectKey, environmentKey);
    }
}
