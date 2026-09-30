import { test, expect, Page } from '@playwright/test';

/**
 * Publicación en redes sociales: la UI debe validar texto e imagen antes de
 * llamar a `POST /social/publish` (el backend sólo acepta JPEG/PNG/WebP de
 * hasta 5 MB y un texto de 2200 caracteres).
 */

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

async function abrirPublicacion(page: Page) {
  await page.goto('redes-sociales');
  await page.waitForSelector('h1', { timeout: 15000 });
  await expect(page.getByRole('heading', { name: 'Crear publicación' })).toBeVisible({ timeout: 10000 });
}

test.describe('Publicación en redes sociales', () => {
  let page: Page;

  test.beforeEach(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
  });

  test.afterEach(async () => {
    await page.close();
  });

  test('el formulario se valida antes de enviar', async () => {
    await abrirPublicacion(page);

    const publicar = page.getByRole('button', { name: /^Publicar/ });
    await expect(publicar).toBeVisible();
    await expect(publicar).toBeDisabled();

    const texto = page.locator('textarea[maxlength="2200"]');
    await expect(texto).toHaveAttribute('maxlength', '2200');
    await expect(page.getByLabel('Cuenta de destino')).toBeVisible();

    await texto.fill('Campaña de blanqueamiento durante todo octubre.');
    const longitud = (await texto.inputValue()).length;
    await expect(page.getByText(`${longitud}/2200 caracteres`)).toBeVisible();
  });

  test('rechaza imágenes que no son JPEG, PNG o WebP', async () => {
    await abrirPublicacion(page);

    const archivo = page.locator('input[type="file"]');
    await archivo.setInputFiles({
      name: 'contrato.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('no es una imagen'),
    });
    await expect(page.getByRole('alert')).toContainText('JPEG, PNG o WebP');
    await expect(page.getByText('contrato.pdf')).toHaveCount(0);
  });

  test('rechaza imágenes de más de 5 MB', async () => {
    await abrirPublicacion(page);

    const archivo = page.locator('input[type="file"]');
    await archivo.setInputFiles({
      name: 'promocion.png',
      mimeType: 'image/png',
      buffer: Buffer.alloc(5 * 1024 * 1024 + 1, 1),
    });
    await expect(page.getByRole('alert')).toContainText('5 MB');
  });

  test('permite quitar la imagen seleccionada', async () => {
    await abrirPublicacion(page);

    const archivo = page.locator('input[type="file"]');
    await archivo.setInputFiles({
      name: 'cartel.png',
      mimeType: 'image/png',
      buffer: Buffer.alloc(2048, 7),
    });
    await expect(page.getByText(/cartel\.png/)).toBeVisible();

    await page.getByRole('button', { name: 'Quitar imagen' }).click();
    await expect(page.getByText(/cartel\.png/)).toHaveCount(0);
    await expect(page.getByRole('alert')).toHaveCount(0);
  });
});
