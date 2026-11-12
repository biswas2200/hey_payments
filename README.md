# Hey Payments

An offline-first, cryptographically verifiable payment gateway built with **Spring Boot 4 (Java 21)** and **Angular 22 (PWA)**.

## Quickstart (Docker)

To run the complete stack locally with Docker Compose:

```bash
# 1. Copy environment variables
cp .env.example .env

# 2. Build and launch all services (PostgreSQL, Redis, Mailpit, Backend, Frontend)
make up

# 3. Verify health
curl -fsS http://localhost:8080/actuator/health
```

- **Frontend Application:** [http://localhost:8081](http://localhost:8081)
- **Backend API:** [http://localhost:8080](http://localhost:8080)
- **Actuator Health:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Swagger UI:** [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **Mailpit Web UI:** [http://localhost:8025](http://localhost:8025)

To stop running containers:

```bash
make down
```

---

## Local Development Workflow

For hot reload during active development:

```bash
# Start supporting infra (PostgreSQL, Redis, Mailpit)
make dev

# Terminal 1: Run Spring Boot backend
cd backend && ./mvnw spring-boot:run

# Terminal 2: Run Angular dev server
cd frontend && npm start
```

- In development mode, `ng serve` proxies `/api/*` to `http://localhost:8080` via `proxy.conf.json`.

---

## Testing & Quality Gates

```bash
# Run both backend and frontend test suites
make test

# Run backend tests only (Maven verify + Testcontainers PostgreSQL 16)
make test-be

# Run frontend tests only (Vitest)
make test-fe

# Run linters (Spotless, Checkstyle, ESLint, Prettier)
make lint

# Auto-format all code
make fmt
```

---

## Repository Layout

```text
hey_payments/
├── backend/                       # Spring Boot 4 / Java 21 backend
│   ├── src/main/java/com/heypayments/
│   │   ├── shared/                # Kernel: Money, UUIDv7, RFC 9457 errors, Clock, filters
│   │   └── BackendApplication.java
│   ├── src/main/resources/
│   │   ├── db/migration/          # Flyway SQL migrations (V1__baseline.sql)
│   │   └── application.yml
│   ├── Dockerfile
│   └── pom.xml
├── frontend/                      # Angular 22 standalone PWA
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/connectivity/ # ConnectivityService (signals)
│   │   │   └── app.ts / app.html  # Shell with WCAG 2.2 AA status banner
│   │   └── styles/tokens.scss     # Design tokens from Canva
│   ├── Dockerfile
│   └── nginx.conf                 # SPA fallback, /api proxy, CSP & security headers
├── test-vectors/                  # Cross-language test vectors (crypto, protocol)
├── docs/                          # Architecture & protocol specification
├── docker-compose.yml             # Orchestration for all services
├── Makefile                       # Developer targets
└── README.md
```
