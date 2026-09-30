import { test, expect, Page } from '@playwright/test';

/**
 * WCAG 2.4.7 "Focus Visible": todo control que se alcanza con Tab debe mostrar
 * un anillo de foco (outline o box-shadow) de ancho > 0. axe no lo mide.
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

interface Foco {
  tag: string;
  clase: string;
  texto: string;
  outlineStyle: string;
  outlineWidth: number;
  outlineColor: string;
  boxShadow: string;
  contraste: number | null;
}

interface RGB {
  r: number;
  g: number;
  b: number;
  a: number;
}

/** Recorre la página con Tab y devuelve los focos sin anillo visible o cuyo
 *  anillo no llega a 3:1 contra el fondo (WCAG 1.4.11). */
const recorrerConTab = async (page: Page, pasos: number): Promise<string[]> => {
  const fallas: string[] = [];
  const vistos = new Set<string>();

  for (let i = 0; i < pasos; i++) {
    await page.keyboard.press('Tab');
    // el anillo entra con transición (0.15s): se mide una vez asentado
    await page.waitForTimeout(180);
    const f: Foco | null = await page.evaluate(() => {
      const el = document.activeElement as HTMLElement | null;
      if (!el || el === document.body || el === document.documentElement) return null;
      const cs = getComputedStyle(el);

      const parse = (c: string): RGB | null => {
        const m = /rgba?\(([^)]+)\)/.exec(c || '');
        if (!m) return null;
        const p = m[1].split(',').map(s => parseFloat(s.trim()));
        return { r: p[0], g: p[1], b: p[2], a: p.length > 3 ? p[3] : 1 };
      };
      const over = (fg: RGB, bg: RGB): RGB => ({
        r: fg.r * fg.a + bg.r * (1 - fg.a),
        g: fg.g * fg.a + bg.g * (1 - fg.a),
        b: fg.b * fg.a + bg.b * (1 - fg.a),
        a: 1,
      });
      const lum = (c: RGB) => {
        const g = (v: number) => {
          v /= 255;
          return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        };
        return 0.2126 * g(c.r) + 0.7152 * g(c.g) + 0.0722 * g(c.b);
      };
      const ratio = (a: RGB, b: RGB) => {
        const l1 = lum(a);
        const l2 = lum(b);
        const hi = Math.max(l1, l2);
        const lo = Math.min(l1, l2);
        return (hi + 0.05) / (lo + 0.05);
      };

      // El anillo se dibuja fuera del elemento: el fondo relevante es el del padre.
      const capas: RGB[] = [];
      let ancestro = el.parentElement;
      while (ancestro) {
        const c = parse(getComputedStyle(ancestro).backgroundColor);
        if (c && c.a > 0) capas.push(c);
        ancestro = ancestro.parentElement;
      }
      let fondo: RGB = { r: 255, g: 255, b: 255, a: 1 };
      for (let i = capas.length - 1; i >= 0; i--) fondo = over(capas[i], fondo);

      const colores: RGB[] = [];
      const outlineActivo = cs.outlineStyle !== 'none' && (parseFloat(cs.outlineWidth) || 0) > 0;
      if (outlineActivo) {
        const c = parse(cs.outlineColor);
        if (c && c.a > 0) colores.push(c);
      }
      for (const m of cs.boxShadow.matchAll(/rgba?\([^)]+\)/g)) {
        const c = parse(m[0]);
        if (c && c.a > 0) colores.push(c);
      }
      const contraste = colores.length
        ? Math.max(...colores.map(c => ratio(c, fondo)))
        : null;

      return {
        tag: el.tagName.toLowerCase(),
        clase: (el.getAttribute('class') || '').slice(0, 60),
        texto: (el.textContent || '').trim().replace(/\s+/g, ' ').slice(0, 30),
        outlineStyle: cs.outlineStyle,
        outlineWidth: parseFloat(cs.outlineWidth) || 0,
        outlineColor: cs.outlineColor,
        boxShadow: cs.boxShadow,
        contraste,
      };
    });
    if (!f) continue;

    const clave = `${f.tag}.${f.clase}`;
    const anillo =
      (f.outlineStyle !== 'none' && f.outlineWidth > 0) || f.boxShadow !== 'none';
    if (!anillo && !vistos.has(clave)) {
      vistos.add(clave);
      fallas.push(
        `${f.tag}${f.clase ? '.' + f.clase.split(' ')[0] : ''} "${f.texto}" → outline ${f.outlineStyle} ${f.outlineWidth}px, box-shadow: ${f.boxShadow}`,
      );
    } else if (anillo && f.contraste !== null && f.contraste < 3 && !vistos.has(clave)) {
      vistos.add(clave);
      fallas.push(
        `${f.tag}${f.clase ? '.' + f.clase.split(' ')[0] : ''} "${f.texto}" → anillo ${f.contraste.toFixed(2)}:1 < 3:1 ` +
          `(outline ${f.outlineColor}, box-shadow ${f.boxShadow})`,
      );
    }
  }
  return fallas;
};

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

test('login: los controles muestran anillo de foco', async ({ page }) => {
  test.setTimeout(60_000);
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  expect(await recorrerConTab(page, 12)).toEqual([]);
});

test('cada página: todo foco por teclado es visible', async ({ page }) => {
  test.setTimeout(480_000);
  await login(page);

  const fallas: string[] = [];
  for (const ruta of RUTAS) {
    await page.goto(ruta);
    await page.waitForSelector('h1', { timeout: 15000 });
    await page.evaluate(() => (document.activeElement as HTMLElement | null)?.blur());
    for (const detalle of await recorrerConTab(page, 35)) {
      fallas.push(`${ruta} · ${detalle}`);
    }
  }
  expect(fallas).toEqual([]);
});
