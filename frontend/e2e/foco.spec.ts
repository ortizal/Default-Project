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
];

interface Foco {
  tag: string;
  clase: string;
  texto: string;
  outlineStyle: string;
  outlineWidth: number;
  boxShadow: string;
}

/** Recorre la página con Tab y devuelve los focos sin anillo visible. */
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
      return {
        tag: el.tagName.toLowerCase(),
        clase: (el.getAttribute('class') || '').slice(0, 60),
        texto: (el.textContent || '').trim().replace(/\s+/g, ' ').slice(0, 30),
        outlineStyle: cs.outlineStyle,
        outlineWidth: parseFloat(cs.outlineWidth) || 0,
        boxShadow: cs.boxShadow,
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
