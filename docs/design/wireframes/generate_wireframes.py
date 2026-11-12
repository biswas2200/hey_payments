"""Generate Hey Payments wireframes as Excalidraw elements.
Outputs: mcp.json (create_view format, with labels + cameras) and hey-payments-wireframes.excalidraw (native file)."""
import json, random

INK = "#1e1e1e"; MUTED = "#757575"; TEAL = "#0f766e"; AMBER = "#f59e0b"
F_TEAL = "#c3fae8"; F_AMB = "#ffd8a8"; F_GREY = "#e9ecef"; F_BLUE = "#d0bfff"; F_GREEN = "#b2f2bb"; F_RED = "#ffc9c9"
els = []
_id = [0]
def nid(p="e"):
    _id[0] += 1; return f"{p}{_id[0]}"

def cam(x, y, w, h):
    els.append({"type": "cameraUpdate", "x": x, "y": y, "width": w, "height": h})

def rect(x, y, w, h, label=None, fill=None, stroke=INK, fs=16, rounded=True, sw=2, dashed=False, op=100):
    e = {"type": "rectangle", "id": nid("r"), "x": x, "y": y, "width": w, "height": h, "strokeColor": stroke, "strokeWidth": sw}
    if rounded: e["roundness"] = {"type": 3}
    if fill: e["backgroundColor"] = fill; e["fillStyle"] = "solid"
    if dashed: e["strokeStyle"] = "dashed"
    if op != 100: e["opacity"] = op
    if label: e["label"] = {"text": label, "fontSize": fs}
    els.append(e); return e

def ell(x, y, w, h, label=None, fill=None, stroke=INK, fs=16):
    e = {"type": "ellipse", "id": nid("o"), "x": x, "y": y, "width": w, "height": h, "strokeColor": stroke}
    if fill: e["backgroundColor"] = fill; e["fillStyle"] = "solid"
    if label: e["label"] = {"text": label, "fontSize": fs}
    els.append(e); return e

def text(x, y, t, fs=16, color=INK):
    e = {"type": "text", "id": nid("t"), "x": x, "y": y, "text": t, "fontSize": fs, "strokeColor": color}
    els.append(e); return e

def arrow(x, y, dx, dy, label=None, color=TEAL, dashed=False):
    e = {"type": "arrow", "id": nid("a"), "x": x, "y": y, "width": abs(dx), "height": abs(dy), "points": [[0, 0], [dx, dy]],
         "strokeColor": color, "strokeWidth": 2, "endArrowhead": "arrow"}
    if dashed: e["strokeStyle"] = "dashed"
    if label: e["label"] = {"text": label, "fontSize": 16}
    els.append(e); return e

def line(x, y, dx, dy, color=MUTED, sw=1):
    els.append({"type": "arrow", "id": nid("l"), "x": x, "y": y, "width": abs(dx), "height": abs(dy), "points": [[0, 0], [dx, dy]],
                "strokeColor": color, "strokeWidth": sw, "endArrowhead": None, "startArrowhead": None})

W, H, GAP = 200, 400, 70

def phone(x, y, title, num):
    rect(x, y, W, H, fill="#ffffff", sw=3)
    rect(x + 70, y + 10, 60, 12, fill=INK, rounded=True, sw=1)
    text(x, y - 34, f"{num}  {title}", 20, INK)

def banner(x, y, t, fill):
    rect(x + 12, y + 32, W - 24, 30, t, fill=fill, fs=14, stroke=MUTED, sw=1)

def note(x, y, t):
    text(x, y + H + 14, t, 16, MUTED)

# ---------------- title ----------------
cam(300, -230, 800, 600)
text(330, -190, "Hey Payments  |  Wireframes (mid-fidelity)", 32, INK)
text(330, -146, "Development reference for the Angular PWA. Greyscale = structure, colour = state only.", 18, MUTED)
text(330, -116, "Status language:  Offline (purple)  |  Pending sync (orange)  |  Settled (green)", 18, TEAL)

# ---------------- payer row ----------------
Y1 = 60
text(-10, Y1 - 85, "PAYER APP  (mobile, 360-430 px)", 22, TEAL)
cam(-40, -40, 800, 600)
X = [0 + i * (W + GAP) for i in range(6)]
# P1 Home
x = X[0]; phone(x, Y1, "Home", "P1"); banner(x, Y1, "Offline - 2 pending sync", F_BLUE)
text(x + 14, Y1 + 72, "Hi, Priya", 16)
rect(x + 12, Y1 + 98, W - 24, 78, "Offline balance\nRs 1,640 / 2,000", fill=F_TEAL, fs=16, stroke=TEAL)
rect(x + 12, Y1 + 186, W - 24, 36, "Main wallet  Rs 4,250", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
rect(x + 12, Y1 + 232, W - 24, 44, "Pay without internet", fill=F_AMB, fs=16, stroke=AMBER)
text(x + 14, Y1 + 288, "Recent", 14, MUTED)
rect(x + 12, Y1 + 308, W - 24, 34, "Ravi Store  -120  pending", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
rect(x + 12, Y1 + 350, W - 24, 34, "Chai Point  -40  settled", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
note(x, Y1, "Balance card + CTA above fold")
arrow(x + W + 4, Y1 + 254, GAP - 8, 0, "tap")
# P2 Load
x = X[1]; phone(x, Y1, "Load offline", "P2"); banner(x, Y1, "Online - synced", F_GREEN)
text(x + 14, Y1 + 72, "Load offline balance", 16)
text(x + 66, Y1 + 100, "Rs 500", 28)
text(x + 22, Y1 + 140, "max 2,000 | 500 per pay", 14, MUTED)
for i, c in enumerate(["200", "500", "1000"]):
    rect(x + 14 + i * 60, Y1 + 166, 52, 28, c, fill=F_TEAL if c == "500" else F_GREY, fs=14, stroke=MUTED, sw=1)
rect(x + 12, Y1 + 206, W - 24, 128, "Numeric keypad\n(44 px keys)", fill=F_GREY, fs=16, stroke=MUTED, sw=1)
rect(x + 12, Y1 + 344, W - 24, 42, "Load Rs 500", fill=F_TEAL, fs=16, stroke=TEAL)
note(x, Y1, "Requires sync first (409)")
arrow(x + W + 4, Y1 + 200, GAP - 8, 0)
# P3 Scan
cam(500, -40, 800, 600)
x = X[2]; phone(x, Y1, "Scan merchant QR", "P3"); banner(x, Y1, "Offline - camera works", F_BLUE)
rect(x + 12, Y1 + 72, W - 24, 250, fill="#495057", stroke=INK)
rect(x + 45, Y1 + 122, 110, 110, stroke="#2dd4bf", sw=3)
text(x + 30, Y1 + 285, "Point at merchant's QR", 14, "#ffffff")
rect(x + 12, Y1 + 336, 80, 44, "Torch", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
rect(x + 104, Y1 + 336, 84, 44, "Enter code", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
note(x, Y1, "Verify merchant sig offline")
arrow(x + W + 4, Y1 + 200, GAP - 8, 0, "valid")
# P4 Confirm
x = X[3]; phone(x, Y1, "Confirm + PIN", "P4"); banner(x, Y1, "Offline - no internet needed", F_BLUE)
rect(x + 12, Y1 + 72, W - 24, 46, "Ravi General Store\n(verified merchant)", fill=F_GREEN, fs=14, stroke="#15803d", sw=1)
text(x + 72, Y1 + 128, "Rs 120", 28)
text(x + 50, Y1 + 168, "Enter payment PIN", 14, MUTED)
text(x + 60, Y1 + 188, "o o o o _ _", 18)
rect(x + 12, Y1 + 216, W - 24, 132, "PIN keypad\n6 digits, 44 px", fill=F_GREY, fs=16, stroke=MUTED, sw=1)
text(x + 76, Y1 + 358, "Cancel", 16, MUTED)
note(x, Y1, "Who + how much before PIN")
arrow(x + W + 4, Y1 + 200, GAP - 8, 0, "PIN ok")
# P5 Voucher
cam(1000, -40, 800, 600)
x = X[4]; phone(x, Y1, "Show voucher", "P5"); banner(x, Y1, "Offline - signed on phone", F_BLUE)
rect(x + 30, Y1 + 74, 140, 140, "Signed\nvoucher QR", fill="#ffffff", fs=16, stroke=INK, sw=3)
text(x + 72, Y1 + 224, "Rs 120", 26)
text(x + 30, Y1 + 260, "keep screen bright", 14, MUTED)
rect(x + 12, Y1 + 288, W - 24, 42, "Scan merchant receipt", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
text(x + 22, Y1 + 346, "Merchant didn't receive?", 14, TEAL)
note(x, Y1, "Saved before QR is shown")
arrow(x + W + 4, Y1 + 200, GAP - 8, 0, "receipt")
# P6 Receipt
x = X[5]; phone(x, Y1, "Receipt", "P6"); banner(x, Y1, "Pending sync", F_AMB)
ell(x + 62, Y1 + 76, 76, 76, "OK", fill=F_GREEN, stroke="#15803d", fs=20)
text(x + 30, Y1 + 162, "Payment received", 18)
text(x + 72, Y1 + 190, "Rs 120", 26)
rect(x + 12, Y1 + 232, W - 24, 70, "Code HP7-K2Q9\nOffline left Rs 1,520", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
rect(x + 12, Y1 + 312, W - 24, 34, "Done", fill=F_TEAL, fs=16, stroke=TEAL)
rect(x + 12, Y1 + 354, W - 24, 32, "Share receipt", fs=14, stroke=MUTED, sw=1)
note(x, Y1, "Never 'success' before proof")

# ---------------- merchant row ----------------
Y2 = 620
cam(-40, 500, 800, 600)
text(-10, Y2 - 85, "MERCHANT POS  (mobile)", 22, "#b45309")
# M1 Amount
x = X[0]; phone(x, Y2, "Enter amount", "M1"); banner(x, Y2, "Offline - accepting", F_BLUE)
text(x + 66, Y2 + 80, "Rs 120", 28)
rect(x + 12, Y2 + 120, W - 24, 46, "Note: 2 chai, 1 samosa", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
rect(x + 12, Y2 + 176, W - 24, 150, "Amount keypad", fill=F_GREY, fs=16, stroke=MUTED, sw=1)
rect(x + 12, Y2 + 338, W - 24, 46, "Show QR", fill=F_TEAL, fs=16, stroke=TEAL)
note(x, Y2, "2 taps from open to QR")
arrow(x + W + 4, Y2 + 200, GAP - 8, 0)
# M2 Request QR
x = X[1]; phone(x, Y2, "Payment request", "M2"); banner(x, Y2, "Offline - accepting", F_BLUE)
rect(x + 30, Y2 + 74, 140, 140, "Signed\nrequest QR", fill="#ffffff", fs=16, stroke=INK, sw=3)
text(x + 72, Y2 + 224, "Rs 120", 26)
text(x + 44, Y2 + 260, "expires in 4:52", 14, MUTED)
rect(x + 12, Y2 + 288, W - 24, 44, "Scan customer's code", fill=F_TEAL, fs=14, stroke=TEAL)
text(x + 54, Y2 + 348, "Cancel request", 14, MUTED)
note(x, Y2, "One voucher per request")
arrow(x + W + 4, Y2 + 200, GAP - 8, 0, "verified")
# M3 Received
cam(500, 500, 800, 600)
x = X[2]; phone(x, Y2, "Payment received", "M3"); banner(x, Y2, "Pending sync", F_AMB)
ell(x + 58, Y2 + 74, 84, 84, "OK", fill=F_GREEN, stroke="#15803d", fs=22)
text(x + 30, Y2 + 168, "Payment received", 18)
text(x + 72, Y2 + 196, "Rs 120", 26)
rect(x + 55, Y2 + 234, 90, 90, "Receipt\nQR", fill="#ffffff", fs=14, stroke=INK, sw=2)
rect(x + 12, Y2 + 340, W - 24, 44, "New payment", fill=F_TEAL, fs=16, stroke=TEAL)
note(x, Y2, "Chime + haptic + big tick")
arrow(x + W + 4, Y2 + 200, GAP - 8, 0, "later")
# M4 Today
x = X[3]; phone(x, Y2, "Today's takings", "M4"); banner(x, Y2, "Online - synced", F_GREEN)
rect(x + 12, Y2 + 72, W - 24, 80, "Rs 8,420 today\nSettled 7,180 | Pending 1,240", fill="#343a40", fs=14, stroke=INK)
rect(x + 12, Y2 + 162, W - 24, 80, "Takings by hour (bars)", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
rect(x + 12, Y2 + 252, W - 24, 34, "Priya S.  120  pending", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
rect(x + 12, Y2 + 294, W - 24, 34, "Arjun K.  60  settled", fill=F_GREY, fs=14, stroke=MUTED, sw=1)
rect(x + 12, Y2 + 340, W - 24, 42, "Export CSV", fs=14, stroke=MUTED, sw=1)
note(x, Y2, "Pending vs settled split")
# fix dark card text colour later via label colour: keep light text by using strokeColor? (label inherits strokeColor)
els[-6]["strokeColor"] = "#f8f9fa"

# ---------------- admin desktop ----------------
cam(1000, 500, 800, 600)
AX, AW, AH = X[4] - 20, 500, 400
text(AX, Y2 - 85, "ADMIN CONSOLE  (desktop, 1200+ px)", 22, "#4338ca")
rect(AX, Y2, AW, AH, fill="#ffffff", sw=3)
text(AX, Y2 - 34, "A1  Dashboard", 20)
rect(AX + 10, Y2 + 10, 120, AH - 20, "Sidebar\nDashboard\nUsers\nDevices\nLedger\nFraud cases\nLimits", fill="#343a40", fs=14, stroke=INK)
els[-1]["strokeColor"] = "#f8f9fa"
for i, (t, f) in enumerate([("GMV\n12.4L", F_GREY), ("Payments\n18,420", F_GREY), ("Escrow\n1.9L", F_AMB), ("Fraud\n3 open", F_RED)]):
    rect(AX + 140 + i * 88, Y2 + 14, 82, 64, t, fill=f, fs=14, stroke=MUTED, sw=1)
rect(AX + 140, Y2 + 92, 180, 290, "Settled\nvolume\n14-day chart", fill=F_TEAL, fs=16, stroke=TEAL, sw=1)
rect(AX + 330, Y2 + 92, 160, 290, "Fraud cases\nHIGH double spend\nMED anomaly\nLOW stale sync\n[Review] [Freeze]", fill=F_RED, fs=14, stroke="#b91c1c", sw=1)
text(AX, Y2 + AH + 14, "One-click freeze, every action audited", 16, MUTED)

# cross-row flow: payer voucher <-> merchant (dashed)
arrow(X[4] + W / 2, Y1 + H + 44, -(X[4] - X[2]) - 20, Y2 - Y1 - H - 90, "voucher QR (offline)", AMBER, dashed=True)

# final overview
cam(-30, -230, 1600, 1200)

mcp = json.dumps(els, separators=(",", ":"))
open("mcp.json", "w").write(mcp)

# ---------- native .excalidraw ----------
rnd = random.Random(7)
native = []
def base(e):
    d = {k: v for k, v in e.items() if k not in ("label",)}
    d.setdefault("strokeColor", INK); d.setdefault("backgroundColor", "transparent"); d.setdefault("fillStyle", "solid")
    d.setdefault("strokeWidth", 2); d.setdefault("strokeStyle", "solid"); d.setdefault("roughness", 1); d.setdefault("opacity", 100)
    d.update({"angle": 0, "seed": rnd.randint(1, 2**30), "version": 1, "versionNonce": rnd.randint(1, 2**30), "isDeleted": False,
              "groupIds": [], "frameId": None, "boundElements": [], "updated": 1, "link": None, "locked": False})
    if d["type"] == "rectangle" and "roundness" not in d: d["roundness"] = None
    if d["type"] in ("ellipse",): d.setdefault("roundness", {"type": 2})
    if d["type"] == "arrow": d.setdefault("roundness", {"type": 2}); d.setdefault("startArrowhead", None); d.setdefault("startBinding", None); d.setdefault("endBinding", None); d.setdefault("lastCommittedPoint", None)
    return d
def mktext(t, fs, x, y, w, h, color, container=None):
    lines = t.split("\n")
    return {"type": "text", "id": nid("tx"), "x": x, "y": y, "width": w, "height": h, "text": t, "originalText": t, "fontSize": fs, "fontFamily": 1,
            "textAlign": "center" if container else "left", "verticalAlign": "middle" if container else "top", "containerId": container,
            "lineHeight": 1.25, "autoResize": True, "strokeColor": color, "backgroundColor": "transparent", "fillStyle": "solid", "strokeWidth": 1,
            "strokeStyle": "solid", "roughness": 1, "opacity": 100, "angle": 0, "seed": rnd.randint(1, 2**30), "version": 1,
            "versionNonce": rnd.randint(1, 2**30), "isDeleted": False, "groupIds": [], "frameId": None, "boundElements": [], "updated": 1,
            "link": None, "locked": False, "roundness": None}
for e in els:
    if e["type"] == "cameraUpdate": continue
    if e["type"] == "text":
        t = e["text"]; fs = e["fontSize"]; lines = t.split("\n")
        native.append(mktext(t, fs, e["x"], e["y"], max(len(l) for l in lines) * fs * 0.55, len(lines) * fs * 1.25, e.get("strokeColor", INK)))
        continue
    d = base(e); native.append(d)
    if "label" in e:
        t = e["label"]["text"]; fs = e["label"]["fontSize"]; lines = t.split("\n")
        tw = max(len(l) for l in lines) * fs * 0.55; th = len(lines) * fs * 1.25
        if e["type"] == "arrow":
            cx = e["x"] + e["points"][1][0] / 2; cy = e["y"] + e["points"][1][1] / 2
        else:
            cx = e["x"] + e["width"] / 2; cy = e["y"] + e["height"] / 2
        tx = mktext(t, fs, cx - tw / 2, cy - th / 2, tw, th, e.get("strokeColor", INK), container=d["id"])
        d["boundElements"].append({"type": "text", "id": tx["id"]})
        native.append(tx)
doc = {"type": "excalidraw", "version": 2, "source": "https://excalidraw.com", "elements": native,
       "appState": {"viewBackgroundColor": "#ffffff", "gridSize": None}, "files": {}}
open("hey-payments-wireframes.excalidraw", "w").write(json.dumps(doc, indent=1))
print(len(els), "mcp elements;", len(native), "native elements;", len(mcp), "chars")
