import { test, expect, Page } from '@playwright/test';

// Todas las rutas autenticadas del app: cada control se audita en cada pantalla.
const PAGINAS = [
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
const ANCHOS = [360, 480, 768, 1280];

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

test.describe('QA de accesibilidad y responsive (regresión)', () => {
  let page: Page;
  const erroresConsola: string[] = [];
  const recursosFallidos: string[] = [];

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    erroresConsola.length = 0;
    recursosFallidos.length = 0;
    page.on('console', msg => {
      if (msg.type() !== 'error') return;
      const t = msg.text();
      if (t.includes('fonts.googleapis.com') || t.includes('fonts.gstatic.com')) return;
      // Los 4xx/5xx se auditan por URL en el propio test
      if (t.startsWith('Failed to load resource')) return;
      erroresConsola.push(t);
    });
    page.on('pageerror', err => erroresConsola.push(err.message));
    page.on('response', res => {
      const url = res.url();
      if (res.status() < 400) return;
      if (url.includes('fonts.googleapis.com') || url.includes('fonts.gstatic.com')) return;
      recursosFallidos.push(`${res.status()} ${url}`);
    });
    await login(page);
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('ninguna página produce scroll horizontal (360 → 1280 px)', async () => {
    test.setTimeout(180_000);
    const fallas: string[] = [];
    for (const ancho of ANCHOS) {
      await page.setViewportSize({ width: ancho, height: 900 });
      for (const ruta of PAGINAS) {
        await ir(page, ruta);
        const { scroll, viewport } = await page.evaluate(() => ({
          scroll: document.documentElement.scrollWidth,
          viewport: window.innerWidth,
        }));
        if (scroll > viewport + 1) fallas.push(`${ruta} @${ancho}px: ${scroll} > ${viewport}`);
      }
    }
    expect(fallas).toEqual([]);
  });

  test('todo botón visible tiene nombre accesible y al menos 30 px', async () => {
    await page.setViewportSize({ width: 1280, height: 900 });
    const fallas: string[] = [];
    for (const ruta of PAGINAS) {
      await ir(page, ruta);
      const problemas = await page.evaluate(() => {
        const out: string[] = [];
        for (const el of Array.from(document.querySelectorAll<HTMLElement>('button, [role="button"]'))) {
          if (el.getClientRects().length === 0) continue;
          const nombre = (
            el.getAttribute('aria-label') ||
            el.getAttribute('title') ||
            el.textContent ||
            ''
          ).trim();
          if (!nombre) out.push(`sin nombre → ${el.outerHTML.slice(0, 90)}`);
          const alto = Math.round(el.getBoundingClientRect().height);
          if (alto < 30) out.push(`alto ${alto}px → ${el.outerHTML.slice(0, 90)}`);
        }
        return out;
      });
      fallas.push(...problemas.map(p => `${ruta}: ${p}`));
    }
    expect(fallas).toEqual([]);
  });

  test('todo campo de formulario visible está etiquetado', async () => {
    await page.setViewportSize({ width: 1280, height: 900 });
    const fallas: string[] = [];
    for (const ruta of PAGINAS) {
      await ir(page, ruta);
      const problemas = await page.evaluate(() => {
        const out: string[] = [];
        for (const el of Array.from(
          document.querySelectorAll<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>(
            'input, select, textarea',
          ),
        )) {
          if (el.getClientRects().length === 0) continue;
          if (el.type === 'hidden') continue;
          const asociado =
            (el.labels && el.labels.length > 0) ||
            !!el.getAttribute('aria-label') ||
            !!el.getAttribute('aria-labelledby') ||
            !!el.getAttribute('title');
          if (!asociado) out.push(`${el.tagName.toLowerCase()} sin etiqueta → ${el.outerHTML.slice(0, 90)}`);
        }
        return out;
      });
      fallas.push(...problemas.map(p => `${ruta}: ${p}`));
    }
    expect(fallas).toEqual([]);
  });

  test('no hay ids duplicados ni errores de consola al navegar', async () => {
    await page.setViewportSize({ width: 1280, height: 900 });
    const duplicados: string[] = [];
    for (const ruta of PAGINAS) {
      await ir(page, ruta);
      const ids = await page.evaluate(() =>
        Array.from(document.querySelectorAll('[id]')).map(el => (el as HTMLElement).id),
      );
      const unicos = new Set(ids);
      for (const id of unicos) if (ids.filter(x => x === id).length > 1) duplicados.push(`${ruta}: #${id}`);
    }
    expect(duplicados).toEqual([]);
    expect(recursosFallidos).toEqual([]);
    expect(erroresConsola).toEqual([]);
  });

  test('los diálogos caben en 360 px sin desbordar la pantalla', async () => {
    await page.setViewportSize({ width: 360, height: 800 });

    // Formulario modal (alta de paciente)
    await ir(page, 'pacientes');
    await page.locator('button.btn.primary', { hasText: 'Nuevo paciente' }).first().click();
    const form = page.locator('.modal');
    await expect(form).toBeVisible();
    const cajaForm = await form.boundingBox();
    expect(cajaForm).not.toBeNull();
    expect(cajaForm!.x).toBeGreaterThanOrEqual(0);
    expect(cajaForm!.x + cajaForm!.width).toBeLessThanOrEqual(361);

    // Escape cierra el modal sin necesidad de tocar el botón
    await page.keyboard.press('Escape');
    await expect(form).toBeHidden();

    // Diálogo de confirmación del sistema UI
    await page.locator('button.btn.small.danger:not([disabled])').first().click();
    const dialogo = page.locator('[role="dialog"]');
    await expect(dialogo).toBeVisible();
    const cajaDlg = await dialogo.boundingBox();
    expect(cajaDlg).not.toBeNull();
    expect(cajaDlg!.x).toBeGreaterThanOrEqual(0);
    expect(cajaDlg!.x + cajaDlg!.width).toBeLessThanOrEqual(361);
    expect(cajaDlg!.y).toBeGreaterThanOrEqual(0);

    const { scroll, viewport } = await page.evaluate(() => ({
      scroll: document.documentElement.scrollWidth,
      viewport: window.innerWidth,
    }));
    expect(scroll).toBeLessThanOrEqual(viewport + 1);
  });

  test('en móvil el topbar conserva el menú y el alternador de tema', async () => {
    await page.setViewportSize({ width: 360, height: 800 });
    await ir(page, 'dashboard');
    const menu = page.locator('.topbar-menu');
    const tema = page.locator('.topbar-tema');
    await expect(menu).toBeVisible();
    await expect(menu).toHaveAttribute('aria-label', /.+/);
    await expect(tema).toBeVisible();
    await expect(tema).toHaveAttribute('title', /modo/);
  });
});
