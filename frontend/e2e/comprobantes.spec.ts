import { expect, test, Page } from '@playwright/test';
import { accionFila } from './actions';

async function login(page: Page): Promise<void> {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 15000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL((url) => !url.pathname.endsWith('/login'), { timeout: 15000 });
}

test.describe('Comprobantes electrónicos (Fase G, §3.7)', () => {
  test('la bandeja lista, filtra, descarga y enlaza a la auditoría', async ({ page }) => {
    await login(page);

    const sidebar = page.locator('.sidebar');
    await expect(sidebar.getByText('Comprobantes', { exact: true })).toBeVisible();
    await sidebar.locator('a', { hasText: 'Comprobantes' }).click();
    await page.waitForURL('**/facturacion/comprobantes', { timeout: 15000 });

    await expect(page.locator('h1.page-title')).toHaveText('Finanzas — Comprobantes');
    await expect(page.locator('[data-testid="comprobantes-total"]')).toContainText(
      'resultados',
      { timeout: 15000 },
    );
    const filas = page.locator('tbody tr');
    await expect(filas.first()).toBeVisible();
    await expect(page.locator('[data-testid="comprobantes-vacio"]')).toHaveCount(0);

    await page.selectOption('#comp-estado', 'NO_AUTORIZADO');
    await expect(page.locator('[data-testid="comprobantes-vacio"]')).toBeVisible({
      timeout: 15000,
    });
    await page.click('button:has-text("Limpiar")');
    await expect(filas.first()).toBeVisible({ timeout: 15000 });

    await accionFila(page, filas.first(), 'Ver');
    await expect(page.locator('[data-testid="detalle-comprobante"]')).toBeVisible();
    await expect(page.locator('[data-testid="detalle-clave"]')).toHaveText(/^\d{49}$/);

    const [descarga] = await Promise.all([
      page.waitForEvent('download'),
      page.click('[data-testid="descargar-xml"]'),
    ]);
    expect(descarga.suggestedFilename()).toContain('.xml');

    await page.click('[data-testid="auditoria-comprobante"]');
    await page.waitForURL('**/auditoria?**', { timeout: 15000 });
    await expect(page.locator('[data-testid="auditoria-filtro-entidad"]')).toBeVisible({
      timeout: 15000,
    });
    await expect(page.locator('[data-testid="auditoria-filtro-entidad"]')).toContainText(
      'COMPROBANTE_ELECTRONICO #',
    );
    await expect(page.locator('tbody tr').first()).toBeVisible();
  });

  test('la nota de crédito y la anulación se piden en sus diálogos (H.2)', async ({ page }) => {
    await login(page);
    await page.goto('./facturacion/comprobantes');
    await expect(page.locator('[data-testid="comprobantes-total"]')).toContainText(
      'resultados',
      { timeout: 15000 },
    );

    // Sólo una factura autorizada ofrece nota de crédito y anulación.
    await page.selectOption('#comp-tipo', '01');
    await page.selectOption('#comp-estado', 'AUTORIZADO');
    const factura = page.locator('tbody tr').first();
    await expect(factura).toBeVisible({ timeout: 15000 });

    await accionFila(page, factura, 'Nota de crédito');
    // El host <ui-modal> no tiene caja propia (el backdrop es fixed): se afirma el .modal.
    const dialogoNota = page.locator('[data-testid="dialogo-nota"] .modal');
    await expect(dialogoNota).toBeVisible();
    await expect(page.locator('[data-testid="nota-resumen"]')).toContainText(
      'emitirá una nota de crédito por',
    );
    await expect(page.locator('[data-testid="nota-motivo-contador"]')).toHaveText('0/300');

    await page.selectOption('#nota-alcance', 'Devolución parcial — sólo algunas líneas');
    await expect(page.locator('[data-testid^="nota-cantidad-"]').first()).toBeVisible();

    await page.fill('[data-testid="nota-motivo"]', 'abc');
    await page.click('[data-testid="nota-emitir"]');
    await expect(page.locator('[data-testid="nota-error"]')).toContainText('5 y 300');

    await page.click('[data-testid="nota-cancelar"]');
    await expect(page.locator('[data-testid="dialogo-nota"]')).toHaveCount(0);

    await accionFila(page, factura, 'Anular');
    const dialogoAnular = page.locator('[data-testid="dialogo-anular"] .modal');
    await expect(dialogoAnular).toBeVisible();
    await expect(page.locator('[data-testid="anular-motivo-contador"]')).toHaveText('0/500');
    await expect(dialogoAnular).toContainText('nunca se elimina');
    await page.click('[data-testid="anular-cancelar"]');
    await expect(page.locator('[data-testid="dialogo-anular"]')).toHaveCount(0);

    // Una nota de crédito ya emitida muestra su motivo y la factura que corrige.
    await page.click('button:has-text("Limpiar")');
    await page.selectOption('#comp-tipo', '04');
    const nota = page.locator('tbody tr').first();
    await expect(nota).toBeVisible({ timeout: 15000 });
    await accionFila(page, nota, 'Ver');
    await expect(page.locator('[data-testid="detalle-motivo"]')).not.toHaveText('—');
    await expect(page.locator('[data-testid="detalle-referencia"]')).toContainText('Factura #');
    await expect(page.locator('[data-testid="nota-credito"]')).toHaveCount(0);
  });

  test('las cuentas publican la acción Emitir factura', async ({ page }) => {
    await login(page);
    await page.goto('./facturacion/cuentas');

    const filas = page.locator('[data-testid="tabla-cuentas"] tbody tr');
    await expect(filas.first()).toBeVisible({ timeout: 15000 });
    await accionFila(page, filas.first(), 'Ver');
    await expect(page.locator('[data-testid="detalle-cuenta"]')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('[data-testid="emitir-factura"]')).toBeVisible({ timeout: 15000 });
  });
});
