package dev.flagpole.api.environment;

import dev.flagpole.api.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {

    List<Environment> findAllByProjectOrderByCreatedAtAsc(Project project);

    Optional<Environment> findByProjectAndKey(Project project, String key);

    boolean existsByProjectAndKey(Project project, String key);

    Optional<Environment> findBySdkKey(String sdkKey);
}
