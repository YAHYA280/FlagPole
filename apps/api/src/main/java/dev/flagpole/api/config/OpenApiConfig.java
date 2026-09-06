package dev.flagpole.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui.html, spec at /v3/api-docs.
 * "Authorize" button takes a raw Keycloak access token (see README for how to get one).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Flagpole API",
                version = "v1",
                description = "Self-hosted feature flag platform: projects, environments, flags and per-environment configuration.",
                license = @License(name = "MIT")),
        security = @SecurityRequirement(name = "bearer-jwt"))
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Keycloak access token")
public class OpenApiConfig {
}
