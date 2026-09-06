package dev.flagpole.api.security;

import dev.flagpole.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthorizationTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void anonymousIsRejected() {
        assertThat(mvc.get().uri("/api/v1/projects"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void healthIsPublic() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk();
    }

    @Test
    void viewerCanReadButNotWrite() {
        assertThat(mvc.get().uri("/api/v1/projects").with(TestAuth.viewer()))
                .hasStatusOk();

        assertThat(mvc.post().uri("/api/v1/projects")
                .with(TestAuth.viewer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"key":"viewer-project","name":"Nope"}
                        """))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void editorManagesFlagsButNotProjects() {
        assertThat(mvc.post().uri("/api/v1/projects")
                .with(TestAuth.editor())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"key":"editor-project","name":"Nope"}
                        """))
                .hasStatus(HttpStatus.FORBIDDEN);

        assertThat(mvc.post().uri("/api/v1/projects")
                .with(TestAuth.admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"key":"authz","name":"Authz"}
                        """))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mvc.post().uri("/api/v1/projects/authz/flags")
                .with(TestAuth.editor())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"key":"editor-flag","name":"Editor flag","type":"BOOLEAN"}
                        """))
                .hasStatus(HttpStatus.CREATED);
    }

    @Test
    void realmRolesBecomeSpringAuthorities() {
        assertThat(new KeycloakRealmRoleConverter().convert(
                org.springframework.security.oauth2.jwt.Jwt.withTokenValue("t")
                        .header("alg", "none")
                        .claim("realm_access", java.util.Map.of("roles", java.util.List.of("admin", "viewer")))
                        .build()))
                .extracting(a -> a.getAuthority())
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_VIEWER");
    }
}
