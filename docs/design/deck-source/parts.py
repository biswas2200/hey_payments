# Shared building blocks for the Hey Payments deck
TOTAL = 43

def I(name, cls=""):
    return f'<i data-lucide="{name}" class="{cls}"></i>'

def head(unit, n, dark=False):
    return f'''<div class="hdr"><div class="unit"><span class="dot"></span>{unit}</div>
<div class="meta">Hey Payments · UI/UX Case Study <b>{n:02d}/{TOTAL}</b></div></div>'''

def title(h, sub=""):
    return f'<div class="ttl"><h1>{h}</h1>{f"<p>{sub}</p>" if sub else ""}</div>'

def ref(syl, method, artefact):
    return f'''<div class="ref"><div><span>Topic covered</span>{syl}</div>
<div><span>Method / source used</span>{method}</div>
<div><span>Project artefact</span><code>{artefact}</code></div></div>'''

def slide(n, unit, h, sub, body, syl, method, artefact, extra="", cls="light"):
    return f'''<section class="slide {cls}" data-n="{n}"><div class="blob2"></div>{head(unit,n)}{title(h,sub)}
<div class="body">{body}</div>{extra}{ref(syl,method,artefact)}</section>'''

SAMPLE = '<div class="sample">Sample data · replace with your study results</div>'

# ---------- phone parts ----------
def sbar(dark=False):
    return f'''<div class="sb"><span>9:41</span><span class="r">{I("signal-zero")}{I("wifi-off")}{I("battery-full")}</span></div>'''

def phone(inner, style=""):
    return f'<div class="phone" style="{style}"><div class="scr">{sbar()}<div class="in">{inner}</div></div></div>'

def banner(kind, text):
    icon = {"off": "wifi-off", "pend": "refresh-cw", "ok": "circle-check"}[kind]
    return f'<div class="banner b-{kind}">{I(icon)}{text}</div>'

def qr(seed, size=190):
    return f'<div class="qr"><div data-qr="{seed}" data-size="{size}"></div></div>'

def scr_home():
    return f'''{banner("off","Offline · 2 payments pending sync")}
<div style="display:flex;justify-content:space-between;align-items:center"><div><div class="lbl">Good evening</div><div style="font-weight:800;font-size:18px">Priya</div></div>
<div style="width:38px;height:38px;border-radius:50%;background:var(--saff);display:grid;place-items:center;font-weight:800">P</div></div>
<div class="balcard"><small>Offline balance</small><div class="amt">₹1,640</div><div style="font-size:12px;opacity:.85;margin-top:4px">of ₹2,000 · valid 3 days</div>
<div style="height:6px;border-radius:6px;background:rgba(255,255,255,.25);margin-top:10px"><div style="width:82%;height:100%;border-radius:6px;background:var(--saff)"></div></div></div>
<div class="mini" style="display:flex;justify-content:space-between"><span><span class="lbl">Main wallet</span><br><b>₹4,250.00</b></span><span style="align-self:center;color:var(--teal);font-weight:800">Add +</span></div>
<div class="btn sf" style="display:flex;gap:8px;justify-content:center;align-items:center">{I("scan-line")}Pay without internet</div>
<div class="lbl">Recent</div>
<div class="mini" style="display:flex;justify-content:space-between;align-items:center"><span><b>Ravi General Store</b><br><span style="color:var(--muted)">Today · 7:42 pm</span></span><span style="text-align:right"><b>−₹120</b><br><span style="color:var(--pend);font-weight:700;font-size:11px">● Pending sync</span></span></div>
<div class="mini" style="display:flex;justify-content:space-between;align-items:center"><span><b>Metro Chai Point</b><br><span style="color:var(--muted)">Yesterday</span></span><span style="text-align:right"><b>−₹40</b><br><span style="color:var(--ok);font-weight:700;font-size:11px">● Settled</span></span></div>'''

def scr_load():
    return f'''{banner("ok","Online · synced 1 min ago")}
<div style="font-weight:800;font-size:19px">Load offline balance</div>
<div style="font-size:12.5px;color:var(--muted);line-height:1.4">Money moves from your wallet into a safe offline pocket on this phone only.</div>
<div style="text-align:center;padding:8px 0"><div class="lbl">Amount</div><div class="disp" style="font-size:46px;font-weight:700">₹500</div>
<div style="font-size:12px;color:var(--muted)">Max offline ₹2,000 · ₹500 per payment</div></div>
<div style="display:flex;gap:8px;justify-content:center">{''.join(f'<span class="tag" style="background:{"var(--teal)" if a=="₹500" else "#fff"};color:{"#fff" if a=="₹500" else "var(--ink)"};border:1px solid var(--line)">{a}</span>' for a in ["₹200","₹500","₹1,000"])}</div>
<div class="keypad">{''.join(f"<div>{k}</div>" for k in ["1","2","3","4","5","6","7","8","9","·","0","⌫"])}</div>
<div class="btn">Load ₹500</div>'''

def scr_confirm():
    return f'''{banner("off","Offline mode · no internet needed")}
<div class="mini" style="display:flex;gap:10px;align-items:center"><div style="width:42px;height:42px;border-radius:12px;background:var(--tealsoft);display:grid;place-items:center;color:var(--teal)">{I("store")}</div>
<div><b>Ravi General Store</b><div style="color:var(--ok);font-weight:700;font-size:12px;display:flex;gap:4px;align-items:center">{I("badge-check")} Verified merchant</div></div></div>
<div style="text-align:center"><div class="lbl">You pay</div><div class="disp" style="font-size:50px;font-weight:700;line-height:1.1">₹120</div><div style="font-size:12px;color:var(--muted)">2× chai, 1 samosa</div></div>
<div style="text-align:center;font-size:12.5px;font-weight:700">Enter payment PIN</div>
<div style="display:flex;gap:10px;justify-content:center">{''.join('<span style="width:14px;height:14px;border-radius:50%;background:var(--ink)"></span>' for _ in range(4))}{''.join('<span style="width:14px;height:14px;border-radius:50%;border:2px solid var(--ink)"></span>' for _ in range(2))}</div>
<div class="keypad">{''.join(f"<div>{k}</div>" for k in ["1","2","3","4","5","6","7","8","9","","0","⌫"])}</div>
<div style="text-align:center;font-size:13px;font-weight:700;color:var(--muted)">Cancel</div>'''

def scr_receipt():
    return f'''{banner("pend","Pending sync · will settle when online")}
<div style="text-align:center;margin-top:6px"><div style="width:84px;height:84px;margin:0 auto;border-radius:50%;background:var(--oksoft);display:grid;place-items:center;color:var(--ok)"><i data-lucide="circle-check" style="width:50px;height:50px"></i></div>
<div style="font-weight:800;font-size:19px;margin-top:10px">Payment received by merchant</div>
<div class="disp" style="font-size:44px;font-weight:700">₹120</div><div style="font-size:12.5px;color:var(--muted)">to Ravi General Store</div></div>
<div class="mini" style="display:grid;grid-template-columns:1fr 1fr;gap:6px;font-size:12px"><span class="lbl">Receipt code</span><span class="mono" style="font-weight:700;text-align:right">HP7-K2Q9</span>
<span class="lbl">Offline left</span><span style="font-weight:700;text-align:right">₹1,520</span><span class="lbl">Status</span><span style="font-weight:700;text-align:right;color:var(--pend)">● Pending sync</span></div>
<div class="btn">Done</div><div class="btn gh">Share receipt</div>'''

def scr_merchant_pr():
    return f'''{banner("off","Offline · accepting payments")}
<div style="font-weight:800;font-size:18px;text-align:center">Show this to the customer</div>
{qr("pr-120",210)}
<div style="text-align:center"><div class="disp" style="font-size:40px;font-weight:700">₹120</div><div style="font-size:12.5px;color:var(--muted)">Request expires in 4:52</div></div>
<div class="btn" style="display:flex;gap:8px;justify-content:center;align-items:center">{I("scan-line")}Scan customer's code</div>
<div style="text-align:center;font-size:13px;font-weight:700;color:var(--muted)">Cancel request</div>'''

def scr_payer_voucher():
    return f'''{banner("off","Offline · payment signed on this phone")}
<div style="font-weight:800;font-size:18px;text-align:center">Show to Ravi General Store</div>
{qr("vc-42",210)}
<div style="text-align:center"><div class="disp" style="font-size:38px;font-weight:700">₹120</div><div style="font-size:12px;color:var(--muted)">Signed voucher #42 · keep screen bright</div></div>
<div class="btn gh" style="display:flex;gap:8px;justify-content:center;align-items:center">{I("scan-line")}Scan merchant receipt</div>
<div style="text-align:center;font-size:12.5px;font-weight:700;color:var(--muted)">Merchant didn't receive?</div>'''

def scr_merchant_ok():
    return f'''{banner("pend","Pending sync · 6 payments today")}
<div style="text-align:center;margin-top:10px"><div style="width:96px;height:96px;margin:0 auto;border-radius:50%;background:var(--ok);display:grid;place-items:center;color:#fff"><i data-lucide="check" style="width:58px;height:58px;stroke-width:3"></i></div>
<div style="font-weight:800;font-size:20px;margin-top:12px">Payment received</div><div class="disp" style="font-size:48px;font-weight:700">₹120</div>
<div style="font-size:12.5px;color:var(--muted)">from Priya S. · voucher verified offline</div></div>
{qr("rc-42",140)}
<div style="text-align:center;font-size:12px;color:var(--muted)">Customer scans this receipt · code <b class="mono">HP7-K2Q9</b></div>
<div class="btn">New payment</div>'''

def scr_merchant_today():
    bars = [38, 52, 30, 64, 80, 46, 72]
    return f'''{banner("ok","Online · last sync 2 min ago")}
<div style="font-weight:800;font-size:18px">Today's takings</div>
<div class="balcard" style="background:linear-gradient(135deg,#132033,#0B1420)"><small>Total accepted</small><div class="amt">₹8,420</div>
<div style="display:flex;gap:8px;margin-top:8px"><span class="tag" style="background:rgba(21,128,61,.3);color:#86EFAC;font-size:11px">Settled ₹7,180</span><span class="tag" style="background:rgba(180,83,9,.35);color:#FCD34D;font-size:11px">Pending ₹1,240</span></div></div>
<div class="mini"><div class="lbl" style="margin-bottom:8px">By hour</div><div style="display:flex;gap:8px;align-items:flex-end;height:70px">{''.join(f'<div style="flex:1;height:{b}px;border-radius:6px;background:{"var(--saff)" if b==80 else "var(--teal2)"}"></div>' for b in bars)}</div></div>
<div class="mini" style="display:flex;justify-content:space-between"><span><b>Priya S.</b><br><span style="color:var(--muted)">7:42 pm</span></span><span style="text-align:right"><b>₹120</b><br><span style="color:var(--pend);font-weight:700;font-size:11px">● Pending</span></span></div>
<div class="mini" style="display:flex;justify-content:space-between"><span><b>Arjun K.</b><br><span style="color:var(--muted)">6:10 pm</span></span><span style="text-align:right"><b>₹60</b><br><span style="color:var(--ok);font-weight:700;font-size:11px">● Settled</span></span></div>
<div class="btn gh">Export CSV</div>'''

def scr_scan():
    return f'''{banner("off","Offline · camera works without internet")}
<div style="flex:1;border-radius:24px;background:linear-gradient(160deg,#1B2A40,#0B1420);position:relative;display:grid;place-items:center">
<div style="width:200px;height:200px;border:4px solid var(--mint);border-radius:28px;box-shadow:0 0 0 999px rgba(0,0,0,.25) inset"></div>
<div style="position:absolute;left:50%;top:50%;width:170px;height:3px;background:var(--saff);transform:translate(-50%,-50%)"></div>
<div style="position:absolute;bottom:18px;left:0;right:0;text-align:center;color:#E8EEF5;font-size:13px;font-weight:700">Point at the merchant's QR</div></div>
<div style="display:flex;gap:10px"><div class="btn gh" style="flex:1">Torch</div><div class="btn gh" style="flex:1">Enter code</div></div>'''
