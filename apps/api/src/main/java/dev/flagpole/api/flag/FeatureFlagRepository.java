package dev.flagpole.api.flag;

import dev.flagpole.api.project.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, UUID> {

    List<FeatureFlag> findAllByProjectOrderByCreatedAtAsc(Project project);

    List<FeatureFlag> findAllByProjectAndArchivedFalseOrderByCreatedAtAsc(Project project);

    Optional<FeatureFlag> findByProjectAndKey(Project project, String key);

    boolean existsByProjectAndKey(Project project, String key);
}
