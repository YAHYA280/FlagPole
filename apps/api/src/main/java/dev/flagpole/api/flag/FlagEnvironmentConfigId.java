package dev.flagpole.api.flag;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FlagEnvironmentConfigId implements Serializable {

    @Column(name = "flag_id")
    private UUID flagId;

    @Column(name = "environment_id")
    private UUID environmentId;
}
