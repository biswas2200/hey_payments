#!/usr/bin/env bash
set -euo pipefail

# Verifies per docs/reviews/REVIEW-phase-0.md MUST-FIX 3 that only frontend publishes on 0.0.0.0.
# All internal services (postgres, redis, mailpit, backend) must bind strictly to 127.0.0.1 loopback.

COMPOSE_CONFIG=$(docker compose config)

# Extract any service publishing ports without 127.0.0.1 (excluding frontend)
# Format in compose config is host_ip: 127.0.0.1 or published: "..." with host_ip: 127.0.0.1
VIOLATIONS=$(python3 - << 'EOF'
import sys, subprocess, yaml

try:
    res = subprocess.run(["docker", "compose", "config", "--format", "json"], capture_output=True, text=True, check=True)
    import json
    data = json.loads(res.stdout)
except Exception:
    res = subprocess.run(["docker", "compose", "config"], capture_output=True, text=True, check=True)
    import yaml
    data = yaml.safe_load(res.stdout)

services = data.get("services", {})
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
