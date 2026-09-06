package dev.flagpole.api.environment;

import dev.flagpole.api.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A deployment target inside a project (dev, staging, production).
 * Each environment owns an SDK key; SDKs authenticate with it and receive that environment's flag configs.
 */
@Entity
@Table(name = "environment")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 64)
    private String key;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "sdk_key", nullable = false, unique = true, length = 128)
    private String sdkKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Environment(Project project, String key, String name, String sdkKey) {
        this.project = project;
        this.key = key;
        this.name = name;
        this.sdkKey = sdkKey;
    }
}
