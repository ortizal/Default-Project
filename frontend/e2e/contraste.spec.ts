import { test, expect, Page } from '@playwright/test';

const PAGINAS = ['dashboard', 'pacientes', 'odontologos', 'citas', 'agenda', 'reportes'];

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

async function ir(page: Page, ruta: string) {
  await page.goto(ruta);
  await page.waitForSelector('h1', { timeout: 15000 });
}

/**
 * Recorre el texto visible, compone los fondos con alfa hacia arriba por la
 * cadena de ancestros y calcula el ratio WCAG (4.5 normal / 3.0 grande).
 */
const evaluarContraste = () => {
  const parse = (c) => {
    const m = /rgba?\(([^)]+)\)/.exec(c || '');
    if (!m) return null;
    const p = m[1].split(',').map(s => parseFloat(s.trim()));
    return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
  };
  const over = (fg, bg) => ({
    r: fg.r * fg.a + bg.r * (1 - fg.a),
    g: fg.g * fg.a + bg.g * (1 - fg.a),
    b: fg.b * fg.a + bg.b * (1 - fg.a),
    a: 1,
  });
  const lum = c => {
    const f = v => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); };
    return 0.2126 * f(c.r) + 0.7152 * f(c.g) + 0.0722 * f(c.b);
  };
  const ratio = (a, b) => {
    const l1 = lum(a), l2 = lum(b);
    const hi = Math.max(l1, l2), lo = Math.min(l1, l2);
    return (hi + 0.05) / (lo + 0.05);
  };
  const fondo = el => {
    const capas = [];
    let n = el;
    while (n) {
      const c = parse(getComputedStyle(n).backgroundColor);
      if (c && c.a > 0) capas.push(c);
      n = n.parentElement;
    }
    let acc = { r: 255, g: 255, b: 255, a: 1 };
    for (let i = capas.length - 1; i >= 0; i--) acc = over(capas[i], acc);
    return acc;
  };
  const ruta = el => {
    const p = [];
    let n = el;
    while (n && n.tagName.toLowerCase() !== 'html' && p.length < 4) {
      p.unshift(n.tagName.toLowerCase() + (n.classList.length ? '.' + [...n.classList].slice(0, 2).join('.') : ''));
      n = n.parentElement;
    }
    return p.join('>');
  };

  const fallas = [];
  for (const el of Array.from(document.querySelectorAll('body *'))) {
    const directo = [...el.childNodes].some(n => n.nodeType === 3 && n.textContent.trim().length > 0);
    if (!directo || el.getClientRects().length === 0) continue;

    const cs = getComputedStyle(el);
    if (cs.visibility === 'hidden' || cs.opacity === '0') continue;
    const fg = parse(cs.color);
    if (!fg || fg.a === 0) continue;

    const bg = fondo(el);
    const compuesto = over(fg, bg);
    const px = parseFloat(cs.fontSize);
    const negrita = parseInt(cs.fontWeight, 10) >= 700;
    const grande = px >= 24 || (negrita && px >= 18.66);
    const minimo = grande ? 3.0 : 4.5;
    const r = ratio(compuesto, bg);
    if (r + 0.01 < minimo) {
      fallas.push(`${ruta(el)} | ${Math.round(px)}px | ratio ${r.toFixed(2)} < ${minimo} | "${el.textContent.trim().slice(0, 30)}"`);
    }
  }
  return fallas;
};

interface Escenario {
  nombre: string;
  colorScheme: 'light' | 'dark';
  tema: 'light' | 'dark' | null;
}

const ESCENARIOS: Escenario[] = [
  { nombre: 'sistema claro', colorScheme: 'light', tema: null },
  { nombre: 'sistema oscuro', colorScheme: 'dark', tema: null },
  { nombre: 'sistema claro + elección manual oscura', colorScheme: 'light', tema: 'dark' },
  { nombre: 'sistema oscuro + elección manual clara', colorScheme: 'dark', tema: 'light' },
];

for (const esc of ESCENARIOS) {
  test(`contraste WCAG AA — ${esc.nombre}`, async ({ browser }) => {
    const page = await browser.newPage();
    await page.emulateMedia({ colorScheme: esc.colorScheme });
    if (esc.tema) {
      await page.addInitScript(t => localStorage.setItem('ls-theme', t), esc.tema);
    }
    await login(page);

    const fallas: string[] = [];
    for (const ruta of PAGINAS) {
      await ir(page, ruta);
      const vistas = await page.evaluate(evaluarContraste);
      fallas.push(...vistas.map(v => `${ruta}: ${v}`));
    }
    expect(fallas).toEqual([]);
    await page.close();
  });
}
