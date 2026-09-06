package dev.flagpole.api.flag;

import dev.flagpole.api.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end flow through the real HTTP layer against a real Postgres (Testcontainers):
 * project -> environment -> flag -> per-environment config -> archive.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@WithMockUser
class FlagLifecycleTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void projectEnvironmentFlagLifecycle() {
        // project
        assertThat(post("/api/v1/projects", """
                {"key":"shop","name":"Shop"}
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.key").isEqualTo("shop");
                    assertThat(json).extractingPath("$.createdAt").isNotNull();
                });

        assertThat(post("/api/v1/projects", """
                {"key":"shop","name":"Duplicate"}
                """))
                .hasStatus(HttpStatus.CONFLICT);

        // environment gets an SDK key
        assertThat(post("/api/v1/projects/shop/environments", """
                {"key":"production","name":"Production"}
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().extractingPath("$.sdkKey").asString().startsWith("fp_");

        // boolean flag without explicit variations defaults to on/off, disabled in every environment
        assertThat(post("/api/v1/projects/shop/flags", """
                {"key":"new-checkout","name":"New checkout","type":"BOOLEAN"}
                """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.variations[0].key").isEqualTo("on");
                    assertThat(json).extractingPath("$.variations[1].value").isEqualTo(false);
                    assertThat(json).extractingPath("$.environments[0].environmentKey").isEqualTo("production");
                    assertThat(json).extractingPath("$.environments[0].enabled").isEqualTo(false);
                    assertThat(json).extractingPath("$.environments[0].onVariation").isEqualTo("on");
                });

        // enabling the flag bumps the config version (optimistic lock / SDK cache marker)
        assertThat(put("/api/v1/projects/shop/environments/production/flags/new-checkout/config", """
                {"enabled":true,"onVariation":"on","offVariation":"off"}
                """))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.enabled").isEqualTo(true);
                    assertThat(json).extractingPath("$.version").isEqualTo(1);
                });

        assertThat(put("/api/v1/projects/shop/environments/production/flags/new-checkout/config", """
                {"enabled":true,"onVariation":"nope","offVariation":"off"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST);

        // a new environment gets a config row for every existing flag
        assertThat(post("/api/v1/projects/shop/environments", """
                {"key":"staging","name":"Staging"}
                """))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mvc.get().uri("/api/v1/projects/shop/flags/new-checkout"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.environments").asArray().hasSize(2);

        // archive hides the flag from the default listing but keeps it retrievable
        assertThat(mvc.delete().uri("/api/v1/projects/shop/flags/new-checkout"))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mvc.get().uri("/api/v1/projects/shop/flags"))
                .hasStatusOk()
                .bodyJson().extractingPath("$").asArray().isEmpty();

        assertThat(mvc.get().uri("/api/v1/projects/shop/flags").param("includeArchived", "true"))
                .hasStatusOk()
                .bodyJson().extractingPath("$[0].archived").isEqualTo(true);
    }

    @Test
    void rejectsInvalidKeysWithFieldErrors() {
        assertThat(post("/api/v1/projects", """
                {"key":"Bad Key!","name":"x"}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.errors.key").asString().contains("lowercase");
    }

    @Test
    void rejectsVariationsThatDoNotMatchFlagType() {
        assertThat(post("/api/v1/projects", """
                {"key":"types","name":"Types"}
                """))
                .hasStatus(HttpStatus.CREATED);

        assertThat(post("/api/v1/projects/types/flags", """
                {"key":"banner","name":"Banner","type":"STRING",
                 "variations":[{"key":"a","value":"hello"},{"key":"b","value":42}]}
                """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").asString().contains("does not match flag type STRING");
    }

    private MockMvcTester.MockMvcRequestBuilder post(String uri, String body) {
        return mvc.post().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private MockMvcTester.MockMvcRequestBuilder put(String uri, String body) {
        return mvc.put().uri(uri).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
