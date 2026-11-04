// Genera RecuVeicol_presentazione.pdf e RecuVeicol_guida_studio.pdf. Istruzioni in README.md.
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL, fileURLToPath } from 'node:url';
import { css, logo, md, esc, C } from './lib.mjs';
import slides from './slides.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.resolve(HERE, '..');
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE ?? 'playwright');

const N = slides.length;
const fontUrl = (f) => pathToFileURL(path.join(HERE, 'fonts', f + '.woff2')).href;
const STYLE = `<style>${css(fontUrl)}</style>`;

function foot(s, n) {
  const white = s.type === 'divider' || s.type === 'cover';
  return `<div class="foot">${logo(C.yellow, white ? C.blue : '#fff')}<b>RecuVeicol</b><span class="sec">${esc(s.section)}</span><span class="num">${n} / ${N}</span></div>`;
}
function renderSlide(s, i) {
  const n = i + 1;
  if (s.type === 'cover') {
    return `<section class="slide cover" data-n="${n}">
      <div class="brand">${logo(C.yellow, C.blue)}RecuVeicol</div>
      <h2>Gestione del soccorso stradale</h2>
      <div class="meta">Project work · ITS Developer 4.0<br>Novembre 2026</div>
      <div class="team"><b>Team</b><br>Vincenzo Minervini · Francesco Valeriano<br>Giuseppe Cangellosi · Marika Sancilio · Marco Colucci</div>
      <div class="art"><img src="../img/carro_attrezzi.png" alt="Carro attrezzi RecuVeicol"></div>
      ${foot(s, n)}</section>`;
  }
  if (s.type === 'divider') {
    return `<section class="slide div" data-n="${n}"><div class="inner"><span class="mem">${esc(s.member)}</span><h1>${esc(s.title)}</h1><div class="who">${esc(s.who)}</div><div class="top">${esc(s.topics)}</div></div>${foot(s, n)}</section>`;
  }
  return `<section class="slide" data-n="${n}"><div class="kicker">${esc(s.section)}</div><h1>${esc(s.title)}</h1><div class="body">${s.html}</div>${foot(s, n)}</section>`;
}
function renderNotes(s) {
  const nt = s.notes;
  return `<h4>Cosa dire a voce</h4><p>${md(nt.voce)}</p>
    <h4>In parole semplici</h4><p>${md(nt.semplice)}</p>
    <h4>File principali</h4><p>${nt.file.map(md).join(' · ')}</p>`;
}
const html = (body) => `<!doctype html><html lang="it"><head><meta charset="utf-8"><title>RecuVeicol</title>${STYLE}</head><body>${body}</body></html>`;

/* controlli sui contenuti prima di generare */
const problems = [];
slides.forEach((s, i) => {
  const n = i + 1, nt = s.notes;
  if (!nt?.voce || !nt.semplice || !nt.file?.length) problems.push(`slide ${n}: note incomplete`);
  const w = nt.voce.split(/\s+/).length;
  if (w > 75) problems.push(`slide ${n}: testo a voce di ${w} parole (massimo 75)`);
});

const slidesHtml = slides.map(renderSlide).join('');
const guideHtml = slides.map((s, i) => `<article class="gpage" data-n="${i + 1}">
    <div class="ghead"><span class="gs">${esc(s.section)}</span><span>Responsabile: ${esc(s.owner)}</span><span class="gn">${i + 1} / ${N}</span></div>
    <div class="gslide">${renderSlide(s, i)}</div>
    <div class="notes">${renderNotes(s)}</div></article>`).join('');
fs.mkdirSync(path.join(HERE, '.tmp'), { recursive: true });
// i percorsi ../img partono da sorgente/.tmp -> riscritti rispetto a docs/presentazione/img
const fix = (h) => h.replaceAll('src="../img/', `src="${pathToFileURL(path.join(OUT, 'img')).href}/`);
const fSlides = path.join(HERE, '.tmp', 'slides.html');
const fGuide = path.join(HERE, '.tmp', 'guide.html');
fs.writeFileSync(fSlides, fix(html(slidesHtml)));
fs.writeFileSync(fGuide, fix(html(guideHtml)));

const browser = await chromium.launch();
try {
  const p = await browser.newPage({ viewport: { width: 1280, height: 720 } });
  await p.goto(pathToFileURL(fSlides).href);
  await p.evaluate(() => document.fonts.ready);
  await p.waitForTimeout(800);
  const overflow = await p.evaluate(() => {
    const out = [];
    document.querySelectorAll('.slide').forEach((sl) => {
      const n = sl.dataset.n, b = sl.getBoundingClientRect();
      const foot = sl.querySelector('.foot')?.getBoundingClientRect();
      const limit = foot ? foot.top : b.bottom;
      sl.querySelectorAll('.body *, .inner *, .team, .art, .meta, h1').forEach((e) => {
        if (e.closest('svg') && e.tagName !== 'svg') return;
        const r = e.getBoundingClientRect();
        if (r.width === 0 || r.height === 0) return;
        if (r.bottom > limit + 0.5 || r.right > b.right - 30 || r.left < b.left + 30) out.push(`slide ${n}: <${e.tagName.toLowerCase()}> ${(e.textContent || '').slice(0, 40).trim()} b=${Math.round(r.bottom - limit)} r=${Math.round(r.right - b.right)}`);
      });
      sl.querySelectorAll('img').forEach((im) => { if (!im.complete || !im.naturalWidth) out.push(`slide ${n}: immagine mancante ${im.src}`); });
    });
    return out;
  });
  problems.push(...overflow);
  await p.pdf({ path: path.join(OUT, 'RecuVeicol_presentazione.pdf'), width: '1280px', height: '720px', printBackground: true, preferCSSPageSize: false });

  const g = await browser.newPage({ viewport: { width: 794, height: 1123 } });
  await g.goto(pathToFileURL(fGuide).href);
  await g.evaluate(() => document.fonts.ready);
  await g.waitForTimeout(800);
  // adatta il corpo delle note: si riduce solo se non entrano, mai sotto 10,6 px
  const fit = await g.evaluate(() => {
    const res = [];
    document.querySelectorAll('.gpage').forEach((pg) => {
      const n = pg.querySelector('.notes');
      let f = 15.5;
      n.style.setProperty('--nf', f + 'px');
      while (n.scrollHeight > n.clientHeight + 1 && f > 12) { f -= 0.1; n.style.setProperty('--nf', f.toFixed(1) + 'px'); }
      res.push({ n: pg.dataset.n, f: +f.toFixed(1), over: n.scrollHeight > n.clientHeight + 1 });
    });
    return res;
  });
  fit.filter((x) => x.over).forEach((x) => problems.push(`guida pagina ${x.n}: note oltre la pagina`));
  console.log('corpo note min/medio:', Math.min(...fit.map((x) => x.f)), (fit.reduce((a, x) => a + x.f, 0) / fit.length).toFixed(1));
  await g.pdf({ path: path.join(OUT, 'RecuVeicol_guida_studio.pdf'), format: 'A4', printBackground: true, preferCSSPageSize: false, margin: { top: 0, bottom: 0, left: 0, right: 0 } });
} finally {
  await browser.close();
  fs.rmSync(path.join(HERE, '.tmp'), { recursive: true, force: true });
}
console.log(`${N} slide generate.`);
if (problems.length) { console.log('PROBLEMI:\n' + problems.join('\n')); process.exitCode = 1; } else console.log('Nessun problema rilevato.');
