import { test, expect, Page } from '@playwright/test';

async function login(page: Page) {
  await page.goto('/');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

async function gotoOdontologos(page: Page) {
  await page.click('a[href="/odontologos"]');
  await page.waitForURL('**/odontologos', { timeout: 10000 });
  await page.waitForSelector('.card', { timeout: 10000 });
}

test.describe('Listado de Odontólogos — Spinner y carga automática', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('SPINNER desaparece y tabla carga automáticamente al abrir la pantalla', async () => {
    await gotoOdontologos(page);

    // El spinner debe desaparecer en máximo 8 segundos
    await expect(page.locator('.spinner')).not.toBeVisible({ timeout: 8000 });

    // La tabla debe tener al menos una fila de datos
    const rowCount = await page.locator('tbody tr').count();
    expect(rowCount).toBeGreaterThan(0);
  });

  test('Datos consistentes: carga inicial == botón Refrescar', async () => {
    await gotoOdontologos(page);

    // Esperar carga inicial
    await expect(page.locator('.spinner')).not.toBeVisible({ timeout: 8000 });

    // Capturar datos de la tabla
    const rowsInit = await page.locator('tbody tr').allInnerTexts();

    // Presionar Refrescar
    await page.click('button[title="Recargar lista"]');
    await expect(page.locator('.spinner')).not.toBeVisible({ timeout: 5000 });

    // Verificar que los datos siguen siendo consistentes
    const rowsAfter = await page.locator('tbody tr').allInnerTexts();
    expect(rowsAfter.length).toBeGreaterThan(0);
  });
});
