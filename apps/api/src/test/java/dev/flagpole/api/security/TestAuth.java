package dev.flagpole.api.security;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Fakes a Keycloak JWT for MockMvc requests. Signature is not checked in MockMvc (the post-processor
 * injects the authentication directly) but claims go through the real {@link KeycloakRealmRoleConverter},
 * so role mapping is exercised exactly as in production.
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor admin() {
        return as("admin", "editor", "viewer");
    }

    public static RequestPostProcessor editor() {
        return as("editor", "viewer");
    }

    public static RequestPostProcessor viewer() {
        return as("viewer");
    }

    public static RequestPostProcessor as(String... realmRoles) {
        return jwt()
                .jwt(jwt -> jwt
                        .claim("preferred_username", realmRoles[0])
                        .claim("realm_access", Map.of("roles", List.of(realmRoles))))
                .authorities(new KeycloakRealmRoleConverter());
    }
}
