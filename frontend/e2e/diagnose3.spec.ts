import { test, expect, Page } from '@playwright/test';

test.describe('Diagnóstico completo', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    page.on('request', req => { if (req.url().includes('/api/')) console.log('REQ:', req.method(), req.url()); });
    page.on('response', async res => { if (res.status() >= 400 && res.url().includes('/api/')) console.log('ERR:', res.status(), res.url()); });
    page.on('console', msg => { if (msg.type() === 'error') console.log('JS ERROR:', msg.text()); });
  });

  test.afterEach(async () => { await page.close(); });

  test('Verificar cargando y datos en odontologos', async () => {
    // Login
    await page.goto('/');
    await page.waitForSelector('input[name="username"]', { timeout: 10000 });
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.press('input[name="password"]', 'Enter');
    await page.waitForURL('**/dashboard', { timeout: 10000 });
    
    // Navegar directamente a odontologos
    await page.goto('/odontologos');
    await page.waitForSelector('.card', { timeout: 10000 });

    // Esperar 8 segundos
    await page.waitForTimeout(8000);

    // Evaluar cargando desde el contexto del navegador
    const cargando = await page.evaluate(() => {
      const el = document.querySelector('app-odontologos');
      return el ? (el as any).__ngContext__?.['cargando'] : null;
    });
    console.log('cargando value:', cargando);

    // Verificar spinner y filas
    const spinnerCount = await page.locator('.spinner').count();
    const tableRows = await page.locator('tbody tr').count();
    console.log('Spinner count:', spinnerCount);
    console.log('Table rows:', tableRows);

    // Verificar el error
    const errorText = await page.locator('.msg.error').textContent();
    console.log('Error text:', errorText);

    await page.screenshot({ path: 'test-results/diag3.png' });
  });
});
