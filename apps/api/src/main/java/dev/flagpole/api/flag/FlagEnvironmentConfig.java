package dev.flagpole.api.flag;

import dev.flagpole.api.environment.Environment;
import dev.flagpole.api.evaluation.TargetingRule;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * State of one flag inside one environment. This is what SDKs actually consume.
 * {@code version} is bumped by Hibernate on every update (optimistic locking) and doubles as the
 * cache/ETag version SDKs use to know whether their local copy is stale.
 */
@Entity
@Table(name = "flag_environment_config")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FlagEnvironmentConfig {

    @EmbeddedId
    private FlagEnvironmentConfigId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("flagId")
    @JoinColumn(name = "flag_id", nullable = false)
    private FeatureFlag flag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("environmentId")
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "on_variation", nullable = false, length = 64)
    private String onVariation;

    @Column(name = "off_variation", nullable = false, length = 64)
    private String offVariation;

    /** Targeting rules, evaluated in order, first match wins. See docs/adr/002-evaluation-engine.md. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<TargetingRule> rules = new ArrayList<>();

    @Version
    @Column(nullable = false)
    private long version;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FlagEnvironmentConfig(FeatureFlag flag, Environment environment, String onVariation, String offVariation) {
        this.id = new FlagEnvironmentConfigId(flag.getId(), environment.getId());
        this.flag = flag;
        this.environment = environment;
        this.enabled = false;
        this.onVariation = onVariation;
        this.offVariation = offVariation;
    }
}
