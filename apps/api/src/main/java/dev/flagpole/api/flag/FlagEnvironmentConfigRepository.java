package dev.flagpole.api.flag;

import dev.flagpole.api.environment.Environment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FlagEnvironmentConfigRepository extends JpaRepository<FlagEnvironmentConfig, FlagEnvironmentConfigId> {

    /** Loads environment eagerly: callers always need env key next to the config. Avoids N+1. */
    @EntityGraph(attributePaths = "environment")
    List<FlagEnvironmentConfig> findAllByFlagIn(Collection<FeatureFlag> flags);

    Optional<FlagEnvironmentConfig> findByFlagAndEnvironment(FeatureFlag flag, Environment environment);

    @EntityGraph(attributePaths = "flag")
    List<FlagEnvironmentConfig> findAllByEnvironment(Environment environment);
}
