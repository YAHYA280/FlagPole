# ADR 002: Targeting rules and evaluation engine

Status: accepted. Date: 2026-09-07.

## Context

M1 stores per-environment flag state as `enabled` + `onVariation` + `offVariation`. Real rollouts need more: serve the new checkout to internal users, then to 10% of everyone, then to everyone in one country. Rules must be evaluated identically by the API (client-side SDKs ask the server) and by server-side SDKs that download the rules and evaluate locally. The engine therefore has to be a pure function with no I/O.

## Decision

### Rule model (stored as JSONB in `flag_environment_config.rules`)

```
rules: [
  {
    "id": "internal-users",
    "conditions": [ {"attribute": "email", "operator": "ENDS_WITH", "values": ["@flagpole.dev"]} ],
    "serve": { "variation": "on" }
  },
  {
    "id": "ten-percent",
    "conditions": [],
    "serve": { "rollout": [ {"variation": "on", "weight": 10}, {"variation": "off", "weight": 90} ] }
  }
]
```

- Rules are evaluated **in order; first match wins**.
- Inside a rule, **all conditions must match** (AND). Different rules give OR.
- A rule with **no conditions matches everyone**. Put it last to get "percentage of all remaining users".
- `serve` is either one fixed `variation` or a percentage `rollout` whose weights sum to 100.
- Operators: `EQUALS, NOT_EQUALS, IN, NOT_IN, CONTAINS, STARTS_WITH, ENDS_WITH, GT, GTE, LT, LTE, REGEX`.
  Numeric operators compare as decimals; everything else compares as strings, case-sensitive.
- Attribute `key` refers to the context key; other attributes come from `context.attributes`.

### Evaluation order

```
flag archived or disabled  -> offVariation   (reason OFF)
first rule whose conditions all match -> its serve (reason RULE_MATCH, ruleId)
otherwise -> onVariation                     (reason FALLTHROUGH)
unknown flag key -> null value               (reason FLAG_NOT_FOUND)
```

### Percentage rollout

Bucket = first 8 bytes of `SHA-256(flagKey + ":" + contextKey)` as a non-negative long, modulo 100.
Splits are walked cumulatively: with weights 10/90, buckets 0-9 get the first variation.

- Same user + same flag = same bucket forever, so a user does not flip between variations on refresh.
- Bucketing is per flag, not per rule, so widening a rollout from 10% to 20% keeps the first 10% in.
- Different flags hash differently, so being in the 10% of one flag says nothing about another.
- Integer percent granularity (1%) is enough for this product. LaunchDarkly uses 100 000 buckets; easy to change later, the constant lives in one place.

### SDK authentication

SDK endpoints (`/api/v1/sdk/**`) authenticate with the environment's SDK key, not a Keycloak JWT. A second Spring Security filter chain owns that path prefix. The key identifies the environment, so SDK requests never name a project or environment in the URL.

## Consequences

- `FlagEvaluator` is a stateless pure function over a `FlagSnapshot` (flag definition + environment config). Unit tests need no Spring context. The same snapshot shape is what M3 caches in Redis and what the Java SDK (M5) evaluates locally.
- Rule validation (variation exists, weights sum to 100, regex compiles) happens on write, so evaluation never has to handle malformed rules.
- One SDK key currently grants access to rules (`GET /api/v1/sdk/flags`). Client-side SDKs running in browsers must not receive rules; M4 introduces separate client-side keys limited to `/evaluate`.
