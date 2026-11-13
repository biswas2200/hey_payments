#!/usr/bin/env bash
set -euo pipefail

# Verifies per docs/reviews/REVIEW-phase-0.md MUST-FIX 3 that only frontend publishes on 0.0.0.0.
# All internal services (postgres, redis, mailpit, backend) must bind strictly to 127.0.0.1 loopback.

ENV_ARGS=()
if [ -f .env ]; then
  ENV_ARGS=(--env-file .env)
elif [ -f .env.example ]; then
  ENV_ARGS=(--env-file .env.example)
fi

COMPOSE_CONFIG=$(docker compose "${ENV_ARGS[@]}" config)

export COMPOSE_CONFIG

# Extract any service publishing ports without 127.0.0.1 (excluding frontend)
# Format in compose config is host_ip: 127.0.0.1 or published: "..." with host_ip: 127.0.0.1
VIOLATIONS=$(python3 - << 'EOF'
import os, sys, yaml

data = yaml.safe_load(os.environ.get("COMPOSE_CONFIG", ""))
services = data.get("services", {}) if data else {}
violations = []

for svc_name, svc_cfg in services.items():
    if svc_name == "frontend":
        continue
    ports = svc_cfg.get("ports", [])
    for p in ports:
        if isinstance(p, dict):
            host_ip = p.get("host_ip")
            published = p.get("published")
            if host_ip != "127.0.0.1":
                violations.append(f"{svc_name}: port {published} bound to host_ip '{host_ip}' (expected 127.0.0.1)")
        elif isinstance(p, str):
            # e.g. "5432:5432" or "127.0.0.1:5432:5432"
            if not p.startswith("127.0.0.1:"):
                violations.append(f"{svc_name}: port {p} does not start with 127.0.0.1:")

if violations:
    for v in violations:
        print(f"SECURITY VIOLATION: {v}", file=sys.stderr)
    sys.exit(1)
else:
    print("PORT EXPOSURE CHECK PASSED: Only frontend publishes on 0.0.0.0; internal services bound to 127.0.0.1.")
    sys.exit(0)
EOF
)

echo "$VIOLATIONS"

# Verify per docs/reviews/REVIEW-phase-0.md Condition C that prod compose config resolves SPRING_DATA_REDIS_PASSWORD
echo "Checking prod compose environment variable resolution..."
PROD_COMPOSE_OUT=$(SPRING_PROFILE=prod docker compose --env-file .env.example config)
if ! echo "$PROD_COMPOSE_OUT" | grep -q "SPRING_DATA_REDIS_PASSWORD: changeme"; then
    echo "ERROR: backend service environment in docker compose does not configure SPRING_DATA_REDIS_PASSWORD" >&2
    exit 1
fi
if ! echo "$PROD_COMPOSE_OUT" | grep -q "HP_MASTER_KEY: base64-32-bytes-change-me"; then
    echo "ERROR: backend service environment in docker compose does not configure HP_MASTER_KEY" >&2
    exit 1
fi
echo "PROD COMPOSE CONFIG CHECK PASSED: SPRING_DATA_REDIS_PASSWORD and HP_MASTER_KEY are wired correctly."
