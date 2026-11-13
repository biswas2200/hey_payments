# 002: Alignment of Phase 1 Auth and Device Endpoints in openapi.yaml
Status: PROPOSED

## Problem
During Phase 1 implementation and automated contract testing, several discrepancies were identified between `docs/openapi.yaml` and `docs/06-api-spec.md`:
1. **Missing Endpoints in openapi.yaml:**
   - `POST /auth/verify-otp`: Explicitly mandated in `docs/06-api-spec.md` line 35 (`{phone, otp} -> 200`) and PRD user journeys, but omitted from `docs/openapi.yaml`.
   - `POST /auth/logout`: Mandated in `docs/06-api-spec.md` line 38 (`any -> 204`) for server-side refresh token family invalidation, but omitted from `docs/openapi.yaml`.
2. **Path Prefix Representation:**
   - Springdoc outputs OpenAPI specifications with the application context and controller mount prefix (`/api/v1/auth/...`, `/api/v1/devices/...`, `/api/v1/keys/...`).
   - `docs/openapi.yaml` uses `servers: [{ url: http://localhost:8080/api/v1 }]` with relative paths (`/auth/register`, `/devices`, `/keys/jwks`).
3. **Register & Verify-OTP Response Payloads:**
   - In `docs/openapi.yaml`, `POST /auth/register` specifies `IdResponse` (`{ id: string }`). In backend, `RegisterResponse` returns `{ userId: UUID, phone: String }` matching domain models.
   - `POST /auth/verify-otp` validates the OTP and activates the user account, returning `{ status: "SUCCESS", message: "User verified" }` or `{ accessToken, expiresIn, user }`.

## Options (with trade-offs)
- **Option 1 (Recommended):** Update `docs/openapi.yaml` to include `/auth/verify-otp` and `/auth/logout`, align schema response types, and configure the contract test to normalize the `/api/v1` server prefix when asserting conformance.
- **Option 2:** Drop OTP verification and logout endpoints to strictly conform to `docs/openapi.yaml`. (Rejected: Violates PRD, security rules in doc 08, and test acceptance criteria).

## Recommendation
Adopt Option 1. The architect will merge `/auth/verify-otp` and `/auth/logout` schemas into `docs/openapi.yaml`.
Until the spec file is updated by the architect, the automated contract test asserts that all endpoints present in `docs/openapi.yaml` for Phase 1 are faithfully implemented, while permitting the intentional extensions documented in this proposal.

## Impacted docs / code
- `docs/openapi.yaml` (pending architect merge)
- `backend/src/test/java/com/heypayments/contract/OpenApiContractTest.java`
- `frontend/src/app/core/api/` (generated client)
