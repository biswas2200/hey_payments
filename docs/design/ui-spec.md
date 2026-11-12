# UI Specification: Angular frontend (Calm Ledger v1)

**This is the visual source of truth for the frontend.** The product owner signed off on this look and logo. Build it in Angular so the result looks the same as the reference screens.

| Asset | Path | Use |
|---|---|---|
| Reference screens (open in a browser) | [reference/screens.html](reference/screens.html) | Every token, component and screen, rendered with the exact CSS |
| Per-screen PNGs | [reference/screens/](reference/screens/) `P1…P6`, `M1…M4`, `A1` | Visual comparison targets (2× resolution) |
| Per-screen HTML | `reference/screens/*.html` | Inspect exact spacing and CSS in DevTools |
| Tokens (SCSS, Angular-ready) | [tokens.scss](tokens.scss) | Copy to `frontend/src/styles/_tokens.scss` |
| Tokens (JSON) | [tokens.json](tokens.json) | Same values, machine-readable |
| Logo | [brand/](brand/) `logo-mark.svg`, `logo-wordmark-light.svg`, `logo-wordmark-dark.svg` | Header, splash, login |
| PWA icons | `brand/app-icon-192.png`, `app-icon-512.png`, `app-icon-maskable.svg` | `manifest.webmanifest`, favicon |
| Wireframes (structure + flow) | [wireframes/hey-payments-wireframes.png](wireframes/hey-payments-wireframes.png) · `.excalidraw` | Screen inventory and navigation |
| Case study deck | [export/Hey-Payments-UIUX-Case-Study-v3.pdf](export/Hey-Payments-UIUX-Case-Study-v3.pdf) slides 27, 32–35 | Rationale, motion and accessibility notes |

The reference CSS lives in `deck-source/style.css` (classes `.banner`, `.balcard`, `.btn`, `.keypad`, `.mini`, `.pill`, `.qr`, `.lbl`). Port it into component styles that use the `--hp-*` tokens. **Do not copy class names into global CSS.**

---

## 1. Brand
- **Logo mark:** 64×64 rounded square (radius 20), gradient `--hp-grad-brand`, Lucide `wifi-off` glyph in `--hp-night`, stroke 2.4. Use the SVG as-is; never recolour it or redraw it in CSS.
- **Wordmark:** `hey` + saffron `.` + `payments`, Space Grotesk 700. Light backgrounds use ink text (`logo-wordmark-light.svg`), dark backgrounds use white text (`-dark.svg`).
- **Clear space:** at least half the mark's height on every side. Minimum mark size: 24 px.
- **Where:** splash/onboarding (mark 96 px), auth header (wordmark 32 px high), admin sidebar (wordmark, dark).

## 2. Foundations
- **Fonts:** self-host for offline use: `@fontsource/space-grotesk` (500/600/700), `@fontsource/inter` (400–800), `@fontsource/jetbrains-mono` (500/700), `@fontsource/noto-sans-devanagari`. **No Google Fonts CDN at runtime.**
- **Icons:** `lucide-angular` pinned to the Lucide version used in the reference (0.460.x icon set), stroke 2. Names in use: `wifi-off`, `refresh-cw`, `circle-check`, `circle-x`, `scan-line`, `qr-code`, `store`, `badge-check`, `check`, `battery-full`, `signal-zero`, `arrow-right`, `chevron-right`.
- **Money:** always Space Grotesk, `₹` prefix, `en-IN` grouping (`₹1,23,450.00`). Use one `paise` pipe everywhere.
- **Layout:** mobile-first, design width 360–430 px, screen padding `--hp-screen-pad` (18 px), vertical rhythm 12 px between blocks. Admin: ≥ 1200 px, 170 px sidebar.
- **Themes:** light (default), dark (system), large text (130%), reduced motion. All are already defined in `tokens.scss`.

## 3. Components (`shared/ui`, build in this order)
| Component | Anatomy & rules | Reference |
|---|---|---|
| `hp-sync-banner` | Full-width, radius 12, padding 8×12, 12.5 px/700. Icon 15 px + text. Variants `offline` (offline-soft bg, offline fg, `wifi-off`), `pending` (`refresh-cw`), `settled`/online (`circle-check`). Driven by `SyncEngine` signals. `role="status"` | top of every mobile screen |
| `hp-status-pill` | Pill, 7×16 padding, 700, white dot + label, solid status colour. Small variant in lists: coloured text `● Pending` 11 px/700 | gallery §2 |
| `hp-button` | Height 48, radius 16, 15 px/800. `primary` (teal), `hero` (saffron, ink text, used **only** for "Pay without internet"), `ghost` (1.5 px line border). Optional leading icon 18 px. Disabled = 40% opacity, no colour change | gallery §2 |
| `hp-balance-card` | Radius 20, padding 16×18, `--hp-grad-balance`, decorative mint circle top-right (25% opacity). UPPERCASE label 11 px, amount 34 px Space Grotesk 700, meta 12 px, progress track 6 px with saffron fill = spent/limit | P1 |
| `hp-keypad` | 3×4 grid, gap 7, keys 44 px high, radius 12, white with 1 px line border, Space Grotesk 19 px/600. Keys `1–9`, `·`/empty, `0`, `⌫`. Haptic 10 ms on press. Keyboard input supported | P2, P4, M1 |
| `hp-pin-dots` | 6 dots of 14 px. Filled = ink, empty = 2 px ink ring. `aria-label="4 of 6 digits entered"` | P4 |
| `hp-amount-display` | Label 11 px UPPERCASE muted, amount 46–50 px Space Grotesk 700, helper 12 px muted | P2, P4, M1 |
| `hp-merchant-chip` | White row, radius 16, 42 px icon tile (primary-soft, `store`), name 14 px/800, "Verified merchant" settled-coloured with `badge-check` | P4 |
| `hp-qr-display` | White container, radius 18, padding 12, QR at ECC-M, centre saffron square. Raises screen brightness hint text | P5, M2, M3 |
| `hp-qr-scanner` | Dark viewport (`--hp-grad-night`), 200 px scan frame with 4 px mint border and radius 28, saffron scan line, helper text, Torch / Enter code ghost buttons | P3 |
| `hp-list-row` | White, radius 16, 1 px line border, padding 12×14. Left: name 14 px/700 + time muted. Right: amount 700 + small status text | P1, M4 |
| `hp-success` | 84–96 px circle (settled-soft on payer, solid settled on merchant), check icon, scale-in 600 ms `--hp-ease-enter` + chime + haptic. Announced via `LiveAnnouncer` | P6, M3 |
| `hp-takings-card` | `--hp-grad-night`, amount 34 px, two tags (settled / pending) | M4 |
| Admin `hp-kpi`, `hp-chart`, `hp-table` | White cards radius 14, label 11 px UPPERCASE, value 26 px Space Grotesk. Charts must have a table fallback | A1 |

## 4. Screens
Copy is final unless marked. Each screen starts with `hp-sync-banner`.

| ID | Route | Content, top to bottom | Primary action |
|---|---|---|---|
| P1 Home | `/pay` | greeting + avatar · `hp-balance-card` (offline) · main wallet row with "Add +" · **hero** button "Pay without internet" · "Recent" label · 2–5 `hp-list-row` | Pay without internet → P3 |
| P2 Load offline | `/pay/load` | title "Load offline balance" · explainer (12.5 px muted) · `hp-amount-display` · quick chips ₹200/₹500/₹1,000 · `hp-keypad` · button "Load ₹{amount}" | Load (requires sync first; show `SYNC_REQUIRED` as an inline sheet) |
| P3 Scan | `/pay/scan` | `hp-qr-scanner` filling the remaining height · Torch · Enter code | auto-advance on valid PR |
| P4 Confirm + PIN | `/pay/confirm` | `hp-merchant-chip` · "You pay" + amount + note · "Enter payment PIN" · `hp-pin-dots` · `hp-keypad` · Cancel | 6th digit submits |
| P5 Voucher | `/pay/voucher/:id` | "Show to {merchant}" · `hp-qr-display` 210 px · amount · "Signed voucher #{counter} · keep screen bright" · ghost "Scan merchant receipt" · link "Merchant didn't receive?" | Scan receipt → P6 |
| P6 Receipt | `/pay/receipt/:id` | `hp-success` · "Payment received by merchant" · amount · details (receipt code mono, offline left, status) · Done · ghost Share receipt | Done → P1 |
| M1 Amount | `/merchant` | "New payment" · `hp-amount-display` · optional note field · `hp-keypad` · button "Show QR" (`qr-code` icon) | → M2 |
| M2 Request | `/merchant/request` | "Show this to the customer" · `hp-qr-display` · amount · "Request expires in m:ss" · button "Scan customer's code" · Cancel request | Scan → verify → M3 |
| M3 Received | `/merchant/received/:id` | solid `hp-success` · "Payment received" · amount · "from {name} · voucher verified offline" · receipt QR 140 px + code · "New payment" | → M1 |
| M4 Today | `/merchant/today` | `hp-takings-card` · by-hour bar chart · rows · ghost "Export CSV" | — |
| A1 Admin | `/admin` | sidebar (Dashboard, Users, Devices, Ledger, Reconciliation, Fraud cases, Limits) · 4 KPIs · 14-day settled line chart · fraud table with Review / Freeze | — |

Screens without a reference (onboarding, auth, history, settings, errors) are composed from these components with the same spacing. List them in PROGRESS.md for architect review.

## 5. Motion & interaction
Tap feedback 100 ms · sheet in 240 ms · screen transition 300 ms · success 600 ms · sync spinner → tick 400 ms, all on `--hp-ease-standard`/`enter`. Gestures: pull to sync (Home, history), long-press to reveal the receipt code, swipe back. Every gesture also has a button. Respect `prefers-reduced-motion` (fades only).

## 6. Accessibility (blocking for review)
WCAG 2.2 AA · status = colour + icon + text · touch targets ≥ 44 px · visible 2 px focus ring in `--hp-focus` · `LiveAnnouncer` for "Payment received ₹120" and sync results · labels on every input · `lang` switches en-IN / hi-IN · axe: 0 serious or critical issues.

## 7. Design acceptance (how the reviewer checks it)
1. Playwright visual test per reference screen at 390×844: `expect(page).toHaveScreenshot()`. Baselines are committed after architect approval. Seed the same demo data as the reference (Priya, Ravi General Store, ₹120, ₹1,640/₹2,000).
2. Side-by-side compare against `reference/screens/*.png`. Hierarchy, spacing, colour and copy must match. Font anti-aliasing differences are fine.
3. Lighthouse PWA installable, accessibility ≥ 95.
