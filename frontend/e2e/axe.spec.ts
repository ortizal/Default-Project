import { expect, test, Page } from '@playwright/test';
import { AxeBuilder } from '@axe-core/playwright';

// Todas las rutas del app (app.routes.ts) salvo `login`, que se audita sin sesión.
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

// WCAG A/AA más las buenas prácticas de axe (landmarks, orden de encabezados,
// cabeceras de tabla, roles): todo ha de pasar sin incidencias.
const ETIQUETAS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'best-practice'];

async function preparar(page: Page) {
  await page.emulateMedia({ reducedMotion: 'reduce' });
}

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
 * `.content` entra con un fundido de 0,25 s: si axe se ejecuta en plena
 * animación, todos los textos quedan con opacidad parcial y el contraste
 * cae de forma artificial. Con `prefers-reduced-motion` esa animación dura
 * 0,01 ms, así que se audita el estado asentado (no el transitorio).
 */
async function auditar(page: Page, contexto: string): Promise<string[]> {
  const res = await new AxeBuilder({ page }).withTags(ETIQUETAS).analyze();
  return res.violations.map(
    v =>
      `${contexto} → ${v.id} [${v.impact}] ${v.help}\n` +
      v.nodes.map(n => `      ${n.html}\n      ${n.failureSummary?.replace(/\n/g, ' ')}`).join('\n'),
  );
}

test('sin violaciones WCAG A/AA en las páginas principales', async ({ page }) => {
  test.setTimeout(180_000);
  await preparar(page);
  await login(page);
  const fallos: string[] = [];
  for (const ruta of PAGINAS) {
    await ir(page, ruta);
    fallos.push(...(await auditar(page, ruta)));
  }
  expect(fallos).toEqual([]);
});

test('sin violaciones WCAG A/AA en la pantalla de acceso', async ({ page }) => {
  test.setTimeout(120_000);
  await preparar(page);
  await page.goto('./');
  await page.waitForSelector('input[name="username"]');
  expect(await auditar(page, 'login')).toEqual([]);
});

test('sin violaciones WCAG A/AA con el formulario y el diálogo abiertos', async ({ page }) => {
  test.setTimeout(180_000);
  await preparar(page);
  await login(page);
  await ir(page, 'pacientes');

  await page.locator('button.btn.primary', { hasText: 'Nuevo paciente' }).first().click();
  await expect(page.locator('.modal')).toBeVisible();
  expect(await auditar(page, 'pacientes/formulario')).toEqual([]);

  await page.keyboard.press('Escape');
  await expect(page.locator('.modal')).toBeHidden();
  await page.locator('button.btn.small.danger:not([disabled])').first().click();
  await expect(page.locator('[role="dialog"]')).toBeVisible();
  expect(await auditar(page, 'pacientes/confirmación')).toEqual([]);
});

test('sin violaciones en el tema oscuro en todas las rutas', async ({ page }) => {
  test.setTimeout(240_000);
  await preparar(page);
  await login(page);
  await page.evaluate(() => {
    localStorage.setItem('ls-theme', 'dark');
    document.documentElement.classList.remove('theme-light');
    document.documentElement.classList.add('theme-dark');
  });
  const fallos: string[] = [];
  for (const ruta of PAGINAS) {
    await ir(page, ruta);
    fallos.push(...(await auditar(page, `oscuro/${ruta}`)));
  }
  expect(fallos).toEqual([]);
});
