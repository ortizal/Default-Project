import { test, expect, Page } from '@playwright/test';

/**
 * Contraste en estados :hover / con puntero encima.
 *
 * axe y contraste.spec.ts sólo miden el estado base de cada elemento; WCAG 1.4.3
 * también aplica a los estados interactivos, y ahí se colaron fallos reales
 * (botón primario al pasar el ratón, icono-button de la bandeja, fila de tabla).
 */

const RUTAS = [
  'dashboard',
  'reportes',
  'pacientes',
  'odontologos',
  'servicios',
  'horarios',
  'agenda',
  'citas',
  'whatsapp/sesiones',
  'whatsapp/inbox',
  'whatsapp/campana',
  'plantillas',
  'automatizaciones',
  'notificaciones',
  'agente',
  'google',
  'auditoria',
  'usuarios',
  'configuracion/consultorio',
  'configuracion/integraciones',
  'redes-sociales',
];

const SELECTORES = [
  '.btn.primary',
  '.btn.small, .btn.sm',
  '.btn.danger',
  '.icon-btn',
  '.seg-btn',
  '.tbl tbody tr',
  '.badge',
  '.topbar-tema',
  'a.map-link',
  'a[href^="http"]',
];

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

/** Pasa el ratón por el primer elemento del selector y mide el contraste de su
 *  subárbol con los estilos vigentes (:hover). Devuelve las fallas detectadas. */
const evaluarHover = async (page: Page, selector: string): Promise<string[]> => {
  const loc = page.locator(selector).first();
  if ((await loc.count()) === 0 || !(await loc.isVisible())) return [];
  try {
    await loc.hover({ timeout: 3000 });
  } catch {
    return ['no se pudo pasar el ratón (elemento no interactivo)'];
  }
  await page.waitForTimeout(220);
  return page.evaluate((sel: string) => {
    const raiz = document.querySelector(sel);
    if (!raiz) return ['elemento no encontrado'];

    const parse = (c: string | null) => {
      const m = /rgba?\(([^)]+)\)/.exec(c || '');
      if (!m) return null;
      const p = m[1].split(',').map(s => parseFloat(s.trim()));
      return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
    };
    const over = (fg: any, bg: any) => ({
      r: fg.r * fg.a + bg.r * (1 - fg.a),
      g: fg.g * fg.a + bg.g * (1 - fg.a),
      b: fg.b * fg.a + bg.b * (1 - fg.a),
      a: 1,
    });
    const lum = (c: any) => {
      const f = (v: number) => { v /= 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); };
      return 0.2126 * f(c.r) + 0.7152 * f(c.g) + 0.0722 * f(c.b);
    };
    const ratio = (a: any, b: any) => {
      const l1 = lum(a), l2 = lum(b);
      const hi = Math.max(l1, l2), lo = Math.min(l1, l2);
      return (hi + 0.05) / (lo + 0.05);
    };
    const fondo = (el: Element) => {
      const capas: any[] = [];
      let n: Element | null = el;
      while (n) {
        const c = parse(getComputedStyle(n).backgroundColor);
        if (c && c.a > 0) capas.push(c);
        n = n.parentElement;
      }
      let acc = { r: 255, g: 255, b: 255, a: 1 };
      for (let i = capas.length - 1; i >= 0; i--) acc = over(capas[i], acc);
      return acc;
    };

    const fallas: string[] = [];
    const nodos = [raiz, ...raiz.querySelectorAll('*')];
    for (const el of nodos) {
      const directo = [...el.childNodes].some(n => n.nodeType === 3 && n.textContent.trim().length > 0);
      if (!directo || el.getClientRects().length === 0) continue;
      if ((el as HTMLButtonElement).disabled) continue;
      if (el.closest('[disabled]')) continue;

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
        fallas.push(`${cs.color} sobre fondo ≈ rgb(${Math.round(bg.r)}, ${Math.round(bg.g)}, ${Math.round(bg.b)}) | ${Math.round(px)}px | ratio ${r.toFixed(2)} < ${minimo} | "${el.textContent.trim().slice(0, 24)}"`);
      }
    }
    return fallas;
  }, selector);
};

for (const oscuro of [false, true]) {
  const etiqueta = oscuro ? 'tema oscuro' : 'tema claro';

  test(`estados hover mantienen contraste AA (${etiqueta})`, async ({ page }) => {
    test.setTimeout(300_000);
    await login(page);
    if (oscuro) await page.emulateMedia({ colorScheme: 'dark' });

    const fallas: string[] = [];
    for (const ruta of RUTAS) {
      await page.goto(ruta);
      await page.waitForSelector('h1', { timeout: 15000 });
      for (const sel of SELECTORES) {
        const f = await evaluarHover(page, sel);
        for (const detalle of f) fallas.push(`${ruta} · ${sel} → ${detalle}`);
      }
    }
    expect(fallas).toEqual([]);
  });
}
