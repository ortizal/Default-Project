import { test, expect, Page } from '@playwright/test';

async function login(page: Page) {
  await page.goto('/');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

test.describe('Google Calendar — Gestión de credenciales OAuth', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    page.on('request', req => { if (req.url().includes('/api/')) console.log('REQ:', req.method(), req.url()); });
    page.on('response', async res => { if (res.status() >= 400 && res.url().includes('/api/')) console.log('ERR:', res.status(), res.url()); });
    await login(page);
  });

  test.afterEach(async () => { await page.close(); });

  test('Formulario de credenciales aparece cuando no están configuradas', async () => {
    await page.goto('/google');
    await page.waitForSelector('.card', { timeout: 10000 });

    // Esperar a que Angular cargue las credenciales del API
    const clientIdInput = page.locator('input[placeholder="google.client-id"]');
    await expect(clientIdInput).toBeVisible({ timeout: 10000 });
    await expect(page.locator('input[placeholder="google.client-secret"]')).toBeVisible({ timeout: 5000 });
  });

  test('Guardar credenciales habilita la opción de conectar Google', async () => {
    await page.goto('/google');
    await page.waitForSelector('.card', { timeout: 10000 });

    // Esperar a que la credencial cargue desde el API
    const clientIdInput = page.locator('input[placeholder="google.client-id"]');
    await expect(clientIdInput).toBeVisible({ timeout: 10000 });

    // Rellenar credenciales
    await clientIdInput.fill('test-client-id');
    await page.locator('input[placeholder="google.client-secret"]').fill('test-client-secret');
    await page.locator('input[placeholder="http://localhost:8080/api/v1/google/callback"]').fill('http://localhost:8080/api/v1/google/callback');

    // Guardar
    await page.click('button.btn.primary');
    await expect(page.locator('.msg.ok')).toContainText('Credenciales guardadas correctamente', { timeout: 5000 });

    // Verificar que el estado cambió a configurada
    await expect(page.locator('.msg.warn')).not.toBeVisible({ timeout: 5000 });
    await expect(page.locator('text=Conectar con Google')).toBeVisible({ timeout: 5000 });
  });
});
