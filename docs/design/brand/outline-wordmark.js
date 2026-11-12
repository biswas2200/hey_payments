const opentype = require('opentype.js');
const fs = require('fs');
const [,, src, out, color] = process.argv;
const font = opentype.loadSync('space-grotesk-latin-700-normal.woff');
const size = 32, y = 43;
let x = 80;
const segs = [['hey', color], ['.', '#F5A524'], ['payments', color]];
const paths = [];
for (const [txt, fill] of segs) {
  const p = font.getPath(txt, x, y, size, { kerning: true });
  paths.push(`<path fill="${fill}" d="${p.toPathData(2)}"/>`);
  x += font.getAdvanceWidth(txt, size, { kerning: true });
}
let svg = fs.readFileSync(src, 'utf8');
svg = svg.replace(/<text[\s\S]*<\/text>/, `<g aria-hidden="true">${paths.join('')}</g>`);
fs.writeFileSync(out, svg);
console.log(out, 'text end x =', x.toFixed(1), 'bytes', svg.length);
