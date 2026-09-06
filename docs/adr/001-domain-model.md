# ADR 001: Domain model

Status: accepted. Date: 2026-09-06.

## Context

A feature flag platform needs to answer one question fast: *which variation does flag X serve to user Y in environment Z?* The data model must make that cheap while keeping the dashboard's editing model simple.

## Decision

Four aggregates:

```
project 1 ── * environment          (dev / staging / production, each with its own SDK key)
project 1 ── * feature_flag         (identity, type, possible variations - shared by all environments)
feature_flag * ── * environment  =>  flag_environment_config   (enabled, on/off variation, targeting rules, version)
```

- **Flag definition and flag state are separate tables.** A flag's key, type and variations are the same everywhere. Whether it is on, and for whom, differs per environment. Splitting them means "enable in staging" never touches production rows.
- **Variations are JSONB inside the flag row**, not a child table. They are always read together with the flag, are small, and their value type depends on the flag type (boolean / string / number / json). A relational child table would need a column per type or a text column with casting.
- **Targeting rules are JSONB inside the config row** for the same reason, and because the evaluation engine (M2) reads the whole rule list in order anyway.
- **`flag_environment_config.version` uses JPA `@Version`.** Two purposes: optimistic locking so two dashboard users cannot silently overwrite each other, and a monotonic marker SDKs use to detect stale local caches without diffing payloads.
- **Flags are archived, never deleted.** Application code in the wild may still call `isEnabled("old-flag")`. Archived flags keep serving their off variation instead of blowing up as unknown.
- **Config rows are created eagerly.** Creating a flag inserts one disabled config per environment; creating an environment inserts one disabled config per flag. Evaluation never has to handle "config missing" as a special case.

## Consequences

- Evaluation for one environment is one query: `flag_environment_config` joined to `feature_flag` where `environment_id = ?`. Good fit for caching the whole environment snapshot in Redis (M3).
- Renaming a flag key is intentionally unsupported. Keys are identifiers baked into client code.
- JSONB columns are opaque to SQL constraints. Validation of variations and rules lives in the service layer.
