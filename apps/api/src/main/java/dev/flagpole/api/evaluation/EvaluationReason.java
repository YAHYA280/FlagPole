package dev.flagpole.api.evaluation;

public enum EvaluationReason {
    /** Flag disabled or archived in this environment: served the off variation. */
    OFF,
    /** A targeting rule matched: see {@code ruleId}. */
    RULE_MATCH,
    /** Flag enabled, no rule matched: served the on variation. */
    FALLTHROUGH,
    /** No flag with that key in this environment. */
    FLAG_NOT_FOUND
}
