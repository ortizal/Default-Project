import { test, expect, Page } from '@playwright/test';

test.describe('Diagnóstico de red y consola', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    // Escuchar logs de consola
    page.on('console', msg => console.log('BROWSER:', msg.text()));
    page.on('pageerror', err => console.log('PAGE ERROR:', err.message));
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('Verificar petición HTTP y cambios de cargando', async () => {
    await page.route('**/api/v1/odontologos**', async route => {
      console.log('INTERCEPTED:', route.request().url());
      await route.continue();
    });

    await page.goto('/');
    await page.waitForSelector('input[name="username"]', { timeout: 10000 });
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.press('input[name="password"]', 'Enter');
    await page.waitForURL('**/dashboard', { timeout: 10000 });
    
    // Ir a odontologos
    await page.click('a[href="/odontologos"]');
    await page.waitForURL('**/odontologos', { timeout: 10000 });

    // Esperar 10 segundos
    await page.waitForTimeout(10000);

    // Revisar si el spinner todavía existe
    const spinnerCount = await page.locator('.spinner').count();
    const tableRows = await page.locator('tbody tr').count();
    
    console.log('Spinner count after 10s:', spinnerCount);
    console.log('Table rows:', tableRows);
    
    // Obtener logs de red
    const logs = await page.evaluate(() => window.__consoleLogs || []);
    console.log('Console logs:', logs);

    // Tomar screenshot final
    await page.screenshot({ path: 'test-results/diagnostico_final.png' });
  });
});
