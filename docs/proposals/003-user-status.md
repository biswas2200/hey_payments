# 003: User Status PENDING_VERIFICATION Lifecycle
Status: ACCEPTED (2026-11-13, merged into docs/openapi.yaml by the architect)

## Problem
In `docs/05-data-model.md`, `users.status` includes `ACTIVE`, `LOCKED`, and `DISABLED`. Currently, newly registered accounts are assigned `LOCKED` until OTP verification. However, `LOCKED` is designated in doc 05 §2.1 and doc 08 as the fraud-freeze and security lockout state. Setting newly registered users to `LOCKED` and transitioning `LOCKED -> ACTIVE` unconditionally on OTP verification creates a severe security flaw: a fraud-locked user could request an OTP and unlock their account.

## Options (with trade-offs)
- **Option 1 (Recommended):** Introduce `PENDING_VERIFICATION` into `users.status`. Initial registration state is `PENDING_VERIFICATION`. OTP verification strictly transitions `PENDING_VERIFICATION -> ACTIVE`. If a user is `LOCKED` (due to fraud or excessive failed attempts), OTP verification is rejected and the account remains `LOCKED`.
- **Option 2:** Keep `LOCKED` and track verification via a separate boolean `phone_verified`. (Adds redundant schema state; does not separate pending onboarding from security lockouts).

## Recommendation
Adopt Option 1. Add `PENDING_VERIFICATION` to `users.status` via Flyway migration `V4__user_status_pending_verification.sql`. Update `AuthService` registration and OTP verification state machine.

## Impacted docs / code
- `docs/05-data-model.md`
- `backend/src/main/resources/db/migration/V4__user_status_pending_verification.sql`
- `backend/src/main/java/com/heypayments/auth/domain/model/UserStatus.java`
- `backend/src/main/java/com/heypayments/auth/application/AuthService.java`
