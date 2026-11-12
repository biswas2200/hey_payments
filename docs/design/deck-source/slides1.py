from parts import *

U1 = "Unit I · UX Fundamentals & Research"
U2 = "Unit II · Personas, Interaction & Wireframes"

def s01():
    return f'''<section class="slide dark" data-n="1">
<div style="position:absolute;left:96px;top:96px;z-index:2;display:flex;align-items:center;gap:16px">
<div style="width:64px;height:64px;border-radius:20px;background:linear-gradient(135deg,var(--mint),var(--teal));display:grid;place-items:center;color:var(--night)"><i data-lucide="wifi-off" style="width:34px;height:34px;stroke-width:2.4"></i></div>
<div class="disp" style="font-size:34px;font-weight:700">hey<span style="color:var(--saff)">.</span>payments</div></div>
<div style="position:absolute;left:96px;top:250px;width:930px;z-index:2">
<div class="kicker" style="color:var(--mint)">UI/UX Design & Product Management · Case Study</div>
<h1 class="disp" style="font-size:118px;line-height:.98;font-weight:700;letter-spacing:-.035em;margin-top:22px">Pay &amp; get paid,<br>even when the<br><span style="color:var(--saff)">network can't.</span></h1>
<p style="font-size:29px;line-height:1.4;color:#B8C6D8;margin-top:30px;width:860px">An offline-first payment app: load a small balance online, pay any shop with signed QR codes at zero signal, and settle automatically later.</p>
<div style="display:flex;gap:14px;margin-top:36px"><span class="pill p-off">Offline</span><span class="pill p-pend">Pending sync</span><span class="pill p-ok">Settled</span></div>
<div style="margin-top:56px;font-size:20px;color:#8DA0B8;display:flex;gap:48px"><span><b style="color:#fff">Student Name</b> · Roll No. ___</span><span>Course: UI/UX Design · LA submission</span></div></div>
<div style="position:absolute;right:120px;top:150px;width:720px;height:820px;z-index:2">
<div style="position:absolute;left:0;top:120px;transform:rotate(-8deg) scale(.9)">{phone(scr_scan())}</div>
<div style="position:absolute;right:0;top:120px;transform:rotate(8deg) scale(.9)">{phone(scr_merchant_ok())}</div>
<div style="position:absolute;left:195px;top:40px">{phone(scr_confirm())}</div></div></section>'''

def s02():
    units = [
        ("I", "UX Fundamentals & Research", ["UI vs UX", "Principles of good design", "Key elements of UX", "UX design process", "Design sprint", "Research plan & methods", "Bias, enquiry & insights"], "05–11"),
        ("II", "Personas, Interaction & Wireframes", ["Personas", "Empathy map", "Pain points & user stories", "Problem & hypothesis", "Ideation & design specs", "Competitive audit", "Journey map · Interaction design", "User flow, storyboard, wireframes", "Deceptive patterns"], "13–24"),
        ("III", "Visual Design, Accessibility & Testing", ["Visual design system", "Accessibility (WCAG, ADA)", "Usability testing", "A/B testing"], "26–29"),
        ("IV", "Hi-fi Prototype & Evolving UX", ["Payer hi-fi screens", "Offline handshake", "Merchant & admin", "Gestures & motion", "Maintaining & evolving UX"], "31–35"),
        ("V", "Product Management & Analytics", ["Product vision & PM role", "PRD, Lean, Scrum, Kanban", "Market, roadmap & MVP", "HEART & AARRR metrics"], "37–40"),
    ]
    cols = "".join(f'''<div class="card" style="display:flex;flex-direction:column;padding:28px 26px;{"background:var(--ink);color:#fff" if i==0 else ""}">
<div style="display:flex;justify-content:space-between;align-items:baseline"><div class="big" style="font-size:64px;color:{"var(--saff)" if i==0 else "var(--teal)"};line-height:1">{u}</div><div class="tag" style="background:{"rgba(45,212,191,.15)" if i==0 else "var(--tealsoft)"};color:{"var(--mint)" if i==0 else "var(--teal)"}">Slides {sl}</div></div>
<h3 style="margin-top:12px;font-size:25px;{"color:#fff" if i==0 else ""}">{t}</h3>
<ul style="margin-top:8px;gap:6px">{"".join(f'<li style="font-size:20px;{"color:#C9D4E2" if i==0 else ""}">{x}</li>' for x in items)}</ul>
<div style="margin-top:auto;padding-top:16px;border-top:1px dashed {"rgba(255,255,255,.2)" if i==0 else "var(--line)"};font-size:17px;font-weight:700;color:{"var(--saff)" if i==0 else "var(--teal)"}">{hrs}</div></div>''' for i,((u,t,items,sl),hrs) in enumerate(zip(units,["8 hrs · research artefacts","8 hrs · personas → wireframes","8 hrs · visual system & tests","8 hrs · hi-fi prototype","8 hrs · PRD & metrics"])))
    body = f'<div style="display:grid;grid-template-columns:repeat(5,1fr);gap:22px;height:100%">{cols}</div>'
    return slide(2, "Agenda", "Syllabus <em>map</em>", "Every unit of the syllabus is applied to one real product: what we did in Hey Payments, plus the method and source behind it.", body,
                 "All units (I–V), mapped to slides", "Each slide's ribbon cites its source. [R1]–[R31] are listed on slide 41", "docs/design/README.md")

def s03():
    steps = [("wifi", "1 · Load (online)", "Move up to ₹2,000 from the wallet into an offline balance. The server signs a balance certificate for this phone.", "ok"),
             ("qr-code", "2 · Pay (offline)", "Merchant shows a signed QR, payer confirms with PIN and shows a signed voucher, merchant shows a receipt. No network needed.", "off"),
             ("refresh-cw", "3 · Sync & settle", "When either phone is back online, vouchers upload and the ledger pays the merchant exactly once. Double spends are caught.", "pend")]
    stepc = "".join(f'''<div class="card" style="flex:1"><div class="ic {"g" if k=="ok" else ("o" if k=="off" else "s")}">{I(ic)}</div><h3>{t}</h3><p>{d}</p></div>''' + ('<div class="arrow">'+I("arrow-right")+'</div>' if i<2 else '') for i,(ic,t,d,k) in enumerate(steps))
    body = f'''<div style="display:grid;grid-template-columns:600px 1fr;gap:32px;height:100%">
<div class="card dark" style="display:flex;flex-direction:column;justify-content:space-between">
<div><h4>The problem</h4><h3 style="font-size:36px;color:#fff">Digital payments fail exactly where people need them most.</h3></div>
<ul><li>UPI needs the <b style="color:#fff">payer's</b> phone online: basements, metros, rural markets and festivals break it.</li>
<li>"Processing…" leaves both sides unsure if money moved, so queues stall and sales are lost.</li>
<li>Small shops fall back to cash: change problems and no records.</li></ul>
<div class="tag" style="background:rgba(245,165,36,.15);color:var(--saff);align-self:flex-start">Inspired by RBI's offline payments framework (2022)</div></div>
<div style="display:flex;flex-direction:column;gap:24px"><div class="kicker">Our product · how Hey Payments works</div>
<div style="display:flex;gap:16px;align-items:stretch;flex:1">{stepc}</div>
<div class="card tint" style="display:flex;flex-direction:row;gap:40px;align-items:center;padding:22px 32px">
<div><div class="big" style="font-size:44px;color:var(--teal)">&lt;10 s</div><div style="font-size:17px;color:var(--ink2)">offline handshake target</div></div>
<div><div class="big" style="font-size:44px;color:var(--teal)">0 bars</div><div style="font-size:17px;color:var(--ink2)">needed on either phone</div></div>
<div><div class="big" style="font-size:44px;color:var(--teal)">1×</div><div style="font-size:17px;color:var(--ink2)">exactly-once settlement</div></div>
<div><div class="big" style="font-size:44px;color:var(--teal)">AA</div><div style="font-size:17px;color:var(--ink2)">WCAG 2.2 accessibility</div></div></div></div></div>'''
    return slide(3, "Project overview", "The problem &amp; <em>our product</em>", "Hey Payments is built with Angular (PWA) and Spring Boot. This case study shows the UX work behind it.", body,
                 "Understanding Product · Problem framing", "RBI offline payments framework [R26] · NPCI UPI Lite [R27]", "docs/01-PRD.md · docs/03-offline-protocol.md")

def divider(n, unit, title_, topics, art):
    t = "".join(f'<div style="display:flex;gap:16px;align-items:center;font-size:25px;color:#C9D4E2;padding:12px 0;border-bottom:1px solid rgba(255,255,255,.08)"><span class="mono" style="color:var(--saff);font-size:18px">{i+1:02d}</span>{x}</div>' for i,x in enumerate(topics))
    return f'''<section class="slide dark" data-n="{n}"><div class="hdr"><div class="unit"><span class="dot"></span>Section</div><div class="meta">Hey Payments · UI/UX Case Study <b>{n:02d}/{TOTAL}</b></div></div>
<div style="position:absolute;left:96px;top:150px;z-index:2"><div class="divnum">{ {"I":"01","II":"02","III":"03","IV":"04","V":"05"}[unit] }</div>
<div class="kicker" style="color:var(--mint);margin-top:40px">Unit {unit}</div>
<h1 class="disp" style="font-size:88px;font-weight:700;line-height:1.02;letter-spacing:-.03em;margin-top:14px;width:900px">{title_}</h1></div>
<div style="position:absolute;right:110px;top:220px;width:640px;z-index:2">{t}</div>
<div style="position:absolute;right:110px;bottom:80px;z-index:2;font-size:18px;color:#8DA0B8">{art}</div></section>'''

def s04():
    return divider(4, "I", "UX fundamentals, process &amp; research", ["UI vs UX in Hey Payments", "Principles of good design", "Key elements of UX", "Our UX design process", "Design sprint (5 days)", "UX research plan", "Bias, enquiry &amp; actionable insights"], "Syllabus Unit I · 8 hours")

def s05():
    ph = phone(scr_confirm())
    L = [("1", "Hero amount", "₹120 in Space Grotesk 50px, the first thing the eye lands on."),
         ("2", "Verified badge", "Green check plus text shows the merchant QR's signature is valid."),
         ("3", "44px PIN keys", "Large targets for one-handed use in a moving metro.")]
    R = [("4", "Trust before paying", "Payer sees who and how much is verified offline before entering a PIN."),
         ("5", "No spinner, no waiting", "Signing happens on-device, so the flow never waits on the network."),
         ("6", "Honest status after", "'Pending sync' rather than a fake 'Success' until the server settles.")]
    def col(items, side):
        return "".join(f'<div class="callout" style="position:relative;width:auto;text-align:{side};font-size:20.5px"><span class="n">{n}</span><b style="font-size:23px">{t}</b>{d}</div>' for n,t,d in items)
    body = f'''<div style="display:grid;grid-template-columns:1fr 360px 1fr;gap:56px;height:100%;align-items:center">
<div class="card" style="height:100%;display:flex;flex-direction:column;gap:16px;justify-content:center;padding:24px 30px"><div class="row" style="align-items:center;gap:14px"><div class="ic" style="margin:0">{I("palette")}</div><div><h4 style="margin:0">UI · what users see &amp; touch</h4><div style="font-size:18px;color:var(--muted)">Interface decisions on the confirm screen</div></div></div>{col(L,"left")}</div>
<div style="display:flex;justify-content:center;zoom:.92">{ph}</div>
<div class="card dark" style="height:100%;display:flex;flex-direction:column;gap:16px;justify-content:center;padding:24px 30px"><div class="row" style="align-items:center;gap:14px"><div class="ic s" style="margin:0">{I("heart-handshake")}</div><div><h4 style="margin:0">UX · how it works &amp; feels</h4><div style="font-size:18px;color:#8DA0B8">Experience decisions across the journey</div></div></div>
<div style="display:flex;flex-direction:column;gap:16px;color:#C9D4E2">{"".join(f'<div style="font-size:20.5px;line-height:1.35"><span class="n" style="display:inline-grid;place-items:center;width:30px;height:30px;border-radius:50%;background:var(--saff);color:var(--ink);font-weight:800;font-size:15px;margin-bottom:6px">{n}</span><b style="display:block;color:#fff;font-size:23px;margin-bottom:3px">{t}</b>{d}</div>' for n,t,d in R)}</div></div></div>'''
    return slide(5, U1, "UI vs UX <em>in Hey Payments</em>", "One screen, two lenses: the payment confirmation, where trust is won or lost.", body,
                 "Difference between UI and UX", "Norman, Design of Everyday Things [R1] · Google UX Certificate [R8]", "docs/09-frontend-architecture.md")

def s06():
    P = [("eye", "Visibility of system status", "Nielsen #1", "A permanent banner shows Offline, Pending sync or Settled on every screen."),
         ("shield-alert", "Error prevention", "Nielsen #5", "₹500 per-payment and ₹2,000 offline caps are shown before paying. Expired QR codes are rejected."),
         ("languages", "Match the real world", "Nielsen #2", "₹1,23,450 Indian number format, Hindi UI and familiar shop language ('takings')."),
         ("undo-2", "User control &amp; freedom", "Nielsen #3", "Cancel on every step. 'Merchant didn't receive?' voids an unreceipted voucher."),
         ("badge-check", "Honest design", "Rams #6", "Merchant fee (0.3%) shown up front. Nothing is called 'success' until it's proven."),
         ("minimize-2", "As little design as possible", "Rams #10", "Three taps to pay: scan, PIN, show. One primary action per screen.")]
    cards = "".join(f'''<div class="card" style="display:flex;flex-direction:column;justify-content:center"><div class="row" style="justify-content:space-between;align-items:flex-start"><div class="ic">{I(ic)}</div><span class="tag" style="background:var(--saffsoft);color:#8A5300">{src}</span></div>
<h3 style="font-size:30px">{t}</h3><p style="font-size:25px">{d}</p></div>''' for ic,t,src,d in P)
    body = f'<div style="display:grid;grid-template-columns:repeat(3,1fr);grid-template-rows:1fr 1fr;gap:26px;height:100%">{cards}</div>'
    return slide(6, U1, "Principles of <em>good design</em>", "We picked six principles and turned each into a concrete product decision.", body,
                 "Principles of Good Design", "Nielsen's 10 heuristics [R2] · Rams' 10 principles [R3]", "docs/01-PRD.md §7 (NFRs)")

def s07():
    H = [("Useful", "Pays with zero network on both phones", "target", 0, 1),
         ("Usable", "3-step QR handshake under 10 s", "mouse-pointer-click", 1, 0),
         ("Desirable", "Calm visual identity, success haptic + chime", "sparkles", 1, 2),
         ("Valuable", "Merchants never lose a sale to bad signal", "indian-rupee", 2, 1),
         ("Findable", "'Pay without internet' is the main home action", "search", 3, 0),
         ("Accessible", "WCAG 2.2 AA, Hindi, large-text mode", "accessibility", 3, 2),
         ("Credible", "Signed QR, verified badge, receipt code", "shield-check", 4, 1)]
    pos = {(0,1):(560,0),(1,0):(300,150),(1,2):(820,150),(2,1):(560,300),(3,0):(300,450),(3,2):(820,450),(4,1):(1080,300)}
    hexes = ""
    for t,d,ic,r,c in H:
        x,y = pos[(r,c)]
        center = t=="Valuable"
        hexes += f'''<div style="position:absolute;left:{x}px;top:{y}px;width:290px;height:300px;clip-path:polygon(50% 0,100% 25%,100% 75%,50% 100%,0 75%,0 25%);background:{"var(--ink)" if center else ("var(--teal)" if t in ("Useful","Credible") else "#fff")};display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;padding:0 26px;color:{"#fff" if center or t in ("Useful","Credible") else "var(--ink)"}">
<i data-lucide="{ic}" style="width:34px;height:34px;color:{"var(--saff)" if center else ("var(--mint)" if t in ("Useful","Credible") else "var(--teal)")}"></i>
<div class="disp" style="font-size:33px;font-weight:700;margin:8px 0 6px">{t}</div><div style="font-size:20.5px;line-height:1.28;opacity:.92">{d}</div></div>'''
    side = '''<div class="card tint" style="position:absolute;right:0;top:0;bottom:0;width:560px;display:flex;flex-direction:column;gap:18px;justify-content:center;padding:36px 40px"><h4>How we used it</h4>
<p style="font-size:26px">Each facet became a review checklist question for every screen in the design critique.</p><p style="font-size:26px">Example: <b>Credible</b> led to a signed receipt QR with a short code the merchant can read aloud.</p><p style="font-size:26px">Example: <b>Findable</b> made "Pay without internet" the saffron primary button on Home.</p></div>'''
    body = f'<div style="position:relative;height:100%;"><div style="position:absolute;left:-235px;top:-16px;transform:scale(.86);transform-origin:top left;width:1400px;height:760px">{hexes}</div>{side}</div>'
    return slide(7, U1, "Key elements <em>of UX</em>", "Morville's UX honeycomb, filled in with Hey Payments features.", body,
                 "Key Elements of UX", "Morville's UX Honeycomb (2004) [R4]", "docs/01-PRD.md §3 Goals")

def s08():
    S = [("search", "Research", "Interviews · survey · contextual enquiry · competitive audit", "Slides 10–11, 19", "Discover"),
         ("pen-tool", "Design", "Personas · journey map · user flows · paper and Figma wireframes", "Slides 13–24", "Define"),
         ("layers", "Prototype", "Clickable Figma lo-fi, then hi-fi built from design tokens", "Slides 31–34", "Develop"),
         ("flask-conical", "Test", "Moderated usability (n=5, SUS) · A/B test on CTA label", "Slides 28–29", "Develop"),
         ("rocket", "Implement", "tokens.json → Angular PWA · TDD · handoff via AGENTS.md", "docs/09, docs/12", "Deliver")]
    cards = "".join(f'''<div class="card" style="flex:1;display:flex;flex-direction:column;{"background:var(--ink);color:#fff" if i==4 else ""}">
<div class="row" style="justify-content:space-between"><div class="ic {"s" if i==4 else ""}">{I(ic)}</div><span class="big" style="font-size:40px;color:{"var(--saff)" if i==4 else "var(--line)"}">0{i+1}</span></div>
<h3 style="{"color:#fff" if i==4 else ""}">{t}</h3><p style="flex:1;{"color:#C9D4E2" if i==4 else ""}">{d}</p>
<div class="tag" style="margin-top:14px;align-self:flex-start;background:{"rgba(45,212,191,.15)" if i==4 else "var(--tealsoft)"};color:{"var(--mint)" if i==4 else "var(--teal)"}">{a}</div></div>''' + (f'<div class="arrow">{I("chevron-right")}</div>' if i<4 else "") for i,(ic,t,d,a,dd) in enumerate(S))
    dd = "".join(f'<div style="flex:{w};text-align:center;padding:14px;border-radius:16px;background:{c};color:{tc};font-weight:800;font-size:19px;letter-spacing:.06em">{t}</div>' for t,w,c,tc in [("DISCOVER",1,"var(--tealsoft)","var(--teal)"),("DEFINE",1,"#C9F0E8","var(--teal)"),("DEVELOP",2,"var(--saffsoft)","#8A5300"),("DELIVER",1,"var(--ink)","#fff")])
    body = f'''<div style="display:flex;flex-direction:column;gap:22px;height:100%"><div style="display:flex;gap:12px;align-items:stretch;flex:1">{cards}</div>
<div style="display:flex;gap:12px;align-items:center"><div style="font-size:16px;font-weight:800;color:var(--muted);width:150px;letter-spacing:.08em">DOUBLE DIAMOND</div>{dd}</div></div>'''
    return slide(8, U1, "Our UX <em>design process</em>", "The five stages from the syllabus, with what we actually produced at each one.", body,
                 "Stages of UX design process: Research → Design → Prototyping → Testing → Implementation", "Design Council Double Diamond [R6] · Google UX Certificate [R8]", "docs/12-implementation-plan.md")

def s09():
    D = [("Mon", "Understand", "Map the offline payment journey, interview 2 experts, choose a target moment: paying in a zero-signal queue.", "Long-term goal + sprint questions"),
         ("Tue", "Sketch", "Lightning demos of UPI Lite X and boarding-pass QR codes, then Crazy 8s and solution sketches.", "8 sketches → 3 concepts"),
         ("Wed", "Decide", "Dot voting chose the 'QR handshake + receipt' concept. Built a 12-panel storyboard.", "Storyboard (slide 22)"),
         ("Thu", "Prototype", "Clickable Figma prototype of payer and merchant with realistic ₹ amounts.", "Figma prototype"),
         ("Fri", "Test", "5 users (2 shopkeepers, 3 students) completed the offline payment with think-aloud.", "Findings → backlog")]
    cols = "".join(f'''<div class="card" style="display:flex;flex-direction:column;padding:0;overflow:hidden">
<div style="background:{"var(--saff)" if i==4 else "var(--teal)"};color:{"var(--ink)" if i==4 else "#fff"};padding:18px 26px;display:flex;justify-content:space-between;align-items:baseline"><span class="big" style="font-size:34px">{d}</span><span style="font-weight:800;letter-spacing:.1em;font-size:16px">DAY {i+1}</span></div>
<div style="padding:24px 26px;display:flex;flex-direction:column;flex:1"><h3>{t}</h3><p style="flex:1">{x}</p><div style="margin-top:12px;padding-top:14px;border-top:1px dashed var(--line);font-size:17px;font-weight:700;color:var(--teal)">Output: {o}</div></div></div>''' for i,(d,t,x,o) in enumerate(D))
    body = f'''<div style="display:flex;flex-direction:column;gap:22px;height:100%">
<div class="card dark" style="display:flex;flex-direction:row;gap:36px;align-items:center;padding:20px 32px"><div class="ic s" style="margin:0">{I("flag")}</div>
<div style="font-size:22px;color:#E8EEF5"><b style="color:var(--saff)">Sprint question:</b> Can a shopper pay with zero signal, and can both people trust that the money moved?</div></div>
<div style="display:grid;grid-template-columns:repeat(5,1fr);gap:20px;flex:1">{cols}</div></div>'''
    return slide(9, U1, "Design <em>sprint</em>", "A 5-day Google Ventures style sprint compressed the riskiest question into one testable week.", body,
                 "Design Sprints", "Knapp et al., Sprint (GV, 2016) [R5]", "docs/design/canva-link.md (sprint artefacts)")

def s10():
    pts = [("Interviews", 22, 18, "qual"), ("Contextual enquiry<br>(market visit)", 70, 22, "qual"), ("Usability test", 74, 46, "qual"),
           ("Survey (n=120)", 24, 74, "quant"), ("Funnel analytics", 76, 80, "quant"), ("A/B test", 60, 66, "quant")]
    dots = "".join(f'<div style="position:absolute;left:{x}%;top:{y}%;transform:translate(-50%,-50%);display:flex;flex-direction:column;align-items:center;gap:6px"><span style="width:22px;height:22px;border-radius:50%;background:{"var(--teal)" if k=="qual" else "var(--saff)"};box-shadow:0 0 0 6px {"rgba(15,118,110,.18)" if k=="qual" else "rgba(245,165,36,.25)"}"></span><span style="font-size:17px;font-weight:700;text-align:center;white-space:nowrap">{t}</span></div>' for t,x,y,k in pts)
    matrix = f'''<div class="card" style="position:relative;height:100%;padding:0">
<div style="position:absolute;left:70px;right:30px;top:30px;bottom:60px;border-left:2px solid var(--ink);border-bottom:2px solid var(--ink);background:linear-gradient(90deg,transparent 49.8%,var(--line) 50%,transparent 50.2%),linear-gradient(0deg,transparent 49.8%,var(--line) 50%,transparent 50.2%)">{dots}</div>
<div style="position:absolute;left:70px;right:30px;bottom:18px;display:flex;justify-content:space-between;font-size:15px;font-weight:800;letter-spacing:.1em;color:var(--muted)"><span>ATTITUDINAL · what people say</span><span>BEHAVIOURAL · what people do</span></div>
<div style="position:absolute;left:18px;top:30px;bottom:60px;writing-mode:vertical-rl;transform:rotate(180deg);display:flex;justify-content:space-between;font-size:15px;font-weight:800;letter-spacing:.1em;color:var(--muted)"><span>QUANTITATIVE · how many</span><span>QUALITATIVE · why</span></div></div>'''
    rq = '''<div class="card" style="display:flex;flex-direction:column;gap:12px"><h4>Research questions</h4>
<ul><li>When and where do payments fail?</li><li>What proof does a shopkeeper need?</li><li>Which words make "offline" feel safe?</li></ul></div>'''
    pp = '''<div class="card" style="display:flex;flex-direction:column;gap:10px"><h4>Participants (planned)</h4>
<table style="font-size:19px"><tr><td style="padding:6px 0">Kirana and chai-stall owners</td><td style="text-align:right;font-weight:800">4 interviews</td></tr>
<tr><td style="padding:6px 0;border-top:1px solid var(--line)">Metro commuters / students</td><td style="text-align:right;font-weight:800;border-top:1px solid var(--line)">4 interviews</td></tr>
<tr><td style="padding:6px 0;border-top:1px solid var(--line)">Campus survey (Google Forms)</td><td style="text-align:right;font-weight:800;border-top:1px solid var(--line)">120 responses</td></tr></table></div>'''
    why = '''<div class="card tint" style="padding:22px 30px"><h4>Why this mix</h4><p>Qualitative methods explain the "Did it go?" anxiety. The survey measures how common it is. Behavioural methods check what people actually do.</p></div>'''
    body = f'<div style="display:grid;grid-template-columns:1.05fr 1fr;gap:28px;height:100%">{matrix}<div style="display:flex;flex-direction:column;gap:14px">{rq}{pp}{why}</div></div>'
    return slide(10, U1, "UX <em>research plan</em>", "We chose methods by the question we needed answered and plotted them on the research landscape.", body,
                 "Types of user research · Choosing the right method · Integrating research into the design process", "NN/g research methods landscape, Rohrer [R7] · Google UX Certificate [R8]", "docs/01-PRD.md §4 Users")

def s11():
    B = [("Confirmation bias", "Wrote a neutral interview script and piloted it before use"), ("Sampling bias", "Recruited both urban and semi-urban shops, not just classmates"),
         ("Social desirability", "Observed real checkouts rather than asking 'would you use it?'"), ("Leading questions", "Asked 'Tell me about the last failed payment', never 'Is offline useful?'")]
    E = ["Trust &amp; proof", "Speed at the counter", "Connectivity patterns", "Record keeping", "Language &amp; literacy", "Fear of double charges"]
    IA = [("Shopkeepers hand over goods only after visible proof", "Signed receipt QR + short code read aloud"),
          ("'Saved' was read as 'done', which caused confusion later", "Three-state language: Offline · Pending · Settled"),
          ("Users retry failed payments and fear paying twice", "Idempotent vouchers + 'one voucher per request'"),
          ("Hindi labels raised confidence for older merchants", "Hindi UI and large-text mode from v1")]
    body = f'''<div style="display:grid;grid-template-columns:1fr .8fr 1.5fr;gap:26px;height:100%">
<div class="card"><h4>Bias we guarded against</h4><div class="grow" style="gap:18px">{"".join(f'<div><b style="font-size:23px">{t}</b><div style="font-size:20.5px;color:var(--ink2);line-height:1.38">{d}</div></div>' for t,d in B)}</div></div>
<div class="card tint"><h4>Areas of enquiry</h4><div class="grow" style="gap:12px">{"".join(f'<div style="background:#fff;border-radius:16px;padding:20px 20px;font-size:22px;font-weight:700">{e}</div>' for e in E)}</div></div>
<div class="card dark"><h4>Insight → design action</h4><div class="grow" style="gap:14px">{"".join(f'<div style="display:grid;grid-template-columns:1fr 40px 1fr;gap:10px;align-items:center;background:rgba(255,255,255,.05);border-radius:16px;padding:22px 20px"><div style="font-size:20.5px;color:#C9D4E2;line-height:1.38">{a}</div><div style="color:var(--saff)">{I("arrow-right")}</div><div style="font-size:20.5px;color:#fff;font-weight:700;line-height:1.38">{b}</div></div>' for a,b in IA)}</div></div></div>'''
    return slide(11, U1, "Bias, enquiry &amp; <em>actionable insights</em>", "How we kept the research honest, and how each insight became a feature.", body,
                 "Bias in UX research · Areas of enquiry · Translating research into actionable insights", "Google UX Certificate, research bias module [R8] · NN/g [R7]", "docs/01-PRD.md §2 Problem", extra=SAMPLE)

def s12():
    return divider(12, "II", "Personas, interaction design &amp; wireframes", ["Personas: Ravi, Priya, Arjun", "Empathy map", "Pain points, user stories &amp; scenarios", "Problem &amp; hypothesis statements", "Ideation &amp; design specifications", "Competitive audit", "Journey map &amp; interaction design", "User flow, storyboard &amp; wireframes", "Avoiding deceptive patterns"], "Syllabus Unit II · 8 hours")

def avatar(init, c1, c2, size=150):
    return f'''<div style="width:{size}px;height:{size}px;border-radius:50%;background:linear-gradient(135deg,{c1},{c2});display:grid;place-items:center;position:relative;flex:none">
<span class="disp" style="font-size:{size*0.42:.0f}px;font-weight:700;color:#fff">{init}</span></div>'''

def s13():
    meter = lambda v: f'<div style="height:10px;border-radius:6px;background:var(--paper2)"><div style="width:{v}%;height:100%;border-radius:6px;background:var(--teal)"></div></div>'
    body = f'''<div style="display:grid;grid-template-columns:470px 1fr 1fr;gap:26px;height:100%">
<div class="card dark" style="display:flex;flex-direction:column;align-items:center;text-align:center;gap:14px">
{avatar("R","#F5A524","#B45309",120)}<div><div class="disp" style="font-size:40px;font-weight:700;color:#fff">Ravi Sharma</div><div style="font-size:19px;color:#8DA0B8">42 · Kirana store owner · Nashik outskirts</div></div>
<div class="tag" style="background:rgba(45,212,191,.15);color:var(--mint)">Primary persona · Merchant</div>
<div style="width:100%;text-align:left;display:flex;flex-direction:column;gap:8px;font-size:16px;color:#C9D4E2">
<div>Tech comfort {meter(45)}</div><div>Network reliability at shop {meter(25)}</div><div>Daily digital payments {meter(70)}</div></div>
<div style="font-size:21px;font-style:italic;color:var(--saff);line-height:1.35;margin-top:auto">"If the payment fails, the customer just walks away."</div></div>
<div style="display:flex;flex-direction:column;gap:18px"><div class="card" style="flex:1;padding:24px 30px"><div class="ic g">{I("target")}</div><h3>Goals</h3><ul><li>Accept every payment, even at one bar of signal</li><li>Know instantly that money is real</li></ul></div>
<div class="card" style="flex:1;padding:24px 30px"><div class="ic r">{I("frown")}</div><h3>Frustrations</h3><ul><li>UPI stuck at "processing" with a queue waiting</li><li>No change for cash; lost small sales</li></ul></div></div>
<div style="display:flex;flex-direction:column;gap:18px"><div class="card" style="flex:1;padding:24px 30px"><div class="ic o">{I("footprints")}</div><h3>Behaviours</h3><ul><li>Android phone, prefers Hindi, uses WhatsApp daily</li><li>Busiest 7–9 pm. Shop is in a concrete basement row</li></ul></div>
<div class="card tint" style="flex:1;padding:24px 30px"><div class="ic">{I("lightbulb")}</div><h3>How Hey Payments serves Ravi</h3><ul><li>Merchant POS with keypad → QR in 2 taps</li><li>Verified receipt + chime, works offline</li><li>"Today's takings" with Pending vs Settled</li></ul></div></div></div>'''
    return slide(13, U2, "Personas · <em>Ravi, the merchant</em>", "Built from the 4 merchant interviews: the person we design the merchant POS around.", body,
                 "Creating a Persona · Using Personas in the Design Process", "Cooper, The Inmates Are Running the Asylum [R9] · Google UX Certificate [R8]", "docs/01-PRD.md §4 Personas")

def s14():
    def pc(init, c1, c2, name, meta, role, goal, frus, quote, hp):
        return f'''<div class="card" style="display:flex;flex-direction:column;gap:16px">
<div class="row" style="align-items:center;gap:20px">{avatar(init,c1,c2,104)}<div><div class="disp" style="font-size:34px;font-weight:700">{name}</div><div style="font-size:18px;color:var(--muted)">{meta}</div><div class="tag" style="margin-top:6px;background:var(--tealsoft);color:var(--teal)">{role}</div></div></div>
<div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;flex:1"><div class="card tint" style="padding:20px 22px"><h4>Goals</h4><ul>{"".join(f"<li style='font-size:22px'>{g}</li>" for g in goal)}</ul></div>
<div class="card saff" style="padding:20px 22px"><h4 style="color:#8A5300">Frustrations</h4><ul>{"".join(f"<li style='font-size:22px'>{g}</li>" for g in frus)}</ul></div></div>
<div style="font-size:25px;font-style:italic;color:var(--teal);font-weight:600">"{quote}"</div>
<div style="font-size:21px;color:var(--ink2);border-top:1px dashed var(--line);padding-top:12px"><b>Design impact:</b> {hp}</div></div>'''
    body = f'''<div class="g2">{pc("P","#4338CA","#0F766E","Priya Nair","27 · Metro commuter · Mumbai","Secondary persona · Payer",["Pay in under 10 s in underground stations","One-handed use while standing","Proof she can show if asked"],["Payments hang at the station kiosk","Never sure if she was charged","Two bars of signal at best"],"Don't make me hold up the queue.","big saffron 'Pay without internet' CTA, PIN pad sized for thumbs.")}
{pc("A","#0F766E","#14B8A6","Arjun Kulkarni","20 · Engineering student · hostel","Secondary persona · Payer",["Small payments at canteen and xerox shop","Keep track of a tight budget","No awkward moments at the counter"],["Hostel Wi-Fi drops constantly","Embarrassed retrying in front of friends","Older phone, little storage"],"I hate trying again while everyone watches.","offline balance meter with 'valid 3 days', discreet error recovery.")}</div>'''
    return slide(14, U2, "Personas · <em>Priya &amp; Arjun</em>", "Two payer personas with different contexts but one shared need: certainty at the counter.", body,
                 "Creating a Persona · Using Personas in the Design Process", "Cooper [R9] · Google UX Certificate persona template [R8]", "docs/01-PRD.md §4 Personas")
