package dev.flagpole.api.environment;

import dev.flagpole.api.project.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {

    List<Environment> findAllByProjectOrderByCreatedAtAsc(Project project);

    Optional<Environment> findByProjectAndKey(Project project, String key);

    boolean existsByProjectAndKey(Project project, String key);

    /** Used by the SDK auth filter outside any transaction, so the project must come along. */
    @EntityGraph(attributePaths = "project")
    Optional<Environment> findBySdkKey(String sdkKey);
}
