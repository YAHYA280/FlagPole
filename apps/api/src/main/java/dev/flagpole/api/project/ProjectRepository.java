package dev.flagpole.api.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByKey(String key);

    boolean existsByKey(String key);

    List<Project> findAllByOrderByCreatedAtAsc();
}
