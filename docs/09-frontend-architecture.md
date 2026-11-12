# 09 · Frontend Architecture (Angular 22 PWA)

## 1. Principles
- **Offline-first:** every screen must render from local data; the network only enriches it.
- **Status always visible:** a global connectivity + sync banner (`Online · Synced`, `Offline · 3 pending`, `Syncing…`).
- **Signals for state**, standalone components, `OnPush` everywhere, lazy-loaded feature routes, typed reactive forms.
- **Accessible by default:** WCAG 2.2 AA (see §8).
- **Design:** `docs/design/ui-spec.md`, `docs/design/reference/` and `docs/design/tokens.scss` are the approved visual source of truth (Calm Ledger v1). Fonts self-hosted, icons via `lucide-angular`.

## 2. Folder structure
```
frontend/src/
  app/
    app.config.ts            providers: router, http (interceptors), service worker, i18n
    app.routes.ts            /onboarding /auth /pay/** /merchant/** /admin/** (role guards)
    core/
      api/                   generated OpenAPI client (openapi-generator typescript-angular) + interceptors
      auth/                  AuthStore (signals), token refresh, role guards
      crypto/                KeyService (WebCrypto), Jcs, Sha256, Base45, Deflate, EnvelopeCodec
      protocol/              PrVerifier, VoucherBuilder, VoucherVerifier, ReceiptService (pure TS, 100% tested)
      offline/               db.ts (Dexie schema), OutboxRepository, CertificateStore, CrlStore
      sync/                  SyncEngine (online event, interval, manual), backoff, conflict mapping
      connectivity/          ConnectivityService: signal<'online'|'offline'|'degraded'>
      pin/                   PinService (PBKDF2, lockout)
    shared/
      ui/                    hp-button, hp-amount-input, hp-keypad, hp-status-pill, hp-qr-display,
                             hp-qr-scanner, hp-sheet, hp-toast, hp-empty-state, hp-skeleton, hp-stepper
      pipes/                 paise → "₹1,234.50" (Intl, en-IN / hi-IN), relative time
      a11y/                  live-announcer helpers, focus-trap directives
    features/
      onboarding/            carousel (what is offline pay), permissions (camera), language
      auth/                  register, OTP, login, set PIN
      payer/                 home, add-money, load-offline, scan-pay (scan → confirm → show voucher → scan receipt), history, tx-detail, settings
      merchant/              pos (keypad → PR QR → scan voucher → success), today, settlements, profile
      admin/                 dashboard (KPIs, charts), users, devices, ledger, reconciliation, fraud cases
  styles/
    _tokens.scss             copied verbatim from docs/design/tokens.scss (--hp-* custom properties)
    themes/                  light, dark, high-contrast
  ngsw-config.json
  manifest.webmanifest
```

## 3. IndexedDB schema (Dexie, versioned)
| Table | Key | Fields |
|---|---|---|
| `keys` | `deviceId` | `privateKey: CryptoKey`, `publicJwk`, `createdAt` |
| `certs` | `id` | BC or MC envelope, `status`, `spent`, `lastCounter`, `lastHash` |
| `outbox` | `voucherId` | `type`, `envelope`, `state`, `createdAt`, `attempts`, `lastError`, `receipt?` |
| `history` | `id` | denormalised list items for the history screen |
| `requests` | `prId` | merchant open/consumed PRs |
| `seen` | `[deviceId+counter]` | merchant dedupe index (M7) |
| `jwks`, `crl`, `meta` | key | cached server keys, CRL + `fetchedAt`, settings |

**Atomicity rule:** signing a voucher and saving it (plus updating `lastCounter`, `lastHash`, `spent`) happens in **one Dexie transaction**. If saving fails, the voucher QR must not be shown.

## 4. Sync engine
- Triggers: `online` event, app foreground, every 60 s while online, manual "Sync now", and the service-worker Background Sync tag `hp-sync` where supported.
- Batches up to 100 outbox items, `Idempotency-Key` = hash of sorted voucherIds, exponential backoff (2s → 5 min) with jitter.
- Maps server results back to outbox states; downloads the CRL delta and JWKS on every successful sync.
- Exposes signals `pendingCount`, `lastSyncedAt`, `syncState`.

## 5. Service worker (`ngsw-config.json`)
- `assetGroups`: app shell `prefetch`; fonts/icons `lazy`.
- `dataGroups`: `/api/v1/keys/jwks` and `/devices/crl` → `freshness` with a 3 s timeout; `/transactions` → `freshness`; **never** cache `POST`.
- Update flow: `SwUpdate.versionUpdates` → non-blocking "New version available" toast.

## 6. QR handling
- Generate: `qrcode` with ECC M, white quiet zone, max brightness hint text "Turn screen brightness up".
- Scan: `@zxing/browser` with back camera, torch toggle, scan-area overlay, haptic on success (`navigator.vibrate`).
- Multipart animated QR support (see protocol §3.6).
- Fallbacks: manual receipt short code; "Camera not working?" help sheet.

## 7. Routing and guards
`authGuard`, `roleGuard('PAYER'|'MERCHANT'|'ADMIN')`, `deviceRegisteredGuard`, `pinSetGuard`. Admin is desktop-first responsive; payer/merchant are mobile-first (360–430 px primary).

## 8. Accessibility (WCAG 2.2 AA)
- Colour contrast ≥ 4.5:1 text, ≥ 3:1 UI components (tokens are pre-validated in the Canva case study).
- Every status uses **icon + text + colour** (never colour alone).
- Touch targets ≥ 44×44 px (WCAG 2.5.8 needs 24 px; we exceed it).
- Keyboard and screen-reader operable: `cdkTrapFocus` in sheets, `LiveAnnouncer` for "Payment received ₹120", skip links, visible focus ring (2 px + offset).
- Respect `prefers-reduced-motion` (disable the success confetti/scale animations), `prefers-color-scheme`, and a large-text setting (root font scaling to 130%).
- Language: `lang` attribute switches per locale (en-IN, hi-IN) via Angular i18n.
- Automated: `axe-core` in Playwright on every page; manual TalkBack/NVDA check in Phase 8.

## 9. Testing
- Vitest + Angular Testing Library for components and services. `core/protocol` and `core/crypto` have **cross-language test vectors** shared with the backend (`/test-vectors/*.json`, see [10](10-testing-strategy.md)).
- Playwright E2E with `context.setOffline(true)` for the offline flow, using two browser contexts (payer and merchant), passing QR payloads through a test hook (`window.__hpTestQr`, test build only).
