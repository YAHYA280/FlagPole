package dev.flagpole.api.project;

import dev.flagpole.api.project.ProjectDtos.CreateProjectRequest;
import dev.flagpole.api.project.ProjectDtos.ProjectResponse;
import dev.flagpole.api.project.ProjectDtos.UpdateProjectRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(request);
    }

    @GetMapping
    public List<ProjectResponse> list() {
        return projectService.list();
    }

    @GetMapping("/{projectKey}")
    public ProjectResponse get(@PathVariable String projectKey) {
        return projectService.get(projectKey);
    }

    @PatchMapping("/{projectKey}")
    public ProjectResponse update(@PathVariable String projectKey, @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(projectKey, request);
    }

    @DeleteMapping("/{projectKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String projectKey) {
        projectService.delete(projectKey);
    }
}
