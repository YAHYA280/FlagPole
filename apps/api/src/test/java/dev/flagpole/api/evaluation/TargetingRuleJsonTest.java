package dev.flagpole.api.evaluation;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Rules are stored as JSONB and shipped to SDKs: the JSON shape must round-trip exactly. */
class TargetingRuleJsonTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, TargetingRule.class);

    @Test
    void emptyList() {
        List<TargetingRule> rules = mapper.readValue("[]", listType);
        assertThat(rules).isEmpty();
    }

    @Test
    void roundTrip() {
        String json = """
                [{"id":"internal","description":null,
                  "conditions":[{"attribute":"email","operator":"ENDS_WITH","values":["@flagpole.dev"]}],
                  "serve":{"variation":"on","rollout":null}},
                 {"id":"half","description":"50/50","conditions":[],
                  "serve":{"variation":null,"rollout":[{"variation":"on","weight":50},{"variation":"off","weight":50}]}}]
                """;
        List<TargetingRule> rules = mapper.readValue(json, listType);

        assertThat(rules).hasSize(2);
        assertThat(rules.get(0).serve().variation()).isEqualTo("on");
        assertThat(rules.get(1).serve().rollout()).hasSize(2);
        assertThat(rules.get(1).conditions()).isEmpty();

        List<TargetingRule> again = mapper.readValue(mapper.writeValueAsString(rules), listType);
        assertThat(again).isEqualTo(rules);
    }
}
