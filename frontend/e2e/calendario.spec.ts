import { test, expect, Page } from '@playwright/test';

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

// La pantalla depende del estado real del backend (configurada / conectada).
// Los tests son no destructivos: NUNCA sobrescriben las credenciales OAuth.
test.describe('Google Calendar — Pantalla de integraciones', () => {
  let page: Page;

  const form = () => page.locator('input[placeholder="google.client-id"]');
  const btnConectar = () => page.locator('button', { hasText: 'Conectar con Google' });
  const btnSincronizar = () => page.locator('button', { hasText: 'Sincronizar citas' });

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('Carga el estado real desde la API sin errores 4xx/5xx', async () => {
    const fallos: string[] = [];
    page.on('response', res => {
      if (res.url().includes('/api/') && res.status() >= 400) {
        fallos.push(`${res.status()} ${res.url()}`);
      }
    });

    await page.goto('google');
    await page.waitForSelector('.card', { timeout: 10000 });

    // Uno de los tres estados debe renderizarse: formulario, sin conectar o conectada
    await expect(form().or(btnConectar()).or(btnSincronizar()).first()).toBeVisible({
      timeout: 15000,
    });
    expect(fallos).toEqual([]);
  });

  test('La advertencia de "no configurada" nunca convive con el panel de conexión', async () => {
    await page.goto('google');
    await page.waitForSelector('.card', { timeout: 10000 });

    // Esperar a que `/google/status` pinte la pantalla
    const credenciales = page.getByLabel('Client ID');
    const advertencia = page.locator('.msg.warn');
    const conexion = page.locator('.stat', { hasText: 'Conexión' });
    await expect(credenciales.or(conexion).first()).toBeVisible({ timeout: 15000 });

    if (await advertencia.isVisible()) {
      // Sin credenciales en el servidor: aviso + formulario, nunca conexión
      await expect(conexion).toHaveCount(0);
      await expect(page.getByLabel('Client Secret')).toBeVisible();
    } else {
      // Con credenciales: sin aviso y con el panel de conexión (si hay sesión de Google)
      await expect(advertencia).toHaveCount(0);
      await expect(credenciales.or(conexion).first()).toBeVisible();
    }
  });

  test('Muestra la cuenta y el calendario conectados', async () => {
    await page.goto('google');
    await page.waitForSelector('.card', { timeout: 10000 });

    const estadoConectado = page.locator('.stat', { hasText: 'Conexión' });
    await expect(form().or(estadoConectado).first()).toBeVisible({ timeout: 15000 });

    // Solo tiene sentido si el backend está realmente conectado
    if (await estadoConectado.isVisible()) {
      await expect(page.locator('.stat .badge', { hasText: 'Conectada' })).toBeVisible();
      await expect(page.locator('h2', { hasText: 'Calendarios de la cuenta' })).toBeVisible();
      expect(await page.locator('table.tbl tbody tr').count()).toBeGreaterThan(0);
    }
  });
});
