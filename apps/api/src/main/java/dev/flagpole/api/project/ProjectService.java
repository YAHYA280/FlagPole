package dev.flagpole.api.project;

import dev.flagpole.api.common.ConflictException;
import dev.flagpole.api.common.NotFoundException;
import dev.flagpole.api.project.ProjectDtos.CreateProjectRequest;
import dev.flagpole.api.project.ProjectDtos.ProjectResponse;
import dev.flagpole.api.project.ProjectDtos.UpdateProjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectResponse create(CreateProjectRequest request) {
        if (projectRepository.existsByKey(request.key())) {
            throw ConflictException.duplicate("Project", request.key());
        }
        // flush so DB-generated timestamps are populated before mapping the response
        Project project = projectRepository.saveAndFlush(new Project(request.key(), request.name()));
        return ProjectResponse.from(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list() {
        return projectRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(ProjectResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(String key) {
        return ProjectResponse.from(requireByKey(key));
    }

    public ProjectResponse update(String key, UpdateProjectRequest request) {
        Project project = requireByKey(key);
        project.setName(request.name());
        return ProjectResponse.from(projectRepository.saveAndFlush(project));
    }

    public void delete(String key) {
        projectRepository.delete(requireByKey(key));
    }

    /** Shared lookup for other services (environments, flags). */
    public Project requireByKey(String key) {
        return projectRepository.findByKey(key)
                .orElseThrow(() -> NotFoundException.of("Project", key));
    }
}
