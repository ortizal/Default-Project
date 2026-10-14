import { expect, test } from '@playwright/test';

/**
 * Agenda pública por enlace temporal (§10–§13 del plan).
 * La página no requiere sesión: se prueba sin login, como el login
 * y la auditoría axe ya hacen con sus rutas públicas.
 */
test.describe('Agenda pública (enlace temporal)', () => {
  test('sin token muestra el aviso de enlace inválido', async ({ page }) => {
    await page.goto('./agenda-publica');
    await expect(page.getByTestId('agenda-publica-error')).toContainText('no es válido');
    await expect(page.getByTestId('agenda-publica-calendario')).toHaveCount(0);
  });

  test('con un token inválido el backend rechaza y la página lo explica', async ({ page }) => {
    await page.goto('./agenda-publica?t=token-basura');
    await expect(page.getByTestId('agenda-publica-error')).toContainText('caducó', { timeout: 15000 });
    await expect(page.getByTestId('agenda-publica-calendario')).toHaveCount(0);
  });

  test('la página pública no exige login ni redirige al dashboard', async ({ page }) => {
    await page.goto('./agenda-publica?t=token-basura');
    await page.waitForSelector('[data-testid="agenda-publica-error"]', { timeout: 15000 });
    expect(page.url()).toContain('agenda-publica');
    expect(page.url()).not.toContain('login');
    await expect(page.locator('h1')).toContainText('Little Smile');
  });
});
