// Genera RecuVeicol_presentazione.pdf e RecuVeicol_guida_studio.pdf. Istruzioni in README.md.
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL, fileURLToPath } from 'node:url';
import { css, logo, md, esc, C } from './lib.mjs';
import s1 from './slides_1_apertura_architettura.mjs';
import s2 from './slides_2_backend_frontend.mjs';
import s3 from './slides_3_chat_mappe_notifiche.mjs';
import s4, { glossario, domandePerMembro, alternative } from './slides_4_chiusura_appendice.mjs';

const HERE = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.resolve(HERE, '..');
const { chromium } = await import(process.env.PLAYWRIGHT_MODULE ?? 'playwright');

const slides = [...s1, ...s2, ...s3, ...s4];
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
  const sc = nt.scelta ? `<h4>Alternative, perché scartate, limiti</h4><p><b>Alternative:</b> ${md(nt.scelta.alt)}</p><p><b>Perché:</b> ${md(nt.scelta.perche)}</p><p><b>Limiti e gestione:</b> ${md(nt.scelta.limiti)}</p>` : '';
  return `<h4>Cosa dire a voce (60-90 secondi)</h4><p>${md(nt.voce)}</p>
    <h4>Spiegazione semplice</h4><p>${md(nt.semplice)}</p>
    <h4>File principali</h4><p>${nt.file.map(md).join(' · ')}</p>${sc}
    <h4>Domande del professore</h4>${nt.domande.map(([q, a]) => `<p class="q">${md(q)}</p><p>${md(a)}</p>`).join('')}`;
}
const html = (body) => `<!doctype html><html lang="it"><head><meta charset="utf-8"><title>RecuVeicol</title>${STYLE}</head><body>${body}</body></html>`;

/* controlli sui contenuti prima di generare */
const problems = [];
slides.forEach((s, i) => {
  const n = i + 1, nt = s.notes;
  if (!nt?.voce || !nt.semplice || !nt.file?.length || !(nt.domande?.length >= 2)) problems.push(`slide ${n}: note incomplete`);
  const w = nt.voce.split(/\s+/).length;
  if (w < 125 || w > 215) problems.push(`slide ${n}: testo a voce di ${w} parole (atteso 125-215, cioè 60-90 s)`);
});

const slidesHtml = slides.map(renderSlide).join('');
const guideHtml = slides.map((s, i) => `<article class="gpage" data-n="${i + 1}">
    <div class="ghead"><span class="gs">${esc(s.section)}</span><span>Responsabile: ${esc(s.owner)}</span><span class="gn">${i + 1} / ${N}</span></div>
    <div class="gslide">${renderSlide(s, i)}</div>
    <div class="notes">${renderNotes(s)}</div></article>`).join('');
const appendix = `
  <article class="gapp"><h1>Appendice · Glossario</h1><p class="intro">Solo nella guida di studio. I termini tecnici in parole semplici.</p>
    <dl>${glossario.map(([t, d]) => `<dt>${esc(t)}</dt><dd>${md(d)}</dd>`).join('')}</dl></article>
  ${[[0, 1], [2, 3], [4]].map((gruppo, pi) => `<article class="gapp">${pi === 0 ? '<h1>Appendice · Domande probabili del professore</h1><p class="intro">Risposte brevi, per membro.</p>' : ''}${gruppo.map((idx) => { const [m, qs] = domandePerMembro[idx]; return `<h2>${esc(m)}</h2>${qs.map(([q, a]) => `<p class="q">${md(q)}</p><p>${md(a)}</p>`).join('')}`; }).join('')}</article>`).join('')}
  <article class="gapp" style="page-break-after:auto;break-after:auto"><h1>Appendice · Alternative considerate</h1>
    <p class="intro">La colonna «Base» dice da dove viene il motivo. «Documentata»: nel registro decisioni o nei manuali. «Dal codice»: si vede da ciò che il codice usa. «Ricostruita» o «Non documentata»: non c'è un confronto scritto nel repository, quindi va presentata come valutazione del team, dopo averla confermata.</p>
    <table><tr><th style="width:12%">Area</th><th style="width:15%">Scelta</th><th style="width:16%">Scartata</th><th>Motivo</th><th style="width:17%">Base</th></tr>
    ${alternative.map((r) => `<tr>${r.map((c) => `<td>${md(c)}</td>`).join('')}</tr>`).join('')}</table></article>`;

fs.mkdirSync(path.join(HERE, '.tmp'), { recursive: true });
// i percorsi ../img partono da sorgente/.tmp -> riscritti rispetto a docs/presentazione/img
const fix = (h) => h.replaceAll('src="../img/', `src="${pathToFileURL(path.join(OUT, 'img')).href}/`);
const fSlides = path.join(HERE, '.tmp', 'slides.html');
const fGuide = path.join(HERE, '.tmp', 'guide.html');
fs.writeFileSync(fSlides, fix(html(slidesHtml)));
fs.writeFileSync(fGuide, fix(html(guideHtml + appendix)));

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
      let f = 13.6;
      n.style.setProperty('--nf', f + 'px');
      while (n.scrollWidth > n.clientWidth + 1 && f > 10.6) { f -= 0.1; n.style.setProperty('--nf', f.toFixed(1) + 'px'); }
      res.push({ n: pg.dataset.n, f: +f.toFixed(1), over: n.scrollWidth > n.clientWidth + 1 });
    });
    return res;
  });
  fit.filter((x) => x.over).forEach((x) => problems.push(`guida pagina ${x.n}: note oltre la pagina`));
  const lunghe = await g.evaluate(() => [...document.querySelectorAll('.gapp')].filter((a) => a.scrollHeight > 1124).map((a) => a.querySelector('h1,h2').textContent));
  lunghe.forEach((t) => problems.push(`appendice oltre la pagina: ${t}`));
  console.log('corpo note min/medio:', Math.min(...fit.map((x) => x.f)), (fit.reduce((a, x) => a + x.f, 0) / fit.length).toFixed(1));
  await g.pdf({ path: path.join(OUT, 'RecuVeicol_guida_studio.pdf'), format: 'A4', printBackground: true, preferCSSPageSize: false, margin: { top: 0, bottom: 0, left: 0, right: 0 } });
} finally {
  await browser.close();
  fs.rmSync(path.join(HERE, '.tmp'), { recursive: true, force: true });
}
console.log(`${N} slide generate.`);
if (problems.length) { console.log('PROBLEMI:\n' + problems.join('\n')); process.exitCode = 1; } else console.log('Nessun problema rilevato.');
