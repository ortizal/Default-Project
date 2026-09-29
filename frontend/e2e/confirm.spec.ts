import { test, expect, Page } from '@playwright/test';

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

async function abrirConfirmacion(page: Page) {
  await page.goto('pacientes');
  await page.waitForSelector('tbody tr', { timeout: 10000 });
  const desactivar = page.locator('button.btn.small.danger:not([disabled])').first();
  await expect(desactivar).toBeVisible();
  await desactivar.click();

  const dialogo = page.locator('[role="dialog"]');
  await expect(dialogo).toBeVisible({ timeout: 5000 });
  return dialogo;
}

test.describe('Confirmación de acciones destructivas (diálogo del sistema UI)', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('el diálogo se abre con título, pregunta y acciones', async () => {
    const dialogo = await abrirConfirmacion(page);

    await expect(dialogo).toHaveAttribute('aria-modal', 'true');
    await expect(dialogo.locator('h2')).toHaveText('Desactivar paciente');
    await expect(dialogo).toContainText('¿Desactivar este paciente?');
    await expect(dialogo.locator('button', { hasText: 'Desactivar' })).toBeVisible();
    await expect(dialogo.locator('button', { hasText: 'Cancelar' })).toBeVisible();
  });

  test('cancelar (botón o Escape) no envía ninguna petición', async () => {
    const borrados: string[] = [];
    page.on('request', req => {
      if (req.method() === 'DELETE') borrados.push(req.url());
    });

    const dialogo = await abrirConfirmacion(page);
    await dialogo.locator('button', { hasText: 'Cancelar' }).click();
    await expect(dialogo).toBeHidden();
    expect(borrados).toEqual([]);

    // Escape también cancela
    const dialogo2 = await abrirConfirmacion(page);
    await page.keyboard.press('Escape');
    await expect(dialogo2).toBeHidden();
    expect(borrados).toEqual([]);
  });

  test('aceptar ejecuta la acción y cierra el diálogo', async () => {
    const borrados: string[] = [];
    await page.route('**/api/v1/pacientes/**', async route => {
      if (route.request().method() === 'DELETE') {
        borrados.push(route.request().url());
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: '{}',
        });
        return;
      }
      await route.continue();
    });

    const dialogo = await abrirConfirmacion(page);
    await dialogo.locator('button', { hasText: 'Desactivar' }).click();

    await expect(dialogo).toBeHidden();
    expect(borrados.length).toBe(1);
    // La lista se recarga tras la acción
    await expect(page.locator('tbody tr').first()).toBeVisible();
  });
});
