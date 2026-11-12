# Hey Payments

Fullstack application scaffold consisting of a **Spring Boot** backend and an **Angular** frontend.

## Project Structure

```text
hey_payments/
├── backend/                  # Spring Boot backend (Java 21, Maven)
│   ├── src/main/java/        # Application source code
│   │   └── com/heypayments/
│   │       ├── BackendApplication.java
│   │       ├── config/WebCorsConfig.java
│   │       └── controller/HealthController.java
│   ├── src/main/resources/
│   │   └── application.yml   # Server port (8080) & Actuator config
│   ├── pom.xml
│   └── mvnw                  # Maven wrapper
│
├── frontend/                 # Angular frontend (Angular 22 standalone)
│   ├── src/app/              # Application components & services
│   │   ├── services/api.service.ts
│   │   ├── app.ts
│   │   ├── app.html
│   │   └── app.scss
│   ├── proxy.conf.json       # Proxies /api requests to localhost:8080
│   ├── package.json
│   └── angular.json
│
└── README.md
```

---

## Getting Started

### 1. Run the Spring Boot Backend

Prerequisites: Java 21+

```bash
cd backend
./mvnw spring-boot:run
```

- **Base URL:** `http://localhost:8080`
- **Health Check:** `http://localhost:8080/api/health`
- **Actuator Health:** `http://localhost:8080/actuator/health`

### 2. Run the Angular Frontend

Prerequisites: Node.js 20+ / 24+

```bash
cd frontend
npm start
```

- **Frontend URL:** `http://localhost:4200`
- Development requests to `/api/*` are automatically forwarded to `http://localhost:8080` via `proxy.conf.json`.

---

## Testing & Building

### Backend
```bash
cd backend
./mvnw clean test     # Run unit tests
./mvnw clean package  # Build executable JAR in target/
```

### Frontend
```bash
cd frontend
npm test -- --watch=false   # Run Vitest unit tests
npm run build               # Build production assets in dist/
```
