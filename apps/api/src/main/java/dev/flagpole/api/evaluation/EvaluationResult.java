package dev.flagpole.api.evaluation;

public record EvaluationResult(
        String flagKey,
        String variation,
        Object value,
        EvaluationReason reason,
        String ruleId,
        long version) {

    public static EvaluationResult notFound(String flagKey) {
        return new EvaluationResult(flagKey, null, null, EvaluationReason.FLAG_NOT_FOUND, null, 0);
    }
}
