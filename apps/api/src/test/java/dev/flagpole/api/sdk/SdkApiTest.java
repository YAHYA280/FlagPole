package dev.flagpole.api.sdk;

import com.jayway.jsonpath.JsonPath;
import dev.flagpole.api.TestcontainersConfiguration;
import dev.flagpole.api.security.TestAuth;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dashboard user (JWT) sets up a flag with rules, then an SDK (SDK key) evaluates it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SdkApiTest {

    @Autowired
    private MockMvcTester mvc;

    private String sdkKey;

    @BeforeAll
    void setUpProjectWithRules() throws Exception {
        admin("/api/v1/projects", """
                {"key":"sdk-test","name":"SDK test"}
                """);
        MvcTestResult env = admin("/api/v1/projects/sdk-test/environments", """
                {"key":"production","name":"Production"}
                """);
        sdkKey = JsonPath.read(env.getResponse().getContentAsString(), "$.sdkKey");

        admin("/api/v1/projects/sdk-test/flags", """
                {"key":"new-checkout","name":"New checkout","type":"BOOLEAN"}
                """);
        admin("/api/v1/projects/sdk-test/flags", """
                {"key":"banner-text","name":"Banner","type":"STRING",
                 "variations":[{"key":"control","value":"Welcome"},{"key":"promo","value":"Summer sale"}]}
                """);

        MvcTestResult config = mvc.put().uri("/api/v1/projects/sdk-test/environments/production/flags/new-checkout/config")
                .with(TestAuth.editor())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"enabled":true,"onVariation":"off","offVariation":"off",
                         "rules":[
                           {"id":"internal","conditions":[{"attribute":"email","operator":"ENDS_WITH","values":["@flagpole.dev"]}],
                            "serve":{"variation":"on"}},
                           {"id":"half","conditions":[],
                            "serve":{"rollout":[{"variation":"on","weight":50},{"variation":"off","weight":50}]}}
                         ]}
                        """)
                .exchange();
        assertThat(config.getResponse().getStatus())
                .as(config.getResponse().getContentAsString())
                .isEqualTo(HttpStatus.OK.value());
        assertThat(config).bodyJson().extractingPath("$.rules[1].id").isEqualTo("half");
    }

    @Test
    void sdkEvaluatesWithRules() {
        assertThat(mvc.post().uri("/api/v1/sdk/evaluate")
                .header("Authorization", "Bearer " + sdkKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"context":{"key":"ada","attributes":{"email":"ada@flagpole.dev"}}}
                        """))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.environment").isEqualTo("production");
                    assertThat(json).extractingPath("$.flags.new-checkout.value").isEqualTo(true);
                    assertThat(json).extractingPath("$.flags.new-checkout.reason").isEqualTo("RULE_MATCH");
                    assertThat(json).extractingPath("$.flags.new-checkout.ruleId").isEqualTo("internal");
                    // banner-text is disabled by default -> off variation (last variation)
                    assertThat(json).extractingPath("$.flags.banner-text.value").isEqualTo("Summer sale");
                    assertThat(json).extractingPath("$.flags.banner-text.reason").isEqualTo("OFF");
                });
    }

    @Test
    void singleFlagEvaluationAndUnknownFlag() {
        assertThat(mvc.post().uri("/api/v1/sdk/evaluate/new-checkout")
                .header(dev.flagpole.api.security.SdkKeyAuthenticationFilter.SDK_KEY_HEADER, sdkKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"key":"someone","attributes":{"email":"x@gmail.com"}}
                        """))
                .hasStatusOk()
                .bodyJson().extractingPath("$.ruleId").isEqualTo("half");

        assertThat(mvc.post().uri("/api/v1/sdk/evaluate/does-not-exist")
                .header("Authorization", "Bearer " + sdkKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"key":"someone"}
                        """))
                .hasStatusOk()
                .bodyJson().extractingPath("$.reason").isEqualTo("FLAG_NOT_FOUND");
    }

    @Test
    void serverSdkDownloadsRawConfigWithRules() {
        assertThat(mvc.get().uri("/api/v1/sdk/flags").header("Authorization", "Bearer " + sdkKey))
                .hasStatusOk()
                .bodyJson()
                .satisfies(json -> {
                    assertThat(json).extractingPath("$.flags").asArray().hasSize(2);
                    assertThat(json).extractingPath("$.flags[0].rules[0].conditions[0].operator").isEqualTo("ENDS_WITH");
                });
    }

    @Test
    void sdkEndpointsRejectMissingWrongAndUserTokens() {
        assertThat(mvc.get().uri("/api/v1/sdk/flags"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/v1/sdk/flags").header("Authorization", "Bearer fp_nope"))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        // a user identity (even admin) is authenticated but lacks ROLE_SDK -> forbidden
        assertThat(mvc.get().uri("/api/v1/sdk/flags").with(TestAuth.admin()))
                .hasStatus(HttpStatus.FORBIDDEN);
        // and an SDK key is useless on management endpoints
        assertThat(mvc.get().uri("/api/v1/projects").header("Authorization", "Bearer " + sdkKey))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void invalidRulesAreRejectedOnWrite() {
        assertThat(mvc.put().uri("/api/v1/projects/sdk-test/environments/production/flags/new-checkout/config")
                .with(TestAuth.editor())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"enabled":true,"onVariation":"on","offVariation":"off",
                         "rules":[{"conditions":[],"serve":{"rollout":[{"variation":"on","weight":60},{"variation":"off","weight":50}]}}]}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").asString().contains("sum to 100");

        assertThat(mvc.put().uri("/api/v1/projects/sdk-test/environments/production/flags/new-checkout/config")
                .with(TestAuth.editor())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"enabled":true,"onVariation":"on","offVariation":"off",
                         "rules":[{"conditions":[{"attribute":"email","operator":"REGEX","values":["("]}],"serve":{"variation":"on"}}]}
                        """))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").asString().contains("invalid regex");
    }

    private MvcTestResult admin(String uri, String body) {
        MvcTestResult result = mvc.post().uri(uri).with(TestAuth.admin())
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.CREATED.value());
        return result;
    }
}
