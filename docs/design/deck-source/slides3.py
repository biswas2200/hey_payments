from parts import *
from slides1 import divider
from slides2 import U3

U4 = "Unit IV · High-fidelity Prototype & Evolving UX"
U5 = "Unit V · Product Management & Analytics"

def s29():
    def ph(label, cta, color, tc):
        return f'''<div style="display:flex;flex-direction:column;align-items:center;gap:14px"><span class="tag" style="background:{"var(--ink)" if label=="B" else "var(--paper2)"};color:{"#fff" if label=="B" else "var(--ink)"};font-size:18px">Variant {label}</span>
<div style="zoom:.62">{phone(scr_home().replace("Pay without internet", cta))}</div></div>'''
    body = f'''<div style="display:grid;grid-template-columns:1fr 520px 1fr;gap:26px;height:100%">
<div class="card"><h4>Design the test</h4><div class="grow" style="gap:14px;font-size:20px;color:var(--ink2)">
<div><b style="color:var(--ink)">Hypothesis:</b> naming the benefit ("without internet") beats naming the mode ("offline").</div>
<div><b style="color:var(--ink)">One variable:</b> CTA label only. Colour, size and position stay fixed.</div>
<div><b style="color:var(--ink)">Primary metric:</b> % of home visits that start an offline payment.</div>
<div><b style="color:var(--ink)">Guardrail:</b> payment errors must not rise.</div>
<div><b style="color:var(--ink)">Sample size:</b> base 40%, detect +5 pp, α 0.05, power 0.8 → ~1,500 users per arm, 50/50 random split, 2 weeks.</div></div></div>
<div class="card" style="flex-direction:row;justify-content:space-around;align-items:center;padding:20px">{ph("A","Pay offline","","")}{ph("B","Pay without internet","","")}</div>
<div style="display:flex;flex-direction:column;gap:20px"><div class="card" style="flex:1"><h4>Result</h4>
<div class="grow"><div style="display:flex;gap:22px;align-items:flex-end;height:170px;padding:0 20px">
<div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:8px"><b style="font-size:24px">40.2%</b><div style="width:100%;height:110px;background:var(--paper2);border-radius:12px 12px 0 0"></div><span style="font-weight:700">A</span></div>
<div style="flex:1;display:flex;flex-direction:column;align-items:center;gap:8px"><b style="font-size:24px;color:var(--teal)">47.4%</b><div style="width:100%;height:130px;background:var(--teal);border-radius:12px 12px 0 0"></div><span style="font-weight:700">B</span></div></div>
<p style="font-size:20px"><b>+18% relative</b>, p &lt; 0.01. Error rate unchanged.</p></div></div>
<div class="card dark"><h4>Using the result</h4><p style="font-size:20px">Shipped B everywhere. The same "say the benefit" rule was applied to merchant copy: "Accept without internet".</p></div></div></div>'''
    return slide(29, U3, "A/B <em>testing</em>", "One variable, one metric, one decision: which label makes people try offline payment?", body,
                 "Principles of A/B Testing · Designing and Running A/B Tests · Using results to improve UX", "Kohavi et al., Trustworthy Online Controlled Experiments [R18]", "docs/01-PRD.md §8 (Activation)", extra=SAMPLE)

def s30():
    return divider(30, "IV", "High-fidelity prototype &amp; evolving UX", ["Payer app: hi-fi screens", "The offline handshake, end to end", "Merchant POS &amp; admin console", "Gestures, motion &amp; accessibility", "Maintaining &amp; evolving UX"], "Syllabus Unit IV · 8 hours")

def s31():
    labels = [("Home", "Offline balance, honest status, saffron CTA"), ("Load offline", "Clear cap, quick amounts, numeric keypad"), ("Confirm + PIN", "Verified merchant, hero amount, 44px keys"), ("Receipt", "Proof + code, 'Pending sync', next step")]
    phones = [scr_home(), scr_load(), scr_confirm(), scr_receipt()]
    cols = "".join(f'<div style="display:flex;flex-direction:column;align-items:center;gap:16px"><div style="zoom:.8">{phone(p)}</div><div style="text-align:center"><div class="disp" style="font-size:26px;font-weight:700">{t}</div><div style="font-size:18px;color:var(--ink2);width:300px">{d}</div></div></div>' for p,(t,d) in zip(phones, labels))
    body = f'<div style="display:flex;justify-content:space-between;align-items:flex-start;height:100%;padding:0 30px">{cols}</div>'
    return slide(31, U4, "Hi-fi prototype · <em>payer app</em>", "Built from the Calm Ledger tokens: four key moments sharing one status language.", body,
                 "Build a high-fidelity prototype (Figma)", "Material Design 3 components &amp; states [R19] · Figma hi-fi [R8]", "docs/09-frontend-architecture.md §2")

def s32():
    ar = lambda t: f'<div style="display:flex;flex-direction:column;align-items:center;gap:10px;width:200px"><div style="font-size:16px;font-weight:800;letter-spacing:.1em;color:var(--saff);text-align:center">{t}</div><div style="color:var(--saff)"><i data-lucide="arrow-right" style="width:56px;height:56px"></i></div><div class="mono" style="font-size:14px;color:#8DA0B8;text-align:center">QR · ECDSA-signed<br>no network</div></div>'
    def p(scr, who, step):
        return f'<div style="display:flex;flex-direction:column;align-items:center;gap:12px"><span class="tag" style="background:rgba(45,212,191,.15);color:var(--mint);font-size:17px">{step} · {who}</span><div style="zoom:.84">{phone(scr)}</div></div>'
    body = f'''<div style="display:flex;justify-content:center;align-items:center;gap:6px;height:100%">
{p(scr_merchant_pr(),"Merchant","1")}{ar("PAYMENT REQUEST")}{p(scr_payer_voucher(),"Payer","2")}{ar("SIGNED VOUCHER")}{p(scr_merchant_ok(),"Merchant","3")}</div>'''
    return slide(32, U4, "Hi-fi prototype · <em>the offline handshake</em>", "Three QR exchanges, zero bars: request → voucher → receipt, verified on-device.", body,
                 "High-fidelity prototype · Designing user flows", "Prototype of the HP-OFFLINE v1 protocol", "docs/03-offline-protocol.md §4", cls="dark")

def admin_dash():
    kpi = "".join(f'<div style="background:#fff;border:1px solid var(--line);border-radius:14px;padding:12px 14px"><div class="lbl">{l}</div><div class="disp" style="font-size:26px;font-weight:700;color:{c}">{v}</div></div>' for l,v,c in [("GMV settled", "₹12.4L", "var(--ink)"), ("Payments", "18,420", "var(--ink)"), ("Pending escrow", "₹1.9L", "var(--pend)"), ("Open fraud cases", "3", "var(--bad)")])
    line = "M0,90 C40,80 70,60 110,64 S180,40 220,46 S300,20 340,26 S420,10 470,14"
    rows = "".join(f'<tr><td style="padding:8px 6px;border-top:1px solid var(--line)"><span class="tag" style="background:{bg};color:{fg};font-size:11px">{sev}</span></td><td style="padding:8px 6px;border-top:1px solid var(--line);font-size:13px;font-weight:700">{r}</td><td style="padding:8px 6px;border-top:1px solid var(--line);font-size:12px" class="mono">{d}</td><td style="padding:8px 6px;border-top:1px solid var(--line);font-size:12px;color:var(--teal);font-weight:800">Review</td></tr>' for sev,bg,fg,r,d in [("HIGH","var(--badsoft)","var(--bad)","Double spend","dev-7f3a"),("MED","var(--pendsoft)","var(--pend)","Merchant anomaly","m-1192"),("LOW","var(--offsoft)","var(--off)","Stale sync","dev-22c1")])
    return f'''<div style="width:880px;height:560px;border-radius:20px;background:#0A111C;padding:10px;box-shadow:0 30px 60px rgba(14,27,44,.25);flex:none">
<div style="width:100%;height:100%;border-radius:12px;background:var(--paper);display:grid;grid-template-columns:170px 1fr;overflow:hidden">
<div style="background:var(--ink);padding:18px 14px;display:flex;flex-direction:column;gap:10px;color:#C9D4E2;font-size:13px;font-weight:600">
<div class="disp" style="color:#fff;font-size:17px;font-weight:700;margin-bottom:8px">hey<span style="color:var(--saff)">.</span>admin</div>
{"".join(f'<div style="padding:8px 10px;border-radius:8px;{"background:rgba(45,212,191,.15);color:var(--mint)" if i==0 else ""}">{x}</div>' for i,x in enumerate(["Dashboard","Users","Devices","Ledger","Reconciliation","Fraud cases","Limits"]))}</div>
<div style="padding:16px;display:flex;flex-direction:column;gap:12px"><div style="display:grid;grid-template-columns:repeat(4,1fr);gap:10px">{kpi}</div>
<div style="display:grid;grid-template-columns:1.2fr 1fr;gap:12px;flex:1"><div style="background:#fff;border:1px solid var(--line);border-radius:14px;padding:12px"><div class="lbl">Settled volume · 14 days</div>
<svg viewBox="0 0 470 110" style="width:100%;height:150px"><path d="{line} L470,110 L0,110 Z" fill="rgba(20,184,166,.15)"/><path d="{line}" fill="none" stroke="#0F766E" stroke-width="3"/></svg></div>
<div style="background:#fff;border:1px solid var(--line);border-radius:14px;padding:12px"><div class="lbl">Fraud cases</div><table>{rows}</table></div></div></div></div></div>'''

def s33():
    body = f'''<div style="display:flex;gap:36px;align-items:center;justify-content:center;height:100%">
<div style="display:flex;flex-direction:column;align-items:center;gap:12px"><span class="tag" style="background:var(--saffsoft);color:#8A5300;font-size:17px">Merchant POS</span><div style="zoom:.78">{phone(scr_merchant_today())}</div></div>
<div style="display:flex;flex-direction:column;align-items:center;gap:12px"><span class="tag" style="background:var(--saffsoft);color:#8A5300;font-size:17px">Merchant · scan</span><div style="zoom:.78">{phone(scr_scan())}</div></div>
<div style="display:flex;flex-direction:column;gap:14px"><span class="tag" style="background:var(--tealsoft);color:var(--teal);font-size:17px;align-self:flex-start">Admin console · desktop</span>{admin_dash()}
<div style="font-size:19px;color:var(--ink2);width:880px">Ops sees GMV, escrow and fraud cases, and can freeze a cloned device in one click (audited).</div></div></div>'''
    return slide(33, U4, "Hi-fi prototype · <em>merchant &amp; admin</em>", "The same visual language, scaled from a shop counter to an operations desk.", body,
                 "High-fidelity prototype", "Responsive layout per Material 3 window classes [R19]", "docs/02-system-architecture.md §5")

def s34():
    G = [("hand", "Tap", "Primary actions. 44×44 px minimum"), ("scan-line", "Point &amp; hold", "Scan QR. Haptic pulse on lock-on"), ("arrow-down", "Pull to sync", "Manual sync on Home and history"),
         ("mouse-pointer-click", "Long-press", "Reveal receipt short code"), ("arrow-left", "Swipe back", "Always returns without losing input"), ("vibrate", "Haptics", "Success = 1 short buzz. Error = 2")]
    gc = "".join(f'<div style="display:flex;gap:14px;align-items:center;background:#fff;border:1px solid var(--line);border-radius:16px;padding:14px 16px"><div class="ic" style="margin:0;width:50px;height:50px">{I(ic)}</div><div><b style="font-size:21px">{t}</b><div style="font-size:18px;color:var(--ink2)">{d}</div></div></div>' for ic,t,d in G)
    M = [("Tap feedback", 100, "standard"), ("Bottom sheet in", 240, "enter"), ("Screen transition", 300, "standard"), ("Success tick + chime", 600, "enter"), ("Sync spinner → tick", 400, "standard")]
    mt = "".join(f'<div style="display:grid;grid-template-columns:230px 1fr 80px;gap:14px;align-items:center"><span style="font-size:19px;font-weight:700">{t}</span><div style="height:16px;border-radius:9px;background:rgba(255,255,255,.08)"><div style="width:{ms/6:.0f}%;height:100%;border-radius:9px;background:{"var(--saff)" if ms==600 else "var(--mint)"}"></div></div><span class="mono" style="font-size:17px;color:#fff;text-align:right">{ms}ms</span></div>' for t,ms,e in M)
    body = f'''<div style="display:grid;grid-template-columns:1fr 1fr;gap:26px;height:100%">
<div class="card"><h4>Gestures</h4><div class="grow" style="display:grid;grid-template-columns:1fr 1fr;gap:14px;align-content:space-evenly">{gc}</div></div>
<div style="display:flex;flex-direction:column;gap:22px"><div class="card dark" style="flex:1"><h4>Motion spec (easing: cubic-bezier(.2,0,0,1))</h4><div class="grow" style="gap:14px">{mt}</div></div>
<div class="card tint"><h4>Accessibility considerations</h4><ul style="gap:6px"><li style="font-size:20px">prefers-reduced-motion → fades replace scale and slide</li><li style="font-size:20px">Every gesture has a button alternative (no gesture-only actions)</li><li style="font-size:20px">Success is announced by screen reader, not only shown</li></ul></div></div></div>'''
    return slide(34, U4, "Gestures, motion &amp; <em>accessibility considerations</em>", "Motion confirms and never distracts. Every gesture has an accessible alternative.", body,
                 "Gestures and motion · Accessibility considerations (hi-fi)", "Material Design 3 motion &amp; gestures [R19] · WCAG 2.2 SC 2.5.1, 2.3.3 [R14]", "docs/design/tokens.json → motion")

def s35():
    loop = ""
    import math
    nodes = [("Measure", "chart-line", -90), ("Learn", "lightbulb", 30), ("Build", "hammer", 150)]
    for t, ic, a in nodes:
        x = 230 + 160 * math.cos(math.radians(a)); y = 230 + 160 * math.sin(math.radians(a))
        loop += f'<div style="position:absolute;left:{x-80:.0f}px;top:{y-80:.0f}px;width:160px;height:160px;border-radius:50%;background:{"var(--saff)" if t=="Learn" else "var(--teal)"};color:{"var(--ink)" if t=="Learn" else "#fff"};display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;box-shadow:0 12px 30px rgba(14,27,44,.2)"><i data-lucide="{ic}" style="width:36px;height:36px"></i><b class="disp" style="font-size:26px">{t}</b></div>'
    ring = f'<div style="position:relative;width:460px;height:460px;flex:none"><div style="position:absolute;left:70px;top:70px;width:320px;height:320px;border-radius:50%;border:4px dashed rgba(15,118,110,.35)"></div>{loop}</div>'
    F = [("star", "In-app rating", "1-tap rating after each settled payment"), ("message-circle", "Merchant circle", "Monthly call with 10 pilot shopkeepers"), ("life-buoy", "Support tickets", "Tagged by screen + issue type"), ("chart-column", "Product analytics", "Funnel events: scan → PIN → receipt → sync")]
    fc = "".join(f'<div style="display:flex;gap:14px;align-items:center"><div class="ic" style="margin:0;width:50px;height:50px">{I(ic)}</div><div><b style="font-size:21px">{t}</b><div style="font-size:18px;color:var(--ink2)">{d}</div></div></div>' for ic,t,d in F)
    body = f'''<div style="display:grid;grid-template-columns:500px 1fr 1fr;gap:26px;height:100%;align-items:stretch">
<div class="card" style="align-items:center;justify-content:center">{ring}<div style="font-size:18px;color:var(--ink2);text-align:center">Monthly UX review · quarterly research refresh</div></div>
<div class="card"><h4>Gathering feedback post-launch</h4><div class="grow" style="gap:16px">{fc}</div></div>
<div class="card dark"><h4>Iteration example · analytics → design</h4><div class="grow" style="gap:16px">
<div style="font-size:20px;color:#C9D4E2"><b style="color:var(--saff)">Signal:</b> 14% drop-off on the PIN screen (funnel)</div>
<div style="font-size:20px;color:#C9D4E2"><b style="color:var(--saff)">Insight:</b> recordings showed users re-reading the merchant name</div>
<div style="font-size:20px;color:#C9D4E2"><b style="color:var(--saff)">Change:</b> merchant + amount pinned above the keypad, copy shortened</div>
<div style="font-size:20px;color:#fff"><b style="color:var(--mint)">Outcome:</b> drop-off fell to 6% in the next release</div></div></div></div>'''
    return slide(35, U4, "Maintaining &amp; <em>evolving UX</em>", "After launch, UX becomes a continuous loop of feedback, analytics and small, safe iterations.", body,
                 "Continuous UX improvement · Post-launch feedback · Iterating with analytics", "Torres, Continuous Discovery Habits [R30] · Ries, Lean Startup [R22]", "docs/12-implementation-plan.md Phase 8.5", extra=SAMPLE)

def s36():
    return divider(36, "V", "Product management &amp; analytics", ["Product vision, strategy &amp; the PM role", "PRD, Lean, Scrum &amp; Kanban", "Market analysis, roadmap &amp; MVP", "Product analytics: HEART &amp; AARRR"], "Syllabus Unit V · 8 hours")

def s37():
    pil = [("shield-check", "Trust", "Signed proof · honest states · safe limits"), ("zap", "Speed", "<10 s handshake · 3 taps · auto-sync"), ("users", "Inclusion", "Any phone · offline · Hindi · AA")]
    pc = "".join(f'<div style="flex:1;background:rgba(255,255,255,.06);border-radius:18px;padding:18px;display:flex;flex-direction:column;gap:8px"><i data-lucide="{ic}" style="width:32px;height:32px;color:var(--saff)"></i><b class="disp" style="font-size:26px;color:#fff">{t}</b><span style="font-size:18px;color:#C9D4E2">{d}</span></div>' for ic,t,d in pil)
    day = [("09:30", "Stand-up with dev team (Gemini agent + reviewer)"), ("11:00", "Merchant interview / usability session"), ("14:00", "Write and refine PRD stories, prioritise backlog"), ("16:00", "Review metrics dashboard (HEART)"), ("17:30", "Align design, engineering and ops on next release")]
    dl = "".join(f'<div style="display:grid;grid-template-columns:76px 1fr;gap:12px;align-items:center"><span class="mono" style="font-size:17px;color:var(--teal);font-weight:700">{t}</span><span style="font-size:19px;color:var(--ink2)">{d}</span></div>' for t,d in day)
    pd = ["Ideate", "Research", "Define (PRD)", "Design", "Build", "Test", "Launch", "Iterate"]
    pdl = "".join(f'<div style="flex:1;text-align:center;padding:12px 6px;border-radius:12px;background:{"var(--teal)" if i<4 else "var(--saffsoft)"};color:{"#fff" if i<4 else "#8A5300"};font-weight:800;font-size:16px">{x}</div>' for i,x in enumerate(pd))
    body = f'''<div style="display:grid;grid-template-columns:1.2fr 1fr;gap:26px;height:100%">
<div style="display:flex;flex-direction:column;gap:22px"><div class="card dark" style="flex:1;justify-content:center"><h4>Product vision</h4><p class="disp" style="font-size:36px;color:#fff;font-weight:600;line-height:1.2">"Pay and get paid anywhere, <span style="color:var(--saff)">even when the network isn't there.</span>"</p>
<h4 style="margin-top:18px">Strategy pillars</h4><div style="display:flex;gap:14px">{pc}</div></div>
<div class="card" style="padding:20px 26px"><h4>Product development lifecycle · where Hey Payments is now</h4><div style="display:flex;gap:8px">{pdl}</div></div></div>
<div class="card"><h4>The PM role · a day in the life</h4><div class="grow" style="gap:12px">{dl}</div>
<div style="background:var(--tealsoft);border-radius:14px;padding:14px 18px;font-size:19px;color:var(--ink2);margin-top:12px"><b>Understanding product:</b> the thing users hire to solve a job, here "move money with confidence at zero signal".</div></div></div>'''
    return slide(37, U5, "Product <em>management</em>", "Vision, strategy, the PM's responsibilities and the lifecycle, all for Hey Payments.", body,
                 "Understanding Product · PM role &amp; responsibilities · PDLC · Vision &amp; Strategy", "Cagan, Inspired [R25]", "docs/01-PRD.md §1 Vision")

def s38():
    cols = [("To do", ["7.2 Admin fraud workflow", "8.4 Accessibility audit"], "var(--paper2)"), ("In progress", ["6.2 Sync API (S1–S7)", "6.6 Sync engine (PWA)"], "var(--saffsoft)"),
            ("Review", ["5.4 Merchant POS", "5.5 Payer scan-pay"], "var(--offsoft)"), ("Done", ["1.x Auth &amp; devices", "2.x Ledger core", "3.x Offline load"], "var(--oksoft)")]
    kb = "".join(f'<div style="background:{bg};border-radius:16px;padding:12px;display:flex;flex-direction:column;gap:8px"><div style="font-size:14px;font-weight:800;letter-spacing:.1em;text-transform:uppercase;color:var(--ink2)">{t}</div>' + "".join(f'<div style="background:#fff;border-radius:10px;padding:10px 12px;font-size:16px;font-weight:600;box-shadow:0 2px 6px rgba(14,27,44,.06)">{c}</div>' for c in cards) + '</div>' for t,cards,bg in cols)
    body = f'''<div style="display:grid;grid-template-columns:1fr 1fr 1.25fr;gap:24px;height:100%">
<div class="card"><h4>PRD snapshot</h4><div class="grow" style="gap:10px;font-size:19px;color:var(--ink2)">
<div><b style="color:var(--ink)">Goals:</b> offline load, offline pay, exactly-once settlement</div><div><b style="color:var(--ink)">Non-goals v1:</b> NFC, refunds, real banks</div>
<div><b style="color:var(--ink)">13 FRs</b> (FR-1…FR-13), MoSCoW prioritised</div><div><b style="color:var(--ink)">NFRs:</b> &lt;10 s, WCAG AA, ASVS L2</div><div><b style="color:var(--ink)">Metrics:</b> HEART + AARRR</div></div>
<div class="mono" style="font-size:15px;color:var(--teal);margin-top:10px">docs/01-PRD.md</div></div>
<div style="display:flex;flex-direction:column;gap:22px"><div class="card tint" style="flex:1;justify-content:center"><h4>Lean</h4><p style="font-size:24px">Build → measure → learn. The sprint prototype was the first MVP experiment before any code.</p></div>
<div class="card" style="flex:1;justify-content:center"><h4>Scrum</h4><p style="font-size:24px">2-week sprints = plan phases 0–8. Review = architect review gate. Retro after each phase.</p></div></div>
<div class="card"><h4>Kanban board · live build</h4><div style="display:grid;grid-template-columns:repeat(4,1fr);gap:10px;flex:1">{kb}</div>
<p style="font-size:18px;margin-top:12px">WIP limit 2 per column. Tasks map to <span class="mono">docs/12-implementation-plan.md</span>.</p></div></div>'''
    return slide(38, U5, "PRD, Lean &amp; <em>Agile delivery</em>", "How the product is specified and delivered: one PRD, Lean thinking, Scrum cadence and a Kanban board.", body,
                 "PRD · Lean product development · Agile · Scrum · Kanban", "Ries [R22] · Scrum Guide [R23] · Anderson, Kanban [R24]", "docs/01-PRD.md · docs/12-implementation-plan.md")

def s39():
    seg = [("Small merchants in low-signal areas", "Kirana, chai, xerox, vegetable stalls", 90), ("Daily commuters", "Metro and suburban rail users", 70), ("Campuses &amp; hostels", "Canteens, stationery shops", 55)]
    sg = "".join(f'<div><div style="display:flex;justify-content:space-between;font-size:20px;font-weight:700"><span>{t}</span><span style="color:var(--teal)">{"High" if v>80 else ("Med-high" if v>60 else "Medium")}</span></div><div style="font-size:17px;color:var(--ink2);margin:2px 0 8px">{d}</div><div style="height:12px;border-radius:7px;background:var(--paper2)"><div style="width:{v}%;height:100%;border-radius:7px;background:var(--teal2)"></div></div></div>' for t,d,v in seg)
    rm = [("MVP", "Phases 0–6", "Load · pay offline · sync &amp; settle · fraud basics", "var(--saff)", "var(--ink)"), ("v1.0", "Phases 7–8", "Merchant dashboard · admin console · AA audit", "var(--teal)", "#fff"), ("Later", "2027", "NFC/BLE transport · refunds · native wrappers", "var(--ink)", "#fff")]
    rmc = "".join(f'<div style="flex:1;background:{bg};color:{fg};border-radius:18px;padding:18px 20px;display:flex;flex-direction:column;gap:6px"><div style="display:flex;justify-content:space-between;align-items:baseline"><b class="disp" style="font-size:32px">{t}</b><span style="font-size:15px;font-weight:800;opacity:.8">{w}</span></div><span style="font-size:18px;line-height:1.35">{d}</span></div>' for t,w,d,bg,fg in rm)
    body = f'''<div style="display:grid;grid-template-columns:1fr 1.3fr;gap:26px;height:100%">
<div class="card"><h4>Market analysis · target segments</h4><div class="grow" style="gap:16px">{sg}</div>
<div style="font-size:18px;color:var(--ink2);background:var(--saffsoft);border-radius:14px;padding:12px 16px;margin-top:10px"><b>Why now:</b> RBI's 2022 offline framework allows offline small-value payments. Competitors need NFC.</div></div>
<div style="display:flex;flex-direction:column;gap:22px"><div class="card" style="flex:1"><h4>Roadmap</h4><div style="display:flex;gap:14px;flex:1;align-items:stretch">{rmc}</div></div>
<div class="card dark"><h4>Minimum Viable Product</h4><p style="font-size:21px;color:#fff">The smallest <span style="color:var(--saff)">trustworthy</span> product: one payer can load, pay one merchant offline, and both see it settle. No dashboard and no refunds until that loop works.</p></div></div></div>'''
    return slide(39, U5, "Market, roadmap &amp; <em>MVP</em>", "Who we serve first, what ships when, and the smallest product that proves the idea.", body,
                 "Competitive and Market Analysis · Roadmapping · Minimum Viable Product", "Cagan, Inspired [R25] · RBI framework [R26]", "docs/01-PRD.md §9 · docs/12 phases")

def s40():
    H = [("Happiness", "Users trust offline pay", "Post-payment rating", "CSAT ≥ 4.3 · SUS ≥ 75"), ("Engagement", "Becomes a habit", "Offline pays / user / week", "≥ 8 per week"),
         ("Adoption", "New users try offline", "First offline load", "40% within 7 days"), ("Retention", "Users come back", "Weekly active payers", "W4 ≥ 35%"), ("Task success", "Payments complete", "Handshake completion", "≥ 97%, median ≤ 8 s")]
    hr = "".join(f'<tr><td style="padding:11px 10px;border-top:1px solid var(--line)"><b class="disp" style="font-size:25px;color:var(--teal)">{a}</b></td><td style="padding:11px 10px;border-top:1px solid var(--line);font-size:20px">{b}</td><td style="padding:11px 10px;border-top:1px solid var(--line);font-size:20px;color:var(--ink2)">{c}</td><td style="padding:11px 10px;border-top:1px solid var(--line);font-size:20px;font-weight:800">{d}</td></tr>' for a,b,c,d in H)
    F = [("Acquisition", "Sign-ups by channel (merchant QR referral, campus)", 100), ("Activation", "First offline payment within 48 h", 84), ("Retention", "D7 / D30 returning payers &amp; merchants", 68), ("Referral", "Merchant invites converted", 52), ("Revenue", "0.3% MDR on settled GMV", 38)]
    fn = "".join(f'<div style="width:{w}%;margin:0 auto;background:{["#0F766E","#14B8A6","#2DD4BF","#F5A524","#B45309"][i]};color:{"#fff" if i in (0,1,4) else "var(--ink)"};border-radius:12px;padding:10px 16px;display:flex;justify-content:space-between;align-items:center;gap:10px"><b style="font-size:19px">{t}</b><span style="font-size:15px;text-align:right">{d}</span></div>' for i,(t,d,w) in enumerate(F))
    body = f'''<div style="display:grid;grid-template-columns:1.25fr 1fr;gap:26px;height:100%">
<div class="card"><h4>HEART framework (Google) · goals → signals → metrics</h4><table style="flex:1"><tr style="font-size:14px;font-weight:800;letter-spacing:.1em;color:var(--muted);text-transform:uppercase"><td style="padding:6px 10px"></td><td style="padding:6px 10px">Goal</td><td style="padding:6px 10px">Signal</td><td style="padding:6px 10px">Metric / target</td></tr>{hr}</table></div>
<div class="card dark"><h4>AARRR · pirate funnel</h4><div class="grow" style="gap:10px">{fn}</div>
<p style="font-size:17px;margin-top:10px">Instrumented events: <span class="mono" style="color:var(--saff);font-size:15px">offline_load · pr_scanned · pin_ok · voucher_shown · receipt_ok · synced</span></p></div></div>'''
    return slide(40, U5, "Product <em>analytics</em>", "Acquisition, activation, engagement, retention and revenue metrics, organised with HEART and AARRR.", body,
                 "Acquisition · Activation · Engagement · Retention · Revenue metrics · HEART · AARRR", "Rodden et al., HEART (CHI 2010) [R20] · McClure, AARRR [R21]", "docs/01-PRD.md §8")

REFS = [
 "Norman, D. (2013). <i>The Design of Everyday Things</i> (rev. ed.). Basic Books.",
 "Nielsen, J. (1994, upd. 2024). 10 Usability Heuristics for User Interface Design. Nielsen Norman Group.",
 "Rams, D. Ten Principles for Good Design. Vitsœ.",
 "Morville, P. (2004). User Experience Design: the UX Honeycomb. Semantic Studios.",
 "Knapp, J., Zeratsky, J., &amp; Kowitz, B. (2016). <i>Sprint</i>. Simon &amp; Schuster.",
 "Design Council (2005, 2019). The Double Diamond / Framework for Innovation.",
 "Rohrer, C. (2022). When to Use Which User-Experience Research Methods. NN/g.",
 "Google (2021–). Google UX Design Professional Certificate. Coursera.",
 "Cooper, A. (1999). <i>The Inmates Are Running the Asylum</i>. Sams.",
 "Gray, D. (2017). Updated Empathy Map Canvas. XPLANE / Gamestorming.",
 "Gothelf, J., &amp; Seiden, J. (2021). <i>Lean UX</i> (3rd ed.). O'Reilly.",
 "Kalbach, J. (2020). <i>Mapping Experiences</i> (2nd ed.). O'Reilly.",
 "Brignull, H. (2023). <i>Deceptive Patterns</i>. deceptive.design.",
 "W3C (2023). Web Content Accessibility Guidelines (WCAG) 2.2. W3C Recommendation.",
 "U.S. Department of Justice. Americans with Disabilities Act (1990); ADA web guidance (ada.gov).",
 "Brooke, J. (1996). SUS: A 'quick and dirty' usability scale. In <i>Usability Evaluation in Industry</i>.",
 "Nielsen, J. (2000). Why You Only Need to Test with 5 Users. NN/g.",
 "Kohavi, R., Tang, D., &amp; Xu, Y. (2020). <i>Trustworthy Online Controlled Experiments</i>. Cambridge UP.",
 "Google (2024). Material Design 3: Motion, Gestures, Layout. m3.material.io.",
 "Rodden, K., Hutchinson, H., &amp; Fu, X. (2010). Measuring the User Experience on a Large Scale. CHI '10.",
 "McClure, D. (2007). Startup Metrics for Pirates: AARRR! 500 Startups.",
 "Ries, E. (2011). <i>The Lean Startup</i>. Crown Business.",
 "Schwaber, K., &amp; Sutherland, J. (2020). <i>The Scrum Guide</i>.",
 "Anderson, D. J. (2010). <i>Kanban</i>. Blue Hole Press.",
 "Cagan, M. (2017). <i>Inspired</i> (2nd ed.). Wiley.",
 "Reserve Bank of India (2022, Jan 3). Framework for Facilitating Small Value Digital Payments in Offline Mode.",
 "NPCI (2022–). UPI Lite and UPI Lite X product documentation. npci.org.in.",
 "Lidwell, W., Holden, K., &amp; Butler, J. (2010). <i>Universal Principles of Design</i>. Rockport.",
 "Itten, J. (1961). <i>The Art of Color</i>. Reinhold.",
 "Torres, T. (2021). <i>Continuous Discovery Habits</i>. Product Talk.",
 "IDEO.org (2015). <i>The Field Guide to Human-Centered Design</i>.",
]

def s41():
    half = 16
    def col(a, b):
        return "".join(f'<div style="display:grid;grid-template-columns:62px 1fr;gap:8px;font-size:16.5px;line-height:1.3;padding:4px 0;border-bottom:1px solid var(--line)"><span class="mono" style="color:var(--teal);font-weight:700">[R{i+1}]</span><span style="color:var(--ink2)">{REFS[i]}</span></div>' for i in range(a, b))
    body = f'<div class="g2" style="gap:44px"><div class="card" style="padding:20px 28px;justify-content:space-between">{col(0,half)}</div><div class="card" style="padding:20px 28px;justify-content:space-between">{col(half,len(REFS))}<div style="font-size:17px;color:var(--teal);font-weight:700;padding-top:8px">Project artefacts: hey_payments repository · docs/00–12, openapi.yaml, tokens.json</div></div></div>'
    return slide(41, "References", "<em>References</em>", "Every method on the slides is cited in the ribbon as [R#]. Full sources below.", body,
                 "All units", "APA-style bibliography", "docs/00-README.md")

def s42():
    return f'''<section class="slide dark" data-n="42">
<div style="position:absolute;inset:0;display:grid;grid-template-columns:1fr 1fr;align-items:center;padding:0 110px;z-index:2">
<div><div class="kicker" style="color:var(--mint)">Thank you</div>
<h1 class="disp" style="font-size:104px;line-height:1;font-weight:700;letter-spacing:-.03em;margin-top:22px">Design for trust<br>when certainty<br>is <span style="color:var(--saff)">scarce.</span></h1>
<div style="display:flex;flex-direction:column;gap:14px;margin-top:40px;font-size:24px;color:#C9D4E2">
<div style="display:flex;gap:14px;align-items:center"><i data-lucide="check" style="color:var(--mint)"></i>Research-driven decisions, cited at every step</div>
<div style="display:flex;gap:14px;align-items:center"><i data-lucide="check" style="color:var(--mint)"></i>Accessibility and honesty built in from day one</div>
<div style="display:flex;gap:14px;align-items:center"><i data-lucide="check" style="color:var(--mint)"></i>One status language: Offline · Pending sync · Settled</div></div>
<div style="display:flex;gap:14px;margin-top:40px"><span class="pill p-off">Offline</span><span class="pill p-pend">Pending sync</span><span class="pill p-ok">Settled</span></div>
<div class="disp" style="font-size:40px;font-weight:700;margin-top:48px;color:var(--mint)">Questions?</div></div>
<div style="display:flex;justify-content:center;align-items:center;gap:30px"><div style="transform:rotate(-6deg) translateY(30px)">{phone(scr_receipt())}</div><div style="transform:rotate(6deg) translateY(-20px)">{phone(scr_merchant_today())}</div></div></div></section>'''
