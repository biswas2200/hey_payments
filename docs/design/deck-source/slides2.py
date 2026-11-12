from parts import *
from slides1 import divider, avatar, U2

U3 = "Unit III · Visual Design, Accessibility & Testing"

def s15():
    Q = [("Says", "message-square-quote", ["\"Show me the message first.\"", "\"Network is always bad in this lane.\"", "\"Customers say it's paid, but is it?\""], "o"),
         ("Thinks", "brain", ["Is this screenshot fake?", "Will I lose this sale?", "Do I have to trust the customer?"], ""),
         ("Does", "footprints", ["Waits for SMS before handing goods", "Asks customers to step outside for signal", "Keeps a cash-and-UPI notebook"], "s"),
         ("Feels", "heart", ["Anxious at peak hours", "Embarrassed to doubt regulars", "Relieved by a loud confirmation"], "r")]
    def q(t, ic, items, k):
        return f'''<div class="card" style="padding:24px 28px"><div class="row" style="align-items:center;gap:14px;margin-bottom:6px"><div class="ic {k}" style="margin:0">{I(ic)}</div><h3 style="margin:0">{t}</h3></div><ul>{"".join(f"<li>{x}</li>" for x in items)}</ul></div>'''
    body = f'''<div style="display:grid;grid-template-columns:1fr 330px 1fr;grid-template-rows:1fr 1fr;gap:22px;height:100%">
{q(*Q[0])}
<div class="card dark" style="grid-row:span 2;align-items:center;justify-content:center;text-align:center;gap:16px">{avatar("R","#F5A524","#B45309",130)}<div class="disp" style="font-size:32px;font-weight:700;color:#fff">Ravi</div><div style="font-size:18px;color:#8DA0B8">at 8 pm rush, one bar of signal</div>
<div style="width:100%;border-top:1px solid rgba(255,255,255,.12);padding-top:16px;text-align:left"><h4>Pains</h4><p style="font-size:19px">Uncertain proof · lost sales · no records</p><h4 style="margin-top:14px">Gains</h4><p style="font-size:19px">Instant verified receipt · end-of-day total</p></div></div>
{q(*Q[1])}{q(*Q[2])}{q(*Q[3])}</div>'''
    return slide(15, U2, "Empathy <em>map</em>", "What Ravi says, thinks, does and feels at the counter, from interviews and a market visit.", body,
                 "Empathy Maps", "Gray, Empathy Map Canvas (XPLANE) [R10]", "docs/01-PRD.md §4 · §10 Risks")

def s16():
    P = [("Payment uncertainty", 92), ("No network at point of sale", 85), ("No proof for merchant", 78), ("Slow checkout, queue pressure", 64), ("Language barrier (English-only apps)", 41)]
    bars = "".join(f'''<div><div style="display:flex;justify-content:space-between;font-size:21px;font-weight:700;margin-bottom:8px"><span>{i+1}. {t}</span><span style="color:var(--muted)">{v}%</span></div>
<div style="height:16px;border-radius:9px;background:var(--paper2)"><div style="width:{v}%;height:100%;border-radius:9px;background:{"var(--bad)" if i<2 else ("var(--saff)" if i<4 else "var(--teal2)")}"></div></div></div>''' for i,(t,v) in enumerate(P))
    S = [("Merchant", "As a shopkeeper, I want to accept a payment without internet <b>so that</b> I never lose a sale to bad signal.", "FR-5, FR-6"),
         ("Payer", "As a commuter, I want to pay from a preloaded balance <b>so that</b> I can pass the queue without waiting for signal.", "FR-4, FR-6"),
         ("Payer", "As a student, I want to see what is pending vs settled <b>so that</b> I trust my balance.", "FR-11"),
         ("Merchant", "As a shopkeeper, I want an end-of-day total <b>so that</b> I can stop keeping a notebook.", "FR-9")]
    stories = "".join(f'''<div style="background:#fff;border-radius:18px;padding:16px 20px;border:1px solid var(--line);display:grid;grid-template-columns:120px 1fr 110px;gap:16px;align-items:center">
<span class="tag" style="background:{"var(--saffsoft)" if r=="Merchant" else "var(--offsoft)"};color:{"#8A5300" if r=="Merchant" else "var(--off)"};justify-content:center">{r}</span><span style="font-size:20px;line-height:1.38;color:var(--ink2)">{t}</span><span class="mono" style="font-size:15px;color:var(--teal);text-align:right;font-weight:700">{f}</span></div>''' for r,t,f in S)
    body = f'''<div style="display:grid;grid-template-columns:.85fr 1.25fr;gap:26px;height:100%">
<div class="card"><h4>Pain points · share of participants mentioning</h4><div class="grow" style="gap:18px">{bars}</div></div>
<div class="card tint"><h4>User stories (traced to PRD requirements)</h4><div class="grow" style="gap:12px">{stories}</div>
<div style="margin-top:14px;font-size:19px;color:var(--ink2)"><b>Scenario:</b> Priya buys chai in a metro basement with zero bars. She scans, enters her PIN and shows her code. Ravi's phone chimes "₹40 received". It syncs later on the platform.</div></div></div>'''
    return slide(16, U2, "Pain points, user stories <em>&amp; scenarios</em>", "We ranked the pains, then wrote stories in the 'As a… I want… so that…' format, each linked to a requirement.", body,
                 "Identifying user pain points · Creating User Stories and Scenarios", "Google UX Certificate, user stories [R8]", "docs/01-PRD.md §5 FR-1…FR-13", extra=SAMPLE)

def s17():
    body = f'''<div class="g2">
<div class="card" style="padding:40px 44px"><div class="row" style="align-items:center;gap:16px"><div class="ic r" style="margin:0">{I("circle-help")}</div><div><h4 style="margin:0">Problem statement</h4><div style="font-size:18px;color:var(--muted)">User + need + insight</div></div></div>
<div class="grow"><p class="disp" style="font-size:40px;line-height:1.25;color:var(--ink);font-weight:600">"<span style="color:var(--teal)">Ravi</span>, a kirana owner in a low-signal market, <span style="background:var(--saffsoft);padding:0 6px;border-radius:6px">needs a way to accept digital payments without internet</span> because failed payments cost him customers and trust."</p></div>
<div class="row" style="gap:12px;flex-wrap:wrap"><span class="tag" style="background:var(--tealsoft);color:var(--teal)">User: Ravi</span><span class="tag" style="background:var(--saffsoft);color:#8A5300">Need: offline acceptance</span><span class="tag" style="background:var(--badsoft);color:var(--bad)">Insight: lost sales &amp; trust</span></div></div>
<div class="card dark" style="padding:40px 44px"><div class="row" style="align-items:center;gap:16px"><div class="ic s" style="margin:0">{I("flask-conical")}</div><div><h4 style="margin:0">Hypothesis statement</h4><div style="font-size:18px;color:#8DA0B8">We believe · will result in · we'll know when</div></div></div>
<div class="grow"><p class="disp" style="font-size:36px;line-height:1.28;color:#fff;font-weight:600">"We believe that a <span style="color:var(--mint)">signed offline receipt with honest status labels</span> for small merchants will result in more completed payments at zero signal. We'll know we're right when <span style="color:var(--saff)">first-attempt completion reaches 97%</span> and retries drop by half."</p></div>
<div class="row" style="gap:12px;flex-wrap:wrap"><span class="tag" style="background:rgba(45,212,191,.15);color:var(--mint)">Measured in usability test</span><span class="tag" style="background:rgba(245,165,36,.15);color:var(--saff)">Tracked post-launch (HEART)</span></div></div></div>'''
    return slide(17, U2, "Problem &amp; <em>hypothesis statements</em>", "Research turned into one problem to solve and one testable bet.", body,
                 "Problem statements · Hypothesis statements", "Google UX Certificate [R8] · Gothelf, Lean UX [R11]", "docs/01-PRD.md §2 · §8 Metrics")

def s18():
    sk = ["Tap to pay", "QR ↔ QR", "Sound code", "Bluetooth", "Voucher card", "Receipt QR", "SMS fallback", "Shop keypad"]
    sketches = "".join(f'''<div style="background:#fff;border:2px dashed {"var(--teal)" if x in ("QR ↔ QR","Receipt QR") else "#B8B2A6"};border-radius:14px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:6px;transform:rotate({[-2,1.5,-1,2,1,-1.5,2,-2][i]}deg)">
<div style="width:56px;height:40px;border:2.5px solid #5B6576;border-radius:8px"></div><span style="font-family:Caveat;font-size:25px;color:var(--ink2)">{x}</span>{"<span class='tag' style='background:var(--teal);color:#fff;font-size:12px;padding:3px 9px'>chosen</span>" if x in ("QR ↔ QR","Receipt QR") else ""}</div>''' for i,x in enumerate(sk))
    R = [("FR-4", "Offline cap ₹2,000", "Balance card with progress bar + 'valid 3 days'", "hp-balance-card"),
         ("FR-6", "₹500 per payment", "Limit shown on keypad and blocked before PIN", "hp-amount-input"),
         ("FR-11", "Trustworthy status", "Indigo/amber/green pill, always icon + text", "hp-status-pill"),
         ("FR-6", "Payer authorisation", "6-digit PIN pad, 44px keys, biometric if available", "hp-keypad"),
         ("FR-6", "Merchant proof", "Receipt QR + short code read aloud", "hp-qr-display")]
    rows = "".join(f'''<tr><td style="padding:13px 10px;border-top:1px solid var(--line)"><span class="mono" style="color:var(--teal);font-weight:700;font-size:16px">{a}</span></td><td style="padding:13px 10px;border-top:1px solid var(--line);font-weight:700;font-size:20px">{b}</td><td style="padding:13px 10px;border-top:1px solid var(--line);font-size:19px;color:var(--ink2)">{c}</td><td style="padding:13px 10px;border-top:1px solid var(--line)"><span class="mono" style="font-size:15px;background:var(--paper2);padding:4px 8px;border-radius:6px">{d}</span></td></tr>''' for a,b,c,d in R)
    body = f'''<div style="display:grid;grid-template-columns:.9fr 1.3fr;gap:26px;height:100%">
<div class="card"><h4>How might we…?</h4><p style="font-size:22px;font-weight:600;color:var(--ink)">…make offline feel as trustworthy as online?<br>…give the merchant proof without a server?</p>
<h4 style="margin-top:18px">Crazy 8s · 8 ideas in 8 minutes</h4><div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px;flex:1;margin-top:6px">{sketches}</div></div>
<div class="card"><h4>Translating requirements into design specifications</h4><table><tr style="font-size:14px;font-weight:800;letter-spacing:.1em;color:var(--muted);text-transform:uppercase"><td style="padding:8px 10px">Req</td><td style="padding:8px 10px">Requirement</td><td style="padding:8px 10px">Design spec</td><td style="padding:8px 10px">Component</td></tr>{rows}</table>
<div style="margin-top:auto;font-size:19px;color:var(--ink2);background:var(--tealsoft);border-radius:14px;padding:14px 18px">Specs live in <span class="mono">docs/design/tokens.json</span> so the Angular build uses the same values as the design.</div></div></div>'''
    return slide(18, U2, "Design ideation &amp; <em>design specifications</em>", "We diverged with HMW questions and Crazy 8s, then converged on requirements and buildable specs.", body,
                 "Design ideation · Translating Requirements into Design Specifications", "IDEO HCD Field Guide (HMW) [R31] · Sprint, Crazy 8s [R5]", "docs/01-PRD.md §5 · docs/design/tokens.json")

def s19():
    cols = ["Hey Payments", "UPI Lite X", "Google Pay", "PhonePe", "Paytm", "Cash"]
    rows = [("Pay with no payer internet", ["y","y","n","n","n","y"]),
            ("Works on phones without NFC", ["y","n","y","y","y","y"]),
            ("Merchant gets offline proof", ["y","p","n","n","n","y"]),
            ("Clear pending vs settled status", ["y","p","p","p","p","n"]),
            ("Hindi / regional UI", ["y","y","y","y","y","–"]),
            ("Automatic digital records", ["y","y","y","y","y","n"])]
    mk = {"y": '<span style="display:inline-grid;place-items:center;width:38px;height:38px;border-radius:50%;background:var(--oksoft);color:var(--ok)"><i data-lucide="check" style="width:22px;height:22px;stroke-width:3"></i></span>',
          "p": '<span style="display:inline-grid;place-items:center;width:38px;height:38px;border-radius:50%;background:var(--pendsoft);color:var(--pend)"><i data-lucide="minus" style="width:22px;height:22px;stroke-width:3"></i></span>',
          "n": '<span style="display:inline-grid;place-items:center;width:38px;height:38px;border-radius:50%;background:var(--badsoft);color:var(--bad)"><i data-lucide="x" style="width:22px;height:22px;stroke-width:3"></i></span>',
          "–": '<span style="color:var(--muted);font-size:22px">n/a</span>'}
    head_ = "".join(f'<th style="padding:16px 10px;font-size:20px;{"background:var(--ink);color:#fff;border-radius:18px 18px 0 0" if i==0 else "color:var(--ink2)"}">{c}</th>' for i,c in enumerate(cols))
    body_ = "".join(f'<tr><td style="padding:14px 16px;font-size:21px;font-weight:700;border-top:1px solid var(--line)">{r}</td>' + "".join(f'<td style="text-align:center;border-top:1px solid var(--line);{"background:rgba(15,118,110,.07)" if i==0 else ""}">{mk[v]}</td>' for i,v in enumerate(vals)) + '</tr>' for r,vals in rows)
    body = f'''<div style="display:grid;grid-template-columns:1.6fr .7fr;gap:26px;height:100%">
<div class="card" style="padding:22px 26px"><table style="height:100%"><tr><th></th>{head_}</tr>{body_}</table></div>
<div style="display:flex;flex-direction:column;gap:20px"><div class="card dark" style="flex:1;justify-content:center"><h4>Opportunity gap</h4><p style="font-size:27px;color:#fff;font-weight:600;line-height:1.35">No app gives the <span style="color:var(--saff)">merchant</span> offline proof on <span style="color:var(--mint)">any phone</span>. That is our position.</p></div>
<div class="card" style="flex:1;justify-content:center"><h4>Audit method</h4><p style="font-size:22px">Desk research and hands-on use of each app (Nov 2026). Scored on the 6 criteria that came out of our research insights.</p></div>
<div class="row" style="gap:14px;font-size:17px;color:var(--ink2);align-items:center">{mk["y"]} Yes {mk["p"]} Partial {mk["n"]} No</div></div></div>'''
    return slide(19, U2, "Competitive <em>audit</em>", "Five alternatives compared on the criteria our users care about.", body,
                 "Competitive audits", "Google UX Certificate competitive audit template [R8] · NPCI UPI Lite [R27]", "docs/01-PRD.md §3 · §9")

def s20():
    st = ["Load at home", "Enter metro", "Scan merchant QR", "Confirm with PIN", "Show voucher", "Get receipt", "Back online", "Settled"]
    em = [60, 40, 30, 45, 55, 85, 80, 95]
    W = 1720; step = W / len(st)
    pts = " ".join(f"{step*i+step/2:.0f},{145-e*1.1:.0f}" for i,e in enumerate(em))
    dots = "".join(f'<circle cx="{step*i+step/2:.0f}" cy="{145-e*1.1:.0f}" r="10" fill="{"#B91C1C" if e<40 else ("#F5A524" if e<60 else "#15803D")}" stroke="#fff" stroke-width="3"/>' for i,e in enumerate(em))
    faces = "".join(f'<text x="{step*i+step/2:.0f}" y="{145-e*1.1-18:.0f}" text-anchor="middle" font-size="15" font-weight="700" fill="#334155" font-family="Inter">{["calm","unsure","anxious","focused","hopeful","relieved","confident","happy"][i]}</text>' for i,e in enumerate(em))
    header = "".join(f'<div style="background:{"var(--teal)" if i<6 else "var(--ink)"};color:#fff;border-radius:14px;padding:12px;text-align:center;font-weight:800;font-size:18px">{s}</div>' for i,s in enumerate(st))
    def r(label, items, bg="#fff"):
        return f'<div style="display:grid;grid-template-columns:150px repeat(8,1fr);gap:10px;flex:1"><div style="font-size:15px;font-weight:800;letter-spacing:.1em;color:var(--teal);align-self:center">{label}</div>' + "".join(f'<div style="background:{bg};border-radius:12px;padding:10px 12px;display:flex;align-items:center;font-size:18px;line-height:1.3;color:var(--ink2);border:1px solid var(--line)">{x}</div>' for x in items) + '</div>'
    body = f'''<div style="display:flex;flex-direction:column;gap:10px;height:100%">
<div style="display:grid;grid-template-columns:150px repeat(8,1fr);gap:10px"><div></div>{header}</div>
{r("DOING", ["Loads ₹500 offline","Signal drops to 0","Points camera at QR","Enters 6-digit PIN","Holds phone to merchant","Scans receipt QR","Train exits tunnel","Gets notification"])}
{r("THINKING", ["'Is this safe?'","'Will it still work?'","'Is this the right shop?'","'Did I type it right?'","'Can he scan it?'","'Done, I have proof'","'Will it sync?'","'Good, it's settled'"])}
<div style="display:grid;grid-template-columns:150px 1fr;gap:10px;align-items:center"><div style="font-size:15px;font-weight:800;letter-spacing:.1em;color:var(--teal)">FEELING</div>
<svg viewBox="0 0 {W} 160" style="width:100%;height:150px;background:#fff;border-radius:14px;border:1px solid var(--line)"><polyline points="{pts}" fill="none" stroke="#0F766E" stroke-width="4" stroke-linejoin="round"/>{dots}{faces}</svg></div>
{r("PAIN", ["Unsure what 'offline' means","No bars, spinner fear","Low light, glare","Queue pressure","Screen brightness","—","Forgets to open app","—"], "var(--badsoft)")}
{r("OPPORTUNITY", ["Education card + cap","Offline banner, no spinner","Torch + verified badge","Big keys, haptics","Auto max brightness","Chime + short code","Background sync","Settled push + receipt"], "var(--tealsoft)")}</div>'''
    return slide(20, U2, "User journey <em>map</em> · Priya", "From loading money at home to the 'Settled' notification: actions, thoughts, emotions, pains and opportunities.", body,
                 "Personas and User Journeys", "Kalbach, Mapping Experiences [R12] · NN/g journey mapping [R7]", "docs/07-sequence-and-state.md")

def s21():
    P = [("Feedback", "zap", "Chime, haptic and green tick the moment the voucher verifies"),
         ("Affordance &amp; signifiers", "scan-line", "Scan frame plus 'Point at the merchant's QR' label"),
         ("Constraints", "lock", "PIN pad disabled after 5 tries. Amount above ₹500 blocked"),
         ("Mapping", "arrow-left-right", "Your QR goes to the merchant, theirs comes to you: an obvious two-way exchange"),
         ("Consistency", "copy", "Same status pill and colours on payer, merchant and admin"),
         ("Forgiveness", "undo-2", "Void an unreceipted voucher. Cancel on every step")]
    flow = ["Merchant enters ₹", "Shows request QR", "Payer scans + verifies", "PIN confirm", "Voucher QR", "Merchant verifies", "Receipt QR"]
    fl = "".join(f'<div style="flex:1;background:{"var(--saff)" if i in (3,6) else "#fff"};border:1.5px solid {"var(--saff)" if i in (3,6) else "var(--line)"};border-radius:14px;padding:12px;text-align:center;font-size:17px;font-weight:700">{x}</div>' + (f'<div style="color:var(--teal);display:grid;place-items:center">{I("chevron-right")}</div>' if i<6 else "") for i,x in enumerate(flow))
    cards = "".join(f'<div class="card" style="padding:22px 26px;flex-direction:row;gap:18px;align-items:center"><div class="ic" style="margin:0;width:64px;height:64px">{I(ic)}</div><div><h3 style="font-size:29px;margin-bottom:8px">{t}</h3><p style="font-size:22.5px">{d}</p></div></div>' for t,ic,d in P)
    body = f'''<div style="display:flex;flex-direction:column;gap:22px;height:100%"><div style="display:grid;grid-template-columns:repeat(3,1fr);grid-template-rows:1fr 1fr;gap:20px;flex:1">{cards}</div>
<div class="card dark" style="padding:20px 24px"><h4>Core interaction flow · offline handshake (micro-interactions on the saffron steps)</h4><div style="display:flex;gap:6px;align-items:stretch;color:var(--ink)">{fl}</div></div></div>'''
    return slide(21, U2, "Interaction <em>design</em>", "Norman's interaction principles applied to the handshake. Techniques: task analysis, flow diagrams and state machines.", body,
                 "Principles of Interaction Design · Designing User Interactions and Flows · Techniques", "Norman [R1] · Nielsen heuristics [R2]", "docs/07-sequence-and-state.md (state machines)")

def s22():
    def node(t, kind="box"):
        if kind == "dec":
            return f'<div style="width:150px;height:150px;transform:rotate(45deg);background:var(--saffsoft);border:2px solid var(--saff);border-radius:16px;display:grid;place-items:center;flex:none"><span style="transform:rotate(-45deg);font-size:17px;font-weight:800;text-align:center;width:120px">{t}</span></div>'
        bg = {"box": "#fff", "start": "var(--teal)", "end": "var(--ok)"}[kind]; c = "#fff" if kind != "box" else "var(--ink)"
        return f'<div style="background:{bg};color:{c};border:2px solid {"var(--line)" if kind=="box" else bg};border-radius:{"40px" if kind!="box" else "14px"};padding:16px 18px;font-size:18px;font-weight:700;text-align:center;flex:none;width:170px">{t}</div>'
    ar = f'<div style="color:var(--teal);flex:1;display:grid;place-items:center">{I("arrow-right")}</div>'
    flow = f'<div style="display:flex;align-items:center">{node("Open app","start")}{ar}{node("Offline balance ≥ amount?","dec")}{ar}{node("Scan request QR")}{ar}{node("QR valid &amp; verified?","dec")}{ar}{node("PIN → show voucher")}{ar}{node("Receipt received?","dec")}{ar}{node("Paid · pending sync","end")}</div>'
    no = '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:20px;margin-top:30px;font-size:17px;color:var(--bad);font-weight:700;text-align:center"><span>No → "Go online to load"</span><span>No → explain why, keep context</span><span>No → "Merchant didn\'t receive?" void</span></div>'
    panels = [("store", "Ravi types ₹40 for chai"), ("qr-code", "His POS shows a request QR"), ("wifi-off", "Priya has zero bars"), ("fingerprint", "She confirms with PIN"), ("scan-line", "Ravi scans her voucher"), ("party-popper", "Chime! Both get proof")]
    sb = "".join(f'<div class="card" style="padding:18px;align-items:center;text-align:center;gap:10px"><div style="width:100%;flex:1;border-radius:14px;background:linear-gradient(160deg,var(--tealsoft),#fff);display:grid;place-items:center;color:var(--teal)"><i data-lucide="{ic}" style="width:72px;height:72px"></i></div><div style="font-size:14px;font-weight:800;color:var(--saff)">PANEL {i+1}</div><div style="font-size:19px;font-weight:600;line-height:1.3">{t}</div></div>' for i,(ic,t) in enumerate(panels))
    body = f'''<div style="display:flex;flex-direction:column;gap:22px;height:100%"><div class="card" style="padding:22px 28px"><h4>User flow · payer</h4>{flow}{no}</div>
<div style="display:grid;grid-template-columns:repeat(6,1fr);gap:18px;flex:1">{sb}</div></div>'''
    return slide(22, U2, "User flow &amp; <em>storyboard</em>", "Decision points first, then the human moment told in six panels (from sprint day 3).", body,
                 "User flow · Storyboarding", "Google UX Certificate [R8] · Sprint storyboard [R5]", "docs/04-dfd.md L2 3.0 · docs/07 §3")

def wf(lines):
    return f'<div style="width:190px;height:380px;border:3px solid #5B6576;border-radius:28px;padding:16px 14px;display:flex;flex-direction:column;gap:10px;background:#fff;flex:none">{lines}</div>'

def s23():
    g = lambda h, w="100%", c="#D9D5CC": f'<div style="height:{h}px;width:{w};background:{c};border-radius:8px"></div>'
    x = lambda h: f'<div style="height:{h}px;border:2px solid #B8B2A6;border-radius:8px;background:linear-gradient(45deg,transparent 49%,#B8B2A6 50%,transparent 51%),linear-gradient(-45deg,transparent 49%,#B8B2A6 50%,transparent 51%)"></div>'
    home = wf(g(14,"70%") + g(90,"100%","#C7E9E3") + g(44) + g(44,"100%","#F5D79E") + g(12,"40%") + g(40) + g(40))
    scan = wf(g(14,"60%") + x(220) + '<div style="display:flex;gap:8px">' + g(40,"50%") + g(40,"50%") + '</div>')
    conf = wf(g(44) + g(54,"60%") + '<div style="display:grid;grid-template-columns:repeat(3,1fr);gap:6px">' + "".join(g(34) for _ in range(9)) + '</div>' + g(14,"40%"))
    rec = wf('<div style="width:70px;height:70px;border-radius:50%;background:#D9D5CC;margin:10px auto"></div>' + g(20,"80%") + g(44,"60%") + g(70) + g(42,"100%","#C7E9E3"))
    paper = "".join(f'<div style="background:#FFFDF6;border:1px solid #E7E0CF;border-radius:6px;padding:14px;box-shadow:0 6px 14px rgba(0,0,0,.08);transform:rotate({r}deg);font-family:Caveat;font-size:24px;color:#334155;display:flex;flex-direction:column;gap:8px;height:100%"><b style="font-size:26px">{t}</b>{"".join(f"<div style=\'height:{h}px;border:2px solid #5B6576;border-radius:6px\'></div>" for h in hs)}</div>' for t,r,hs in [("Home",-3,[40,22,30]),("Scan",2,[90]),("PIN",-2,[30,50]),("Done ✓",3,[50,22])])
    body = f'''<div style="display:grid;grid-template-columns:560px 1fr;gap:26px;height:100%">
<div class="card saff"><h4 style="color:#8A5300">1 · Paper wireframes · 4 core screens</h4><div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;flex:1;padding:6px">{paper}</div></div>
<div class="card"><div class="row" style="justify-content:space-between;align-items:baseline"><h4>2 · Digital lo-fi wireframes (Figma, greyscale)</h4><span class="tag" style="background:var(--tealsoft);color:var(--teal)">Clickable → tested on sprint day 5</span></div>
<div style="display:flex;justify-content:space-between;align-items:center;flex:1">{home}{scan}{conf}{rec}</div>
<div style="display:grid;grid-template-columns:repeat(4,190px);justify-content:space-between;font-size:18px;font-weight:700;text-align:center;color:var(--ink2)"><span>Home</span><span>Scan</span><span>Confirm + PIN</span><span>Receipt</span></div>
<div style="margin-top:14px;background:var(--paper);border-radius:14px;padding:14px 18px;font-size:19px;color:var(--ink2)"><b>Wireframe vs prototype in our project:</b> wireframes fixed layout and hierarchy without colour. The prototype linked them so users could <i>feel</i> the flow and timing.</div></div></div>'''
    return slide(23, U2, "Wireframes · <em>paper to digital</em>", "Low-fidelity first, so feedback was about flow and structure rather than colours.", body,
                 "Difference between wireframes and prototypes · Paper wireframes · Digital wireframing using Figma", "Google UX Certificate, Figma wireframing [R8]", "docs/design/ (Figma lo-fi)")

def s24():
    D = [("Hidden costs", "eye-off", "The merchant fee (0.3%) is shown on the POS before every settlement."),
         ("Confirmshaming", "frown", "A neutral 'Not now' instead of 'No, I like losing sales'."),
         ("Preselection", "square-check", "No pre-ticked consent or marketing opt-ins at sign-up."),
         ("Roach motel", "door-open", "Unloading offline balance takes 2 taps, as easy as loading."),
         ("Fake urgency", "hourglass", "No countdown pressure except the real 5-minute QR expiry."),
         ("Disguised status", "badge-alert", "Never 'Success' before proof. 'Pending sync' is shown honestly.")]
    cards = "".join(f'''<div class="card" style="padding:24px 26px"><div class="row" style="align-items:center;gap:14px"><div class="ic r" style="margin:0">{I(ic)}</div><div><div style="font-size:14px;font-weight:800;letter-spacing:.12em;color:var(--bad)">PATTERN REFUSED</div><h3 style="margin:0;font-size:27px">{t}</h3></div></div>
<div class="grow"><p style="font-size:21px;margin-top:12px"><b style="color:var(--ok)">Instead:</b> {d}</p></div></div>''' for t,ic,d in D)
    body = f'<div style="display:grid;grid-template-columns:repeat(3,1fr);grid-template-rows:1fr 1fr;gap:22px;height:100%">{cards}</div>'
    return slide(24, U2, "Avoiding <em>deceptive patterns</em>", "Money apps earn trust by being honest. Six dark patterns we deliberately designed out.", body,
                 "Avoiding deceptive patterns", "Brignull, deceptive.design [R13]", "AGENTS.md §10 UI rules")

def s25():
    return divider(25, "III", "Visual design, accessibility &amp; testing", ["Visual design system: Calm Ledger", "Accessibility: WCAG 2.2 AA &amp; ADA", "Usability testing", "A/B testing"], "Syllabus Unit III · 8 hours")

def s26():
    sw = [("Paper", "#F7F5F0", "Background", "var(--ink)"), ("Ink", "#0E1B2C", "Text · 15.9:1", "#fff"), ("Trust teal", "#0F766E", "Primary · 5.0:1", "#fff"), ("Saffron", "#F5A524", "Accent · CTA", "var(--ink)"), ("Mint", "#2DD4BF", "Dark-mode primary", "var(--ink)")]
    sws = "".join(f'<div style="flex:1;border-radius:18px;overflow:hidden;border:1px solid var(--line)"><div style="height:110px;background:{h}"></div><div style="padding:12px 14px;background:#fff"><b style="font-size:19px">{n}</b><div class="mono" style="font-size:15px;color:var(--muted)">{h}</div><div style="font-size:15px;color:var(--ink2)">{u}</div></div></div>' for n,h,u,t in sw)
    status = "".join(f'<span class="pill {c}" style="font-size:16px">{t}</span>' for c,t in [("p-off","Offline"),("p-pend","Pending sync"),("p-ok","Settled"),("p-bad","Failed")])
    scale = "".join(f'<div style="display:flex;align-items:baseline;gap:18px;border-bottom:1px solid var(--line);padding:6px 0"><span class="mono" style="font-size:14px;color:var(--muted);width:90px">{n} · {px}</span><span style="font-family:{f};font-size:{px}px;font-weight:{w};line-height:1.1">{t}</span></div>' for n,px,f,w,t in [("Hero","56","Space Grotesk","700","₹1,250.00"),("H1","40","Space Grotesk","700","Pay without internet"),("H3","26","Space Grotesk","600","Ravi General Store"),("Body","20","Inter","400","Payment received by merchant"),("Caption","15","Inter","600","PENDING SYNC · VALID 3 DAYS")])
    body = f'''<div style="display:grid;grid-template-columns:1.15fr 1fr;gap:26px;height:100%">
<div style="display:flex;flex-direction:column;gap:22px"><div class="card"><h4>Colour · theory &amp; roles</h4><div style="display:flex;gap:14px">{sws}</div>
<p style="font-size:19px;margin-top:14px"><b>Why:</b> teal reads as calm and trustworthy for money. Saffron is warm and locally familiar, and as a near-complement it makes the CTA pop. All text pairs pass WCAG AA contrast.</p></div>
<div class="card" style="flex:1"><h4>Status language: colour + icon + label</h4><div class="row" style="gap:12px;flex-wrap:wrap">{status}</div>
<div class="grow"><p style="font-size:19px">Used identically on payer, merchant and admin screens, so the colour alone is never the only signal.</p></div></div></div>
<div style="display:flex;flex-direction:column;gap:22px"><div class="card" style="flex:1"><h4>Typography · Space Grotesk + Inter</h4><div class="grow">{scale}</div></div>
<div class="card dark"><h4>Cohesive visual language</h4><p style="font-size:19px">4-pt spacing grid · 12/20 px radii · line icons (Lucide) · hierarchy, proximity, alignment and repetition (Gestalt) · imagery: real shops, not stock smiles.</p></div></div></div>'''
    return slide(26, U3, "Visual design system · <em>Calm Ledger</em>", "Key visual design principles, typography, colour theory and imagery, turned into one cohesive language.", body,
                 "Key Principles of Visual Design · Typography · Color Theory · Images · Cohesive visual language", "Lidwell et al., Universal Principles of Design [R28] · Itten, The Art of Color [R29]", "docs/design/tokens.json")

def s27():
    lv = [("A", "Minimum", "Alt text, keyboard access, no seizure flashes", False), ("AA", "Our target", "4.5:1 contrast, 24px targets, focus visible, resize 200%", True), ("AAA", "Enhanced", "7:1 contrast, sign language video: aspirational", False)]
    ladder = "".join(f'<div style="display:grid;grid-template-columns:110px 1fr;gap:16px;align-items:center;background:{"var(--teal)" if t else "#fff"};color:{"#fff" if t else "var(--ink)"};border-radius:18px;padding:14px 18px;border:1px solid var(--line);margin-left:{i*30}px"><span class="big" style="font-size:44px;color:{"var(--saff)" if t else "var(--teal)"}">{l}</span><div><b style="font-size:20px">{n}</b><div style="font-size:17px;opacity:.85">{d}</div></div></div>' for i,(l,n,d,t) in enumerate(lv))
    pour = [("Perceivable", "eye", "Status = icon + text + colour · 4.5:1 text contrast · captions on tutorial video"),
            ("Operable", "keyboard", "44px targets · full keyboard path · no time limits except QR expiry (extendable)"),
            ("Understandable", "languages", "Plain language, Hindi + English · errors say what to do next"),
            ("Robust", "code", "Semantic HTML + ARIA live region: 'Payment received ₹120' read aloud")]
    pc = "".join(f'<div class="card" style="padding:20px 22px"><div class="row" style="gap:12px;align-items:center"><div class="ic" style="margin:0;width:48px;height:48px">{I(ic)}</div><h3 style="margin:0;font-size:25px">{t}</h3></div><div class="grow"><p style="font-size:22px;margin-top:8px">{d}</p></div></div>' for t,ic,d in pour)
    tools = "".join(f'<span class="tag" style="background:rgba(255,255,255,.08);color:#fff;font-size:17px">{t}</span>' for t in ["axe DevTools", "Lighthouse", "WAVE", "Contrast checker", "TalkBack", "NVDA", "Keyboard-only pass"])
    body = f'''<div style="display:grid;grid-template-columns:.9fr 1.3fr;gap:26px;height:100%">
<div style="display:flex;flex-direction:column;gap:20px"><div class="card" style="flex:1"><h4>WCAG 2.2 levels of compliance</h4><div class="grow" style="gap:12px">{ladder}</div></div>
<div class="card saff"><h4 style="color:#8A5300">Why it matters · ADA</h4><p style="font-size:19px">The ADA (1990) requires equal access to services, and US courts apply it to apps. India's RPwD Act 2016 sets the same goal. For payments, access means independence.</p></div></div>
<div style="display:flex;flex-direction:column;gap:20px"><div style="display:grid;grid-template-columns:1fr 1fr;gap:20px;flex:1">{pc}</div>
<div class="card dark" style="padding:20px 26px"><h4>Tools &amp; techniques we used</h4><div class="row" style="gap:10px;flex-wrap:wrap">{tools}</div></div></div></div>'''
    return slide(27, U3, "Accessibility · <em>WCAG 2.2 AA</em>", "We designed for accessibility from the start, so it didn't need retrofitting. The POUR principles map to concrete features.", body,
                 "Importance of accessibility · WCAG · Levels of compliance · ADA · Tools &amp; techniques", "W3C WCAG 2.2 [R14] · ADA, U.S. DOJ [R15]", "docs/09-frontend-architecture.md §8")

def s28():
    tasks = [("Load ₹500 offline", 100), ("Pay ₹120 with no signal", 80), ("Find a settled payment", 100), ("Unload offline balance", 60)]
    tb = "".join(f'<div><div style="display:flex;justify-content:space-between;font-size:20px;font-weight:700;margin-bottom:6px"><span>T{i+1} · {t}</span><span>{v//20}/5</span></div><div style="height:16px;border-radius:9px;background:var(--paper2)"><div style="width:{v}%;height:100%;border-radius:9px;background:{"var(--ok)" if v==100 else ("var(--saff)" if v>=80 else "var(--bad)")}"></div></div></div>' for i,(t,v) in enumerate(tasks))
    body = f'''<div style="display:grid;grid-template-columns:1fr 1fr .9fr;gap:24px;height:100%">
<div class="card"><h4>Plan</h4><div class="grow" style="gap:12px;font-size:20px;color:var(--ink2)">
<div><b style="color:var(--ink)">Goal:</b> can first-time users complete an offline payment unaided?</div>
<div><b style="color:var(--ink)">Type:</b> moderated, in-person (shop) + remote unmoderated (Maze)</div>
<div><b style="color:var(--ink)">Participants:</b> 5 (2 merchants, 3 payers), enough to find ~85% of issues</div>
<div><b style="color:var(--ink)">Metrics:</b> task success · time on task · errors · SUS</div>
<div><b style="color:var(--ink)">Script:</b> think-aloud, no hints, 20 min each</div></div></div>
<div class="card"><h4>Results · task success</h4><div class="grow" style="gap:16px">{tb}</div>
<div class="row" style="gap:20px;align-items:center;margin-top:10px;background:var(--tealsoft);border-radius:16px;padding:14px 18px"><span class="big" style="font-size:56px;color:var(--teal)">78</span><span style="font-size:19px;color:var(--ink2)"><b>SUS score</b><br>above the 68 average ("good")</span></div></div>
<div class="card dark"><h4>Top 3 fixes shipped</h4><div class="grow" style="gap:14px">
<div style="font-size:20px;color:#C9D4E2"><b style="color:var(--saff)">1.</b> "Saved" read as final → renamed to <b style="color:#fff">"Pending sync"</b> + amber icon</div>
<div style="font-size:20px;color:#C9D4E2"><b style="color:var(--saff)">2.</b> Unload was hidden in Settings → moved to the balance card</div>
<div style="font-size:20px;color:#C9D4E2"><b style="color:var(--saff)">3.</b> QR glare in sunlight → auto max brightness + torch</div></div></div></div>'''
    return slide(28, U3, "Usability <em>testing</em>", "We planned and ran a test on the riskiest moments, not only the happy path.", body,
                 "Types of Usability Testing · Planning and Conducting Usability Tests", "Brooke, SUS [R16] · Nielsen, test with 5 users [R17]", "docs/10-testing-strategy.md", extra=SAMPLE)
