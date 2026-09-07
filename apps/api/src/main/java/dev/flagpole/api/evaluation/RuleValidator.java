package dev.flagpole.api.evaluation;

import dev.flagpole.api.flag.FeatureFlag;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Semantic checks bean validation cannot express. Runs on write so the evaluator can trust stored rules.
 * Returns a normalised copy (ids filled in) or throws IllegalArgumentException (-> 400).
 */
@Component
public class RuleValidator {

    public List<TargetingRule> validate(FeatureFlag flag, List<TargetingRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return List.of();
        }
        Set<String> ids = new HashSet<>();
        List<TargetingRule> normalised = new ArrayList<>(rules.size());
        for (int i = 0; i < rules.size(); i++) {
            TargetingRule rule = rules.get(i);
            String id = rule.id() == null || rule.id().isBlank() ? UUID.randomUUID().toString() : rule.id();
            if (!ids.add(id)) {
                throw new IllegalArgumentException("rules[" + i + "]: duplicate rule id '" + id + "'");
            }
            for (int c = 0; c < rule.conditions().size(); c++) {
                validateCondition("rules[" + i + "].conditions[" + c + "]", rule.conditions().get(c));
            }
            validateServe("rules[" + i + "].serve", flag, rule.serve());
            normalised.add(new TargetingRule(id, rule.description(), List.copyOf(rule.conditions()), rule.serve()));
        }
        return List.copyOf(normalised);
    }

    private static void validateCondition(String path, Condition condition) {
        switch (condition.operator()) {
            case GT, GTE, LT, LTE -> {
                if (condition.values().size() != 1 || !isNumber(condition.values().getFirst())) {
                    throw new IllegalArgumentException(path + ": " + condition.operator() + " needs exactly one numeric value");
                }
            }
            case REGEX -> {
                for (Object value : condition.values()) {
                    try {
                        Pattern.compile(String.valueOf(value));
                    } catch (PatternSyntaxException e) {
                        throw new IllegalArgumentException(path + ": invalid regex '" + value + "': " + e.getDescription());
                    }
                }
            }
            default -> {
                // any non-empty value list is fine
            }
        }
    }

    private static void validateServe(String path, FeatureFlag flag, Serve serve) {
        boolean hasVariation = serve.variation() != null && !serve.variation().isBlank();
        boolean hasRollout = serve.usesRollout();
        if (hasVariation == hasRollout) {
            throw new IllegalArgumentException(path + ": specify exactly one of 'variation' or 'rollout'");
        }
        if (hasVariation) {
            requireVariation(path + ".variation", flag, serve.variation());
            return;
        }
        int total = 0;
        for (RolloutSplit split : serve.rollout()) {
            requireVariation(path + ".rollout", flag, split.variation());
            total += split.weight();
        }
        if (total != 100) {
            throw new IllegalArgumentException(path + ".rollout: weights must sum to 100, got " + total);
        }
    }

    private static void requireVariation(String path, FeatureFlag flag, String variationKey) {
        if (!flag.hasVariation(variationKey)) {
            throw new IllegalArgumentException(path + ": unknown variation '" + variationKey + "'");
        }
    }

    private static boolean isNumber(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String s) {
            try {
                new BigDecimal(s.trim());
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
