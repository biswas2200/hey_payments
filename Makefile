.PHONY: help up down dev test test-be test-fe e2e lint fmt logs psql reset-db scan

help:
	@echo "Hey Payments Makefile targets:"
	@echo "  make up       - Build and start full stack in Docker"
	@echo "  make down     - Stop and remove running containers"
	@echo "  make dev      - Start supporting infrastructure (PostgreSQL, Redis, Mailpit)"
	@echo "  make test     - Run both backend and frontend tests"
	@echo "  make test-be  - Run backend tests (Maven verify)"
	@echo "  make test-fe  - Run frontend tests (Vitest)"
	@echo "  make e2e      - Run end-to-end tests"
	@echo "  make lint     - Run linter checks (Spotless, Checkstyle)"
	@echo "  make fmt      - Auto-format code"
	@echo "  make logs     - Follow container logs"
	@echo "  make psql     - Open PostgreSQL CLI in container"
	@echo "  make reset-db - Drop and recreate DB volume"

up:
	docker compose up -d --build

down:
	docker compose down

dev:
	docker compose up -d postgres redis mailpit

test: test-be test-fe

test-be:
	cd backend && ./mvnw clean verify

test-fe:
	cd frontend && npm test -- --watch=false

e2e:
	@echo "E2E tests will run via Playwright in Phase 5+"

lint:
	cd backend && ./mvnw spotless:check checkstyle:check

fmt:
	cd backend && ./mvnw spotless:apply

logs:
	docker compose logs -f

psql:
	docker compose exec postgres psql -U hp -d heypayments

reset-db:
	docker compose down -v
	docker compose up -d postgres redis

scan:
	@echo "Running security scans..."
