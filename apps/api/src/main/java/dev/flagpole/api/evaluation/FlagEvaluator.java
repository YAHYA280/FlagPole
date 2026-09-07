package dev.flagpole.api.evaluation;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Pure, stateless rule engine. No I/O, no Spring dependencies beyond the bean annotation.
 * See docs/adr/002-evaluation-engine.md for the semantics.
 */
@Component
public class FlagEvaluator {

    /** Number of buckets for percentage rollouts. 100 = whole-percent granularity. */
    static final int BUCKETS = 100;

    private final Map<String, Pattern> regexCache = new ConcurrentHashMap<>();

    public EvaluationResult evaluate(FlagSnapshot flag, EvaluationContext context) {
        if (flag.archived() || !flag.enabled()) {
            return result(flag, flag.offVariation(), EvaluationReason.OFF, null);
        }
        for (TargetingRule rule : flag.rules()) {
            if (matches(rule, context)) {
                String variation = resolve(rule.serve(), flag.key(), context.key());
                return result(flag, variation, EvaluationReason.RULE_MATCH, rule.id());
            }
        }
        return result(flag, flag.onVariation(), EvaluationReason.FALLTHROUGH, null);
    }

    boolean matches(TargetingRule rule, EvaluationContext context) {
        for (Condition condition : rule.conditions()) {
            if (!matches(condition, context)) {
                return false;
            }
        }
        return true;
    }

    boolean matches(Condition condition, EvaluationContext context) {
        Object actual = context.attribute(condition.attribute());
        if (actual == null) {
            return false;
        }
        List<Object> expected = condition.values();
        return switch (condition.operator()) {
            case EQUALS, IN -> expected.stream().anyMatch(v -> equal(actual, v));
            case NOT_EQUALS, NOT_IN -> expected.stream().noneMatch(v -> equal(actual, v));
            case CONTAINS -> contains(actual, expected);
            case STARTS_WITH -> expected.stream().anyMatch(v -> str(actual).startsWith(str(v)));
            case ENDS_WITH -> expected.stream().anyMatch(v -> str(actual).endsWith(str(v)));
            case GT -> compare(actual, expected.getFirst()) > 0;
            case GTE -> compare(actual, expected.getFirst()) >= 0;
            case LT -> compare(actual, expected.getFirst()) < 0;
            case LTE -> compare(actual, expected.getFirst()) <= 0;
            case REGEX -> expected.stream().anyMatch(v -> regex(str(v)).matcher(str(actual)).find());
        };
    }

    /** Fixed variation, or the split this context's bucket falls into. */
    String resolve(Serve serve, String flagKey, String contextKey) {
        if (!serve.usesRollout()) {
            return serve.variation();
        }
        int bucket = bucket(flagKey, contextKey);
        int cumulative = 0;
        for (RolloutSplit split : serve.rollout()) {
            cumulative += split.weight();
            if (bucket < cumulative) {
                return split.variation();
            }
        }
        // weights validated to sum to 100, so only reachable with a bucket >= 100, which cannot happen
        return serve.rollout().getLast().variation();
    }

    /**
     * Deterministic bucket in [0, BUCKETS). Depends on flag + context key only, so the same user always
     * lands in the same bucket for a given flag, and different flags bucket independently.
     */
    static int bucket(String flagKey, String contextKey) {
        byte[] digest = sha256(flagKey + ":" + contextKey);
        long value = ByteBuffer.wrap(digest, 0, Long.BYTES).getLong() & Long.MAX_VALUE;
        return (int) (value % BUCKETS);
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private EvaluationResult result(FlagSnapshot flag, String variation, EvaluationReason reason, String ruleId) {
        return new EvaluationResult(flag.key(), variation, flag.valueOf(variation), reason, ruleId, flag.version());
    }

    // ---- value semantics -------------------------------------------------------------------------

    /** Numbers compare numerically (1 == 1.0), everything else as case-sensitive strings. */
    private static boolean equal(Object actual, Object expected) {
        BigDecimal a = number(actual);
        BigDecimal b = number(expected);
        if (a != null && b != null) {
            return a.compareTo(b) == 0;
        }
        return str(actual).equals(str(expected));
    }

    /** Collections: any element equals any expected value. Strings: substring match. */
    private static boolean contains(Object actual, List<Object> expected) {
        if (actual instanceof Collection<?> items) {
            return items.stream().anyMatch(item -> expected.stream().anyMatch(v -> equal(item, v)));
        }
        String s = str(actual);
        return expected.stream().anyMatch(v -> s.contains(str(v)));
    }

    /** Numeric comparison; non-numeric operands never match (returns a value that fails every operator). */
    private static int compare(Object actual, Object expected) {
        BigDecimal a = number(actual);
        BigDecimal b = number(expected);
        if (a == null || b == null) {
            return Integer.MIN_VALUE;
        }
        return a.compareTo(b);
    }

    private static BigDecimal number(Object value) {
        if (value instanceof Number n) {
            return new BigDecimal(n.toString());
        }
        if (value instanceof String s) {
            try {
                return new BigDecimal(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String str(Object value) {
        return String.valueOf(value);
    }

    private Pattern regex(String pattern) {
        return regexCache.computeIfAbsent(pattern, p -> {
            try {
                return Pattern.compile(p);
            } catch (PatternSyntaxException e) {
                // validated on write; a bad pattern here means data was tampered with, match nothing
                return Pattern.compile("(?!)");
            }
        });
    }
}
