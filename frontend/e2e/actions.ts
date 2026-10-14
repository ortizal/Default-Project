import { Locator, Page } from '@playwright/test';

/**
 * Las columnas de acciones de las tablas muestran un único disparador por fila
 * (icono de menú con flecha hacia abajo); todas las opciones viven dentro de un
 * mat-menu desplegado en el overlay de CDK. Estos helpers abren el menú de una
 * fila y, si se indica, pulsan una opción.
 */
export async function abrirAcciones(page: Page, fila: Locator): Promise<void> {
  const trigger = fila.locator('button.menu-trigger').first();
  await trigger.click();
  await page.locator('[role="menu"]:visible').waitFor({ state: 'visible' });
}

export async function accionFila(
  page: Page,
  fila: Locator,
  nombre: string | RegExp,
): Promise<void> {
  await abrirAcciones(page, fila);
  await page.getByRole('menuitem', { name: nombre }).click();
}

/**
 * Abre el diálogo de confirmación de "Desactivar" recorriendo las filas hasta
 * encontrar un registro cuyo menú ofrezca la opción activa (las filas ya
 * inactivas no la incluyen deshabilitada).
 */
export async function abrirDesactivar(page: Page): Promise<void> {
  const filas = page.locator('tbody tr');
  const total = await filas.count();
  for (let i = 0; i < total; i++) {
    const trigger = filas.nth(i).locator('button.menu-trigger');
    if ((await trigger.count()) === 0) continue;
    await trigger.first().click();
    const item = page.getByRole('menuitem', { name: 'Desactivar' });
    const hay = (await item.count()) > 0 && (await item.isEnabled());
    if (hay) {
      await item.click();
      await page.locator('[role="dialog"]').waitFor({ state: 'visible', timeout: 5000 });
      return;
    }
    await page.keyboard.press('Escape');
  }
  throw new Error('No hay ninguna fila con la acción "Desactivar" activa');
}
