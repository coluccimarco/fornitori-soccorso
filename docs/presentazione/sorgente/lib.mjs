// Stile e componenti comuni delle slide. I colori vengono da src/main/resources/static (styles.css, public-site.css).
export const C = {
  blue: '#174995', blueDark: '#164a9c', yellow: '#f2bf00', indigo: '#5664d9', teal: '#2bb392',
  ink: '#20283a', muted: '#5b6478', line: '#e9edf3', canvas: '#f5f7fb', violet: '#6d28d9', white: '#ffffff',
};

export const MEMBRI = {
  m1: 'Membro 1 · Vincenzo Minervini',
  m2: 'Membro 2 · Francesco Valeriano, Giuseppe Cangellosi, Marika Sancilio',
  m3: 'Membro 3 · Marco Colucci',
  m4: 'Membro 4 · Marco Colucci',
  m5: 'Membro 5 · Marika Sancilio, Francesco Valeriano, Giuseppe Cangellosi',
  team: 'Tutto il team',
};

export const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
/** Mini markdown delle note: `codice` e **grassetto**. */
export const md = (s) => esc(s).replace(/`([^`]+)`/g, '<code>$1</code>').replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');

export const logo = (fill = C.yellow, cut = '#fff') => `<svg class="logo" viewBox="0 0 42 48" aria-hidden="true"><path fill="${fill}" d="M27 3 9 10 5 17l18 1-12 8 4 7 15-7-2 17 7-2 4-24-8-7 6-4-3-7-8 4-2-7Z"/><path fill="${cut}" d="m23 10-5 8 8 1-7 8 4 2 9-11-8-2 4-7Z"/></svg>`;

export const pts = (items, cls = '') => `<ul class="pts ${cls}">${items.map((i) => `<li>${i}</li>`).join('')}</ul>`;
export const shot = (name, cap = '', style = '') => `<figure class="shot" style="${style}"><img src="../img/${name}" alt="${esc(cap || name)}">${cap ? `<figcaption>${cap}</figcaption>` : ''}</figure>`;
export const card = (t, body, color = C.blue) => `<div class="card" style="--c:${color}"><h3>${t}</h3><div>${body}</div></div>`;
export const two = (a, b, cols = '1fr 1fr') => `<div class="two" style="grid-template-columns:${cols}">${a}${b}</div>`;
export const three = (a, b, c) => `<div class="three">${a}${b}${c}</div>`;
export const motivi = (label, items) => `<div class="motivi"><h3>${label}</h3>${pts(items)}</div>`;

/* ---------- diagrammi SVG ---------- */
const palette = {
  blue: [C.blue, '#fff'], indigo: [C.indigo, '#fff'], yellow: [C.yellow, C.ink], teal: [C.teal, '#0d3b30'],
  ink: [C.ink, '#fff'], violet: [C.violet, '#fff'],
  light: ['#fff', C.ink], soft: [C.canvas, C.ink],
};
export function box(x, y, w, h, title, sub = [], color = 'light', opts = {}) {
  const [bg, fg] = palette[color];
  const lines = Array.isArray(sub) ? sub : [sub];
  const ts = opts.ts ?? 21, ss = opts.ss ?? 16.5, lh = ss * 1.28;
  const total = ts + (lines.length ? 6 + lines.length * lh : 0);
  const y0 = y + (h - total) / 2 + ts * 0.82;
  const stroke = color === 'light' || color === 'soft' ? `stroke="${opts.stroke ?? '#c6cedb'}" stroke-width="1.6"` : '';
  return `<g><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="12" fill="${bg}" ${stroke}/>`
    + `<text x="${x + w / 2}" y="${y0}" text-anchor="middle" font-size="${ts}" font-weight="700" fill="${fg}">${esc(title)}</text>`
    + lines.map((l, i) => `<text x="${x + w / 2}" y="${y0 + 6 + (i + 1) * lh - 2}" text-anchor="middle" font-size="${ss}" font-weight="500" fill="${fg}">${esc(l)}</text>`).join('')
    + '</g>';
}
export function arrow(x1, y1, x2, y2, label = '', opts = {}) {
  const col = opts.color ?? C.ink;
  const dash = opts.dash ? 'stroke-dasharray="7 6"' : '';
  const ang = Math.atan2(y2 - y1, x2 - x1), s = 11;
  const tip = (x, y, a) => `<path d="M${x} ${y} L${x - s * Math.cos(a - 0.45)} ${y - s * Math.sin(a - 0.45)} L${x - s * Math.cos(a + 0.45)} ${y - s * Math.sin(a + 0.45)} Z" fill="${col}"/>`;
  const lx = (x1 + x2) / 2 + (opts.dx ?? 0), ly = (y1 + y2) / 2 + (opts.dy ?? -8);
  const ls = opts.ls ?? 17;
  const lbl = label ? (Array.isArray(label) ? label : [label]).map((t, i) => `<text x="${lx}" y="${ly + i * (ls + 3)}" text-anchor="${opts.anchor ?? 'middle'}" font-size="${ls}" font-weight="600" fill="${opts.lc ?? C.ink}">${esc(t)}</text>`).join('') : '';
  return `<g><line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${col}" stroke-width="2.4" ${dash}/>${tip(x2, y2, ang)}${opts.both ? tip(x1, y1, ang + Math.PI) : ''}${lbl}</g>`;
}
export const txt = (x, y, t, o = {}) => `<text x="${x}" y="${y}" text-anchor="${o.a ?? 'middle'}" font-size="${o.s ?? 17}" font-weight="${o.w ?? 500}" fill="${o.c ?? C.muted}">${esc(t)}</text>`;
export const zone = (x, y, w, h, label, col = C.blue) => `<g><rect x="${x}" y="${y}" width="${w}" height="${h}" rx="14" fill="none" stroke="${col}" stroke-width="1.8" stroke-dasharray="6 6"/><text x="${x + 14}" y="${y + 23}" font-size="15.5" font-weight="700" fill="${col}">${esc(label)}</text></g>`;
export const svg = (w, h, inner) => `<svg class="dia" viewBox="0 0 ${w} ${h}" width="${w}" height="${h}">${inner}</svg>`;

/* ---------- foglio di stile ---------- */
export const css = (fontUrl) => `
@font-face{font-family:Inter;font-weight:400;src:url(${fontUrl('Inter-Regular')})}
@font-face{font-family:Inter;font-weight:400;font-style:italic;src:url(${fontUrl('Inter-Italic')})}
@font-face{font-family:Inter;font-weight:500;src:url(${fontUrl('Inter-Medium')})}
@font-face{font-family:Inter;font-weight:600;src:url(${fontUrl('Inter-SemiBold')})}
@font-face{font-family:Inter;font-weight:700;src:url(${fontUrl('Inter-Bold')})}
@font-face{font-family:Inter;font-weight:800;src:url(${fontUrl('Inter-ExtraBold')})}
@font-face{font-family:JetBrains;font-weight:400;src:url(${fontUrl('JetBrainsMono-Regular')})}
*{box-sizing:border-box}
html,body{margin:0;padding:0;background:#fff}
body{font-family:Inter,sans-serif;color:${C.ink};-webkit-print-color-adjust:exact;print-color-adjust:exact}
svg text{font-family:Inter,sans-serif}
.slide{position:relative;width:1280px;height:720px;overflow:hidden;background:#fff;padding:32px 54px 0;page-break-after:always;break-after:page;border-top:8px solid ${C.yellow}}
.kicker{font-size:17px;font-weight:700;color:${C.blue};letter-spacing:.04em;text-transform:uppercase;margin-bottom:4px}
.slide h1{font-size:39px;line-height:1.14;font-weight:800;margin:0 0 18px;color:${C.ink};letter-spacing:-.01em}
.body{position:absolute;left:54px;right:54px;top:118px;bottom:52px;display:flex;flex-direction:column;justify-content:center}
.foot{position:absolute;left:0;right:0;bottom:0;height:44px;display:flex;align-items:center;gap:9px;padding:0 54px;font-size:15px;color:${C.muted};border-top:1px solid ${C.line}}
.foot b{color:${C.blue};font-weight:800;font-size:17px}
.foot .sec{margin-left:12px}.foot .num{margin-left:auto;font-weight:700;font-size:17px;color:${C.ink}}
.logo{width:20px;height:24px;display:block}
ul.pts{list-style:none;margin:0;padding:0}
ul.pts li{position:relative;padding-left:29px;font-size:28px;line-height:1.27;font-weight:500;margin-bottom:21px}
ul.pts li::before{content:"";position:absolute;left:0;top:11px;width:14px;height:14px;border-radius:3px;background:${C.yellow}}
ul.pts.small li{font-size:23.5px;margin-bottom:14px;padding-left:25px}ul.pts.small li::before{top:8px;width:12px;height:12px}
.two{display:grid;gap:36px;align-items:center;width:100%}
.three{display:grid;grid-template-columns:1fr 1fr 1fr;gap:24px;width:100%}
.center{display:flex;justify-content:center;align-items:center;height:100%}
.card{background:${C.canvas};border-radius:14px;padding:20px 22px;border-top:6px solid var(--c)}
.card h3{margin:0 0 10px;font-size:25px;font-weight:800;color:${C.ink}}
.card div{font-size:22.5px;line-height:1.3;font-weight:500}
.card .pts li{font-size:23px;margin-bottom:11px;padding-left:22px}.card .pts li::before{top:9px;width:11px;height:11px}
.motivi{background:${C.canvas};border-left:6px solid ${C.blue};border-radius:10px;padding:16px 20px 7px}
.motivi h3{margin:0 0 11px;font-size:23px;color:${C.blue};font-weight:800}
.motivi .pts li{font-size:23.5px;margin-bottom:11px;padding-left:22px}.motivi .pts li::before{top:9px;width:11px;height:11px}
figure.shot{margin:0;text-align:center}
figure.shot img{display:block;max-width:100%;max-height:var(--mh,500px);margin:0 auto;border-radius:10px;border:1px solid #d3dae5;box-shadow:0 8px 22px rgba(31,42,74,.13)}
figure.shot figcaption{font-size:17.5px;font-weight:600;color:${C.muted};margin-top:8px}
table.t{width:100%;border-collapse:collapse;font-size:21px}
table.t th{background:${C.blue};color:#fff;text-align:left;padding:12px 14px;font-weight:700}
table.t td{padding:12px 14px;border-bottom:1px solid ${C.line};vertical-align:top;font-weight:500;line-height:1.24}
table.t tr:nth-child(even) td{background:${C.canvas}}
table.t td:first-child{font-weight:800;color:${C.blue}}
.big{font-size:27px;line-height:1.3;font-weight:600}
.pill{display:inline-block;background:${C.yellow};color:${C.ink};font-weight:800;padding:3px 13px;border-radius:99px;font-size:19px}
.note-s{font-size:19px;color:${C.muted};font-weight:500;line-height:1.3}
.dia{display:block;margin:0 auto;max-width:100%}
/* divisorie, tutte uguali */
.slide.div{background:${C.blue};color:#fff;border-top-color:${C.yellow}}
.div .inner{position:absolute;left:84px;right:84px;top:118px}
.div .mem{display:inline-block;background:${C.yellow};color:${C.ink};font-size:25px;font-weight:800;padding:6px 20px;border-radius:9px}
.div h1{color:#fff;font-size:78px;margin:24px 0 14px}
.div .who{font-size:32px;font-weight:600;line-height:1.3}
.div .top{font-size:23px;font-weight:500;margin-top:36px;opacity:.93;line-height:1.35;max-width:1040px}
.div .foot{border-top-color:rgba(255,255,255,.25);color:#fff}.div .foot b,.div .foot .num{color:#fff}
/* copertina */
.cover{background:${C.blue};color:#fff}
.cover .brand{display:flex;align-items:center;gap:16px;font-size:64px;font-weight:800;margin-top:42px}
.cover .brand .logo{width:58px;height:66px}
.cover h2{font-size:33px;font-weight:600;margin:16px 0 22px}
.cover .meta{font-size:22px;font-weight:500;line-height:1.5}
.cover .team{position:absolute;left:54px;bottom:70px;font-size:20px;font-weight:500;line-height:1.5;width:640px}
.cover .team b{color:${C.yellow}}
.cover .art{position:absolute;right:30px;bottom:60px;width:560px;background:#fff;border-radius:18px;padding:10px}
.cover .art img{width:100%;display:block}
.cover .foot{border-top-color:rgba(255,255,255,.25);color:#fff}.cover .foot b,.cover .foot .num{color:#fff}
/* guida di studio */
.gpage{width:210mm;height:297mm;padding:9mm 11mm 8mm;overflow:hidden;page-break-after:always;break-after:page;display:flex;flex-direction:column}
.ghead{display:flex;align-items:baseline;gap:10px;border-bottom:3px solid ${C.yellow};padding-bottom:4px;margin-bottom:6px;font-size:11px;font-weight:600;color:${C.muted}}
.ghead .gs{color:${C.blue};font-weight:800;font-size:13px}.ghead .gn{margin-left:auto;font-weight:800;color:${C.ink};font-size:12px;white-space:nowrap}
.gslide{width:640px;height:360px;margin:0 auto 7px;overflow:hidden;outline:1px solid #c6cedb;flex:none}
.gslide .slide{zoom:.5;page-break-after:auto;break-after:auto}
.notes{flex:1;min-height:0;column-count:2;column-gap:16px;column-fill:auto;font-size:var(--nf,12.2px);line-height:1.34;overflow:hidden}
.notes h4{margin:7px 0 2px;font-size:1em;font-weight:800;color:${C.blue};break-after:avoid}
.notes h4:first-child{margin-top:0}
.notes p{margin:0 0 3px}
.notes code{font-weight:400;font-family:JetBrains,monospace;font-size:.86em;background:${C.canvas};padding:0 2px;border-radius:2px;overflow-wrap:anywhere}
.notes .q{font-weight:700;margin-top:3px}
.notes ul{margin:0 0 3px;padding-left:13px}
.gapp{width:210mm;height:297mm;overflow:hidden;padding:10mm 12mm;page-break-after:always;break-after:page;font-size:13.4px;line-height:1.36}
.gapp h1{font-size:23px;font-weight:800;margin:0 0 4px;color:${C.blue}}
.gapp h2{font-size:15.5px;font-weight:800;margin:12px 0 3px;color:${C.ink};border-bottom:2px solid ${C.yellow};padding-bottom:2px;break-after:avoid}
.gapp p{margin:0 0 4px}.gapp .q{font-weight:700;margin-top:6px;break-after:avoid}
.gapp .intro{color:${C.muted};font-weight:500;margin-bottom:7px}
.gapp dl{margin:0;column-count:2;column-gap:18px;font-size:11.8px;line-height:1.3}.gapp dt{font-weight:800;margin-top:6px;color:${C.blue};break-after:avoid}.gapp dd{margin:0 0 2px;break-inside:avoid}
.gapp table{width:100%;border-collapse:collapse;font-size:12.4px;margin-top:5px}
.gapp th{background:${C.blue};color:#fff;text-align:left;padding:4px 6px}
.gapp td{padding:4px 6px;border-bottom:1px solid #d9dfe8;vertical-align:top}
.gapp tr{break-inside:avoid}
.gapp code{font-weight:400;font-family:JetBrains,monospace;font-size:.88em}
@page{margin:0}
`;
