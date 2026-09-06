# Flagpole

Self-hosted feature flag platform. Toggle features, roll out gradually, target users by attributes, and push changes to your apps in real time without redeploying.

> Work in progress. Milestone 1 (core API) in development.

## Why

Deploying code and releasing a feature should be separate decisions. Flagpole lets you ship dark, turn features on from a dashboard, roll out to 5% then 100%, and kill a broken feature in one second.

## Architecture

```
apps/api            Spring Boot 4 (Java 21) REST API, evaluation engine, SSE push
apps/web            Next.js dashboard
sdks/typescript     npm SDK (browser + Node)
sdks/java           Maven SDK
infra/              production Docker Compose, Grafana dashboards, k6 load tests
docs/               design notes, ADRs
```

Stack: Java 21, Spring Boot 4, PostgreSQL, Redis, Kafka, Keycloak, Next.js, TypeScript, Docker, GitHub Actions, OpenTelemetry, Prometheus, Grafana.

## Local development

Prerequisites: JDK 21, Node 20+, pnpm, Docker.

```bash
# 1. infrastructure (Postgres on localhost:5434, Redis on localhost:6379)
docker compose up -d

# 2. API
cd apps/api
./mvnw spring-boot:run
# http://localhost:8080/actuator/health
```

Windows PowerShell, if another JDK is your default:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
.\mvnw.cmd spring-boot:run
```

## Roadmap

- [ ] M1 Projects, environments, flags CRUD + auth
- [ ] M2 Targeting rules + evaluation API
- [ ] M3 SSE streaming + Redis cache
- [ ] M4 TypeScript SDK
- [ ] M5 Java SDK
- [ ] M6 Kafka events, audit log, evaluation analytics
- [ ] M7 Observability + load testing
- [ ] M8 Production deploy, docs, demo

## License

MIT
