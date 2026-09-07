package dev.flagpole.api.evaluation;

import dev.flagpole.api.flag.FlagType;
import dev.flagpole.api.flag.Variation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** Pure unit tests: no Spring, no database. */
class FlagEvaluatorTest {

    private final FlagEvaluator evaluator = new FlagEvaluator();

    private static final List<Variation> ON_OFF = List.of(
            new Variation("on", true),
            new Variation("off", false));

    private static FlagSnapshot flag(boolean enabled, List<TargetingRule> rules) {
        return new FlagSnapshot("new-checkout", FlagType.BOOLEAN, ON_OFF, enabled, "on", "off", rules, 3, false);
    }

    private static TargetingRule rule(String id, Serve serve, Condition... conditions) {
        return new TargetingRule(id, null, List.of(conditions), serve);
    }

    private static Condition cond(String attribute, Operator op, Object... values) {
        return new Condition(attribute, op, List.of(values));
    }

    private static EvaluationContext user(String key, Object... kv) {
        Map<String, Object> attrs = new java.util.HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            attrs.put((String) kv[i], kv[i + 1]);
        }
        return new EvaluationContext(key, attrs);
    }

    @Test
    void disabledFlagServesOffVariation() {
        EvaluationResult result = evaluator.evaluate(flag(false, List.of()), user("u1"));

        assertThat(result.reason()).isEqualTo(EvaluationReason.OFF);
        assertThat(result.variation()).isEqualTo("off");
        assertThat(result.value()).isEqualTo(false);
        assertThat(result.version()).isEqualTo(3);
    }

    @Test
    void archivedFlagServesOffVariationEvenWhenEnabled() {
        FlagSnapshot archived = new FlagSnapshot("old", FlagType.BOOLEAN, ON_OFF, true, "on", "off", List.of(), 1, true);

        assertThat(evaluator.evaluate(archived, user("u1")).reason()).isEqualTo(EvaluationReason.OFF);
    }

    @Test
    void enabledFlagWithoutRulesFallsThroughToOnVariation() {
        EvaluationResult result = evaluator.evaluate(flag(true, List.of()), user("u1"));

        assertThat(result.reason()).isEqualTo(EvaluationReason.FALLTHROUGH);
        assertThat(result.value()).isEqualTo(true);
    }

    @Test
    void firstMatchingRuleWins() {
        List<TargetingRule> rules = List.of(
                rule("internal", Serve.variation("on"), cond("email", Operator.ENDS_WITH, "@flagpole.dev")),
                rule("everyone-else", Serve.variation("off")));
        FlagSnapshot flag = flag(true, rules);

        EvaluationResult internal = evaluator.evaluate(flag, user("u1", "email", "ada@flagpole.dev"));
        EvaluationResult external = evaluator.evaluate(flag, user("u2", "email", "bob@gmail.com"));

        assertThat(internal.reason()).isEqualTo(EvaluationReason.RULE_MATCH);
        assertThat(internal.ruleId()).isEqualTo("internal");
        assertThat(internal.value()).isEqualTo(true);
        assertThat(external.ruleId()).isEqualTo("everyone-else");
        assertThat(external.value()).isEqualTo(false);
    }

    @Test
    void allConditionsInARuleMustMatch() {
        TargetingRule rule = rule("premium-fr", Serve.variation("on"),
                cond("plan", Operator.EQUALS, "premium"),
                cond("country", Operator.IN, "FR", "BE"));
        FlagSnapshot flag = flag(true, List.of(rule));

        assertThat(evaluator.evaluate(flag, user("u1", "plan", "premium", "country", "FR")).ruleId()).isEqualTo("premium-fr");
        assertThat(evaluator.evaluate(flag, user("u2", "plan", "premium", "country", "DE")).reason()).isEqualTo(EvaluationReason.FALLTHROUGH);
        assertThat(evaluator.evaluate(flag, user("u3", "country", "FR")).reason()).isEqualTo(EvaluationReason.FALLTHROUGH);
    }

    @Test
    void keyAttributeTargetsTheContextKey() {
        TargetingRule rule = rule("beta-users", Serve.variation("on"), cond("key", Operator.IN, "u42", "u43"));

        assertThat(evaluator.evaluate(flag(true, List.of(rule)), user("u42")).ruleId()).isEqualTo("beta-users");
        assertThat(evaluator.evaluate(flag(true, List.of(rule)), user("u1")).ruleId()).isNull();
    }

    @Test
    void numericOperatorsCompareAsNumbersAndIgnoreNonNumbers() {
        assertThat(evaluator.matches(cond("age", Operator.GTE, 18), user("u", "age", 18))).isTrue();
        assertThat(evaluator.matches(cond("age", Operator.GT, 18), user("u", "age", 18))).isFalse();
        assertThat(evaluator.matches(cond("age", Operator.LT, "18.5"), user("u", "age", 18))).isTrue();
        assertThat(evaluator.matches(cond("age", Operator.GT, 18), user("u", "age", "eighteen"))).isFalse();
        assertThat(evaluator.matches(cond("version", Operator.EQUALS, 2), user("u", "version", 2.0))).isTrue();
    }

    @Test
    void stringOperators() {
        EvaluationContext ctx = user("u", "email", "Ada@Flagpole.dev", "groups", List.of("qa", "beta"));

        assertThat(evaluator.matches(cond("email", Operator.STARTS_WITH, "Ada"), ctx)).isTrue();
        assertThat(evaluator.matches(cond("email", Operator.STARTS_WITH, "ada"), ctx)).isFalse();
        assertThat(evaluator.matches(cond("email", Operator.CONTAINS, "@Flagpole"), ctx)).isTrue();
        assertThat(evaluator.matches(cond("groups", Operator.CONTAINS, "beta"), ctx)).isTrue();
        assertThat(evaluator.matches(cond("groups", Operator.CONTAINS, "admins"), ctx)).isFalse();
        assertThat(evaluator.matches(cond("email", Operator.REGEX, "^[a-z]+@flagpole\\.dev$"), ctx)).isFalse();
        assertThat(evaluator.matches(cond("email", Operator.REGEX, "(?i)^[a-z]+@flagpole\\.dev$"), ctx)).isTrue();
        assertThat(evaluator.matches(cond("email", Operator.NOT_EQUALS, "x"), ctx)).isTrue();
        assertThat(evaluator.matches(cond("missing", Operator.NOT_EQUALS, "x"), ctx)).isFalse();
    }

    @Test
    void rolloutIsDeterministicPerUserAndFlag() {
        Serve rollout = Serve.rollout(List.of(new RolloutSplit("on", 30), new RolloutSplit("off", 70)));
        FlagSnapshot flag = flag(true, List.of(rule("thirty", rollout)));

        String first = evaluator.evaluate(flag, user("user-123")).variation();
        for (int i = 0; i < 20; i++) {
            assertThat(evaluator.evaluate(flag, user("user-123")).variation()).isEqualTo(first);
        }
    }

    @Test
    void rolloutSplitsUsersRoughlyByWeight() {
        Serve rollout = Serve.rollout(List.of(new RolloutSplit("on", 30), new RolloutSplit("off", 70)));
        FlagSnapshot flag = flag(true, List.of(rule("thirty", rollout)));

        long onCount = IntStream.range(0, 20_000)
                .mapToObj(i -> evaluator.evaluate(flag, user("user-" + i)).variation())
                .filter("on"::equals)
                .count();

        // 30% of 20 000 = 6 000; allow +-2 percentage points
        assertThat(onCount).isBetween(5_600L, 6_400L);
    }

    @Test
    void wideningARolloutKeepsExistingUsersIn() {
        Serve ten = Serve.rollout(List.of(new RolloutSplit("on", 10), new RolloutSplit("off", 90)));
        Serve forty = Serve.rollout(List.of(new RolloutSplit("on", 40), new RolloutSplit("off", 60)));
        FlagSnapshot tenPercent = flag(true, List.of(rule("r", ten)));
        FlagSnapshot fortyPercent = flag(true, List.of(rule("r", forty)));

        for (int i = 0; i < 2_000; i++) {
            EvaluationContext ctx = user("user-" + i);
            if ("on".equals(evaluator.evaluate(tenPercent, ctx).variation())) {
                assertThat(evaluator.evaluate(fortyPercent, ctx).variation()).isEqualTo("on");
            }
        }
    }

    @Test
    void bucketsDifferAcrossFlagsForTheSameUser() {
        long sameBucket = IntStream.range(0, 1_000)
                .filter(i -> FlagEvaluator.bucket("flag-a", "user-" + i) == FlagEvaluator.bucket("flag-b", "user-" + i))
                .count();

        // independent hashing: ~1% collide by chance, certainly not most of them
        assertThat(sameBucket).isLessThan(50);
    }
}
