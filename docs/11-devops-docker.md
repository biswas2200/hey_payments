# 11 · DevOps and Docker

Everything runs in Docker. A new developer must be able to do `cp .env.example .env && make up` and open the app.

## 1. Repository layout (target)
```
hey_payments/
  AGENTS.md  GEMINI.md  Makefile  docker-compose.yml  docker-compose.test.yml  .env.example
  backend/   (Maven, Spring Boot)     Dockerfile
  frontend/  (Angular PWA)            Dockerfile  nginx.conf
  e2e/       (Playwright project)     Dockerfile
  test-vectors/
  docs/
  .github/workflows/ci.yml
```

## 2. docker-compose.yml (reference; the agent implements it)
```yaml
name: heypayments
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: heypayments
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck: { test: ["CMD-SHELL", "pg_isready -U ${DB_USER}"], interval: 5s, retries: 10 }
    ports: ["5432:5432"]
  redis:
    image: redis:7-alpine
    command: ["redis-server", "--appendonly", "yes"]
    healthcheck: { test: ["CMD", "redis-cli", "ping"], interval: 5s, retries: 10 }
  mailpit:
    image: axllent/mailpit:latest
    ports: ["8025:8025"]
  backend:
    build: ./backend
    env_file: .env
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILE:-dev}
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/heypayments
      SPRING_DATA_REDIS_HOST: redis
    depends_on:
      postgres: { condition: service_healthy }
      redis: { condition: service_healthy }
    healthcheck: { test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health/readiness"], interval: 10s, retries: 12 }
    ports: ["8080:8080"]
  frontend:
    build: ./frontend
    depends_on:
      backend: { condition: service_healthy }
    ports: ["8443:443", "8081:80"]
volumes:
  pgdata:
```
Dev mode alternative: `make dev` runs postgres/redis/mailpit in Docker, with `./mvnw spring-boot:run` and `ng serve --proxy-config proxy.conf.json` on the host (hot reload). **Tests always run in Docker or Testcontainers.**

## 3. Dockerfiles
**backend/Dockerfile** (multi-stage, layered jar, non-root):
```dockerfile
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package && java -Djarmode=tools -jar target/*.jar extract --layers --destination /out

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=build /out/dependencies/ ./
COPY --from=build /out/spring-boot-loader/ ./
COPY --from=build /out/snapshot-dependencies/ ./
COPY --from=build /out/application/ ./
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","backend.jar"]
```
(The agent must adjust the jar name or extract layout to match the Spring Boot 4 tooling output; verify with `docker compose build`.)

**frontend/Dockerfile:** `node:22-alpine` build (`npm ci && npm run build -- --configuration production`) → `nginx:1.27-alpine` serving `dist/frontend/browser`, with `nginx.conf` providing an SPA fallback, `/api` proxy to `backend:8080`, security headers (see 08 §5), gzip/brotli, `Cache-Control: no-cache` for `ngsw.json` / `index.html`, and long cache for hashed assets. TLS on 443 uses mkcert certs mounted from `./certs` (git-ignored); the camera and service worker need a secure context on phones over LAN.

## 4. Makefile targets
| Target | Does |
|---|---|
| `make up` / `make down` | Build + start the full stack / stop |
| `make dev` | Infra only + host dev servers |
| `make test` | Backend `./mvnw verify` (with Testcontainers) + frontend `npm test -- --coverage` in containers |
| `make test-be` / `make test-fe` | One side only |
| `make e2e` | `docker compose -f docker-compose.yml -f docker-compose.test.yml up --abort-on-container-exit e2e` |
| `make lint` | Spotless check + Checkstyle + ESLint + Prettier check |
| `make fmt` | Apply formatters |
| `make vectors` | Regenerate `test-vectors/` (only on approved protocol changes) |
| `make logs` / `make psql` / `make reset-db` | Utilities |
| `make certs` | mkcert for `localhost` + LAN IP |
| `make scan` | gitleaks, OWASP dependency-check, `npm audit`, trivy |

## 5. Environment variables (`.env.example`)
```
DB_USER=hp
DB_PASSWORD=change-me
HP_MASTER_KEY=base64-32-bytes-change-me
HP_JWT_ISSUER=heypayments-dev
HP_CORS_ORIGINS=http://localhost:4200,https://localhost:8443
HP_LIMIT_OFFLINE_MAX=200000
HP_LIMIT_PER_TXN=50000
HP_BC_VALIDITY=P4D
HP_CLAIM_WINDOW=P7D
SPRING_PROFILE=dev
```

## 6. Spring profiles
`dev` (seed data, Swagger, verbose logs) · `test` (Testcontainers) · `prod` (no Swagger, JSON logs, strict CORS).

## 7. CI (GitHub Actions, `.github/workflows/ci.yml`)
Jobs on every push/PR: `lint` → `backend-test` (JDK 21, Testcontainers on the runner's Docker, JaCoCo gate) ∥ `frontend-test` (Node 22, Vitest coverage gate) → `contract` (OpenAPI diff, test vectors) → `build-images` → `e2e` (compose + Playwright, upload traces on failure) → `security` (gitleaks, dependency-check, npm audit, trivy). Branch protection on `main`: all jobs green and one review.

## 8. Git workflow
- `main` is always green. One branch per phase: `phase/N-short-name`; small Conventional Commits.
- Tag on phase completion: `v0.N.0`.
- The first commit must add a `.gitignore` covering `target/`, `node_modules/`, `dist/`, `.angular/`, `.env`, `certs/` (the scaffold currently has `backend/target` and `frontend/dist` on disk; they must never be committed).

## 9. Observability
Actuator health groups (liveness/readiness), Micrometer metrics (`hp.sync.items{status}`, `hp.settlement.amount`, `hp.fraud.cases{rule}`), JSON logs with `requestId`, `userId`, `deviceId`. Optional `make observe` adds Prometheus + Grafana with a provided dashboard JSON.
