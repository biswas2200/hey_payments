import sys, re, importlib, importlib.util
sys.path.insert(0, '.')
mods = [m for m in ['slides1', 'slides2', 'slides3'] if importlib.util.find_spec(m)]
fns = []
for m in mods:
    mod = importlib.import_module(m)
    fns += [getattr(mod, f) for f in dir(mod) if re.fullmatch(r's\d+[a-z]?', f)]
def key(f):
    m = re.fullmatch(r's(\d+)([a-z]?)', f.__name__); return (int(m.group(1)), m.group(2))
fns.sort(key=key)
only = set(int(x) for x in sys.argv[1].split(',')) if len(sys.argv) > 1 and sys.argv[1] else None
from parts import TOTAL
html_slides = []
for seq, f in enumerate(fns, 1):
    h = f()
    h = re.sub(r'data-n="\d+"', f'data-n="{seq}"', h, count=1)
    h = re.sub(r'<b>\d+/\d+</b>', f'<b>{seq:02d}/{TOTAL}</b>', h, count=1)
    if not only or seq in only: html_slides.append(h)
print('functions:', len(fns))

QRJS = r'''
function rng(s){let h=2166136261;for(const c of s){h^=c.charCodeAt(0);h=Math.imul(h,16777619)}return()=>{h^=h<<13;h^=h>>>17;h^=h<<5;return((h>>>0)%1000)/1000}}
document.querySelectorAll('[data-qr]').forEach(el=>{const N=29,sz=+el.dataset.size,r=rng(el.dataset.qr),m=sz/N;let s=`<svg width="${sz}" height="${sz}" viewBox="0 0 ${sz} ${sz}">`;
const fin=(x,y)=>{s+=`<rect x="${x*m}" y="${y*m}" width="${7*m}" height="${7*m}" fill="#0E1B2C" rx="${m*1.4}"/><rect x="${(x+1)*m}" y="${(y+1)*m}" width="${5*m}" height="${5*m}" fill="#fff" rx="${m}"/><rect x="${(x+2)*m}" y="${(y+2)*m}" width="${3*m}" height="${3*m}" fill="#0E1B2C" rx="${m*.6}"/>`};
for(let y=0;y<N;y++)for(let x=0;x<N;x++){if((x<8&&y<8)||(x>N-9&&y<8)||(x<8&&y>N-9))continue;if(r()>.52)s+=`<rect x="${x*m}" y="${y*m}" width="${m}" height="${m}" fill="#0E1B2C" rx="${m*.3}"/>`}
fin(0,0);fin(N-7,0);fin(0,N-7);s+=`<rect x="${sz/2-m*3}" y="${sz/2-m*3}" width="${m*6}" height="${m*6}" fill="#F5A524" rx="${m*1.5}"/></svg>`;el.innerHTML=s});
'''
CHECK = r'''
function report(){const out=[];document.querySelectorAll('.slide').forEach(sl=>{const n=sl.dataset.n,R=sl.getBoundingClientRect();
 const body=sl.querySelector('.body'),ref=sl.querySelector('.ref');
 sl.querySelectorAll('*').forEach(e=>{if(e.closest('.phone')&&!e.classList.contains('phone'))return;const r=e.getBoundingClientRect();if(!r.width||!r.height)return;
  const cs=getComputedStyle(e);
  if((e.scrollHeight>e.clientHeight+3&&cs.overflow!=='visible'&&!e.classList.contains('slide')&&!e.classList.contains('scr'))) out.push(`S${n} clip <${e.tagName.toLowerCase()} class="${e.className}"> ${e.scrollHeight}>${e.clientHeight} "${(e.textContent||'').trim().slice(0,40)}"`);
  if(body&&body.contains(e)&&e!==body){const b=body.getBoundingClientRect(); if(r.bottom>b.bottom+4||r.right>b.right+4) out.push(`S${n} overflow-body <${e.tagName.toLowerCase()} class="${e.className}"> by ${Math.round(Math.max(r.bottom-b.bottom,r.right-b.right))}px "${(e.textContent||'').trim().slice(0,40)}"`)}
  if(r.right>R.right+2||r.bottom>R.bottom+2){ if(!e.closest('.blob2')&&!getComputedStyle(e).position.includes('fixed')&&!e.classList.contains('blob2')&&!e.closest('.divnum')) out.push(`S${n} off-slide <${e.tagName.toLowerCase()} class="${e.className}">`)}
 });
 sl.querySelectorAll('i[data-lucide]').forEach(i=>out.push(`S${n} missing icon ${i.dataset.lucide}`));
 sl.querySelectorAll('.card').forEach(c=>{if(c.closest('.phone'))return;const cr=c.getBoundingClientRect();let mb=0;c.querySelectorAll(':scope > *').forEach(k=>{const kr=k.getBoundingClientRect();if(kr.height)mb=Math.max(mb,kr.bottom)});const g=cr.bottom-mb-parseFloat(getComputedStyle(c).paddingBottom);if(g>90)out.push(`S${n} card-gap ${Math.round(g)}px "${c.textContent.trim().slice(0,30)}"`)});
 if(body){const b=body.getBoundingClientRect();let maxB=0;body.querySelectorAll(':scope > *').forEach(c=>{maxB=Math.max(maxB,c.getBoundingClientRect().bottom)});const gap=b.bottom-maxB; if(gap>60) out.push(`S${n} whitespace ${Math.round(gap)}px below content`)}
});document.body.insertAdjacentHTML('beforeend','<pre id="report">'+out.join('\n').replace(/</g,'&lt;')+'</pre>')}
'''
html = f'''<!doctype html><html><head><meta charset="utf-8"><title>Hey Payments UI/UX Case Study</title>
<link rel="preconnect" href="https://fonts.googleapis.com"><link href="https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&family=Inter:ital,wght@0,400;0,500;0,600;0,700;0,800;1,500;1,600&family=JetBrains+Mono:wght@500;700&family=Caveat:wght@600;700&display=swap" rel="stylesheet">
<link rel="stylesheet" href="style.css"><script src="https://unpkg.com/lucide@0.460.0/dist/umd/lucide.min.js"></script></head><body>
{''.join(html_slides)}
<script>{QRJS}
lucide.createIcons();
{CHECK}
if(location.hash==='#check'){{document.fonts.ready.then(()=>setTimeout(report,300))}}
</script></body></html>'''
open('deck.html', 'w').write(html)
print(len(html_slides), 'slides')
