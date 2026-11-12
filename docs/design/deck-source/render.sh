#!/bin/bash
# usage: render.sh [slide-list]   -> deck.pdf, png/pg-NN.png, report.txt
set -e
cd "$(dirname "$0")"
python3 build.py "$1"
CH="google-chrome --headless=new --disable-gpu --no-sandbox --hide-scrollbars --virtual-time-budget=20000 --run-all-compositor-stages-before-draw"
$CH --dump-dom "file://$PWD/deck.html#check" 2>/dev/null | python3 -c "
import sys,re,html
d=sys.stdin.read();m=re.findall(r'<pre id=\"report\">(.*?)</pre>',d,re.S)
print(html.unescape(m[-1]) if m else 'NO REPORT (JS did not run)')" > report.txt
$CH --no-pdf-header-footer --print-to-pdf=deck.pdf "file://$PWD/deck.html" 2>/dev/null
rm -rf png && mkdir png && pdftoppm -r 48 -png deck.pdf png/pg
echo "--- report ---"; cat report.txt | head -80
pdfinfo deck.pdf | grep Pages
