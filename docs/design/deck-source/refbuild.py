"""Build the frontend design reference (docs/design/reference) from the same parts as the deck."""
import sys, os, re
sys.path.insert(0, '.')
from parts import *
from slides3 import admin_dash
import re as _re
QRJS = _re.search(r"QRJS = r'''(.*?)'''", open('build.py').read(), _re.S).group(1)

OUT = sys.argv[1]
os.makedirs(f"{OUT}/screens", exist_ok=True)
css = open('style.css').read()
logo = open(sys.argv[2]).read()

def scr_merchant_amount():
    return f'''{banner("off","Offline · accepting payments")}
<div style="font-weight:800;font-size:18px">New payment</div>
<div style="text-align:center"><div class="lbl">Amount</div><div class="disp" style="font-size:50px;font-weight:700">₹120</div><div style="font-size:12px;color:var(--muted)">Max ₹500 per offline payment</div></div>
<div class="mini" style="color:var(--muted)">Note (optional): 2× chai, 1 samosa</div>
<div class="keypad">{''.join(f"<div>{k}</div>" for k in ["1","2","3","4","5","6","7","8","9","·","0","⌫"])}</div>
<div class="btn" style="display:flex;gap:8px;justify-content:center;align-items:center">{I("qr-code")}Show QR</div>'''

SCREENS = [
    ("P1", "payer-home", "Payer · Home", scr_home()),
    ("P2", "payer-load-offline", "Payer · Load offline balance", scr_load()),
    ("P3", "payer-scan", "Payer · Scan merchant QR", scr_scan()),
    ("P4", "payer-confirm-pin", "Payer · Confirm + PIN", scr_confirm()),
    ("P5", "payer-voucher", "Payer · Show signed voucher", scr_payer_voucher()),
    ("P6", "payer-receipt", "Payer · Receipt (pending sync)", scr_receipt()),
    ("M1", "merchant-amount", "Merchant · Enter amount", scr_merchant_amount()),
    ("M2", "merchant-request-qr", "Merchant · Payment request QR", scr_merchant_pr()),
    ("M3", "merchant-received", "Merchant · Payment received", scr_merchant_ok()),
    ("M4", "merchant-today", "Merchant · Today's takings", scr_merchant_today()),
]

HEAD = f'''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap" rel="stylesheet">
<script src="https://unpkg.com/lucide@0.460.0/dist/umd/lucide.min.js"></script><style>{css}
html,body{{background:var(--paper)}} .wrap{{padding:48px 56px;max-width:1900px}} h2.sec{{font-family:'Space Grotesk';font-size:40px;margin:56px 0 8px}} p.lead{{font-size:19px;color:var(--ink2);margin-bottom:24px;max-width:1100px}}
.gallery{{display:flex;flex-wrap:wrap;gap:40px}} .cell{{display:flex;flex-direction:column;gap:10px;align-items:center}} .cell > b{{font-family:'Space Grotesk';font-size:20px}} .cell > span{{font-size:14px;color:var(--muted)}}
.sw{{width:190px;border-radius:16px;overflow:hidden;border:1px solid var(--line);background:#fff}} .sw div{{height:90px}} .sw p{{padding:10px 12px;font-size:14px;line-height:1.4}}
</style></head><body>'''
TAIL = f'<script>{QRJS}lucide.createIcons();</script></body></html>'

tok = [("--paper", "#F7F5F0", "Page background"), ("--card", "#FFFFFF", "Cards, sheets"), ("--ink", "#0E1B2C", "Text, phone frame"), ("--ink2", "#334155", "Secondary text"),
       ("--teal", "#0F766E", "Primary / buttons"), ("--mint", "#2DD4BF", "Dark-mode primary"), ("--saff", "#F5A524", "Hero CTA, amounts on dark"), ("--night", "#0B1420", "Dark surfaces"),
       ("--off", "#4338CA", "Status: Offline"), ("--pend", "#B45309", "Status: Pending sync"), ("--ok", "#15803D", "Status: Settled"), ("--bad", "#B91C1C", "Status: Failed")]
sw = "".join(f'<div class="sw"><div style="background:{h}"></div><p><b class="mono">{n}</b><br>{h}<br>{u}</p></div>' for n, h, u in tok)
comps = f'''<div class="gallery" style="align-items:flex-start">
<div class="cell"><b>hp-status-pill</b><div style="display:flex;gap:10px"><span class="pill p-off">Offline</span><span class="pill p-pend">Pending sync</span><span class="pill p-ok">Settled</span><span class="pill p-bad">Failed</span></div></div>
<div class="cell" style="width:330px"><b>hp-sync-banner</b>{banner("off","Offline · 2 payments pending sync")}{banner("pend","Pending sync · will settle when online")}{banner("ok","Online · synced 1 min ago")}</div>
<div class="cell" style="width:300px"><b>hp-button</b><div class="btn" style="width:100%">Primary</div><div class="btn sf" style="width:100%">Hero CTA (saffron)</div><div class="btn gh" style="width:100%">Ghost</div></div>
<div class="cell" style="width:300px"><b>hp-balance-card</b><div class="balcard" style="width:100%"><small>Offline balance</small><div class="amt">₹1,640</div><div style="font-size:12px;opacity:.85;margin-top:4px">of ₹2,000 · valid 3 days</div><div style="height:6px;border-radius:6px;background:rgba(255,255,255,.25);margin-top:10px"><div style="width:82%;height:100%;border-radius:6px;background:var(--saff)"></div></div></div></div>
<div class="cell" style="width:260px"><b>hp-keypad</b><div class="keypad" style="width:100%">{''.join(f"<div>{k}</div>" for k in ["1","2","3","4","5","6","7","8","9","·","0","⌫"])}</div></div>
<div class="cell"><b>hp-qr-display</b>{qr("ref", 170)}</div></div>'''

phones = "".join(f'<div class="cell"><div style="zoom:.9">{phone(h)}</div><b>{c} · {t.split(" · ")[1]}</b><span>screens/{c}-{f}.png</span></div>' for c, f, t, h in SCREENS)
page = f'''{HEAD}<div class="wrap">
<div style="display:flex;justify-content:space-between;align-items:center">{logo}<span class="tag" style="background:var(--tealsoft);color:var(--teal)">Design reference v1 · source of truth for the Angular build</span></div>
<h2 class="sec">1 · Colour tokens</h2><p class="lead">Use only these CSS custom properties (see tokens.scss). Status is always colour + icon + label.</p><div class="gallery" style="gap:18px">{sw}</div>
<h2 class="sec">2 · Core components</h2><p class="lead">Build these first in <span class="mono">shared/ui</span>. Every screen is composed from them.</p>{comps}
<h2 class="sec">3 · Mobile screens (360–430 px)</h2><p class="lead">Payer P1–P6 and merchant M1–M4. Match spacing, hierarchy and copy. The phone frame is presentation only and is not part of the app.</p><div class="gallery">{phones}</div>
<h2 class="sec">4 · Admin console (desktop ≥ 1200 px)</h2><p class="lead">Sidebar navigation, KPI row, charts and the fraud table.</p>{admin_dash()}
</div>{TAIL}'''
open(f"{OUT}/screens.html", "w").write(page)
for c, f, t, h in SCREENS:
    open(f"{OUT}/screens/{c}-{f}.html", "w").write(f'{HEAD}<div style="padding:20px;display:inline-block">{phone(h)}</div>{TAIL}')
open(f"{OUT}/screens/A1-admin-dashboard.html", "w").write(f'{HEAD}<div style="padding:20px;display:inline-block">{admin_dash()}</div>{TAIL}')
print("ok", len(SCREENS) + 1, "screens")
