package dev.flagpole.api.security;

import java.util.UUID;

/** Identity of an authenticated SDK: the environment its key belongs to. */
public record SdkPrincipal(UUID environmentId, String environmentKey, UUID projectId, String projectKey) {
}
