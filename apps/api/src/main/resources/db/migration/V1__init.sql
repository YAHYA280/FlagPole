-- Core domain: project -> environments, project -> flags, flag x environment -> config

CREATE TABLE project (
    id          UUID PRIMARY KEY,
    key         VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE environment (
    id          UUID PRIMARY KEY,
    project_id  UUID         NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    key         VARCHAR(64)  NOT NULL,
    name        VARCHAR(128) NOT NULL,
    sdk_key     VARCHAR(128) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (project_id, key)
);

CREATE TABLE feature_flag (
    id          UUID PRIMARY KEY,
    project_id  UUID         NOT NULL REFERENCES project (id) ON DELETE CASCADE,
    key         VARCHAR(128) NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description TEXT,
    type        VARCHAR(16)  NOT NULL,          -- BOOLEAN | STRING | NUMBER | JSON
    variations  JSONB        NOT NULL,          -- [{"key":"on","value":true},{"key":"off","value":false}]
    archived    BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (project_id, key)
);

CREATE TABLE flag_environment_config (
    flag_id            UUID        NOT NULL REFERENCES feature_flag (id) ON DELETE CASCADE,
    environment_id     UUID        NOT NULL REFERENCES environment (id) ON DELETE CASCADE,
    enabled            BOOLEAN     NOT NULL DEFAULT false,
    on_variation       VARCHAR(64) NOT NULL,   -- variation served when enabled and no rule matches
    off_variation      VARCHAR(64) NOT NULL,   -- variation served when disabled
    rules              JSONB       NOT NULL DEFAULT '[]'::jsonb,  -- targeting rules, evaluated in order (M2)
    version            BIGINT      NOT NULL DEFAULT 1,            -- bumps on every change, used by SDK cache
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (flag_id, environment_id)
);

CREATE INDEX idx_feature_flag_project ON feature_flag (project_id);
CREATE INDEX idx_environment_project ON environment (project_id);
CREATE INDEX idx_flag_env_config_env ON flag_environment_config (environment_id);
