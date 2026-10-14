import { expect, test, Page, APIRequestContext } from '@playwright/test';
import { accionFila } from './actions';

const API = '/api/v1';

async function login(page: Page, usuario = 'admin', clave = 'admin123') {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', usuario);
  await page.fill('input[name="password"]', clave);
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL((url) => !url.pathname.endsWith('/login'), { timeout: 10000 });
}

async function token(api: APIRequestContext): Promise<string> {
  const r = await api.post(`${API}/auth/login`, {
    data: { username: 'admin', password: 'admin123' },
  });
  expect(r.ok()).toBeTruthy();
  return (await r.json()).token as string;
}

const auth = (t: string) => ({ Authorization: `Bearer ${t}` });

/** Usuario idempotente de sólo lectura: si ya existe se reutiliza. */
async function asegurarRecepcion(api: APIRequestContext, t: string) {
  const existe = await api.post(`${API}/auth/login`, {
    data: { username: 'qa.recep.lectura', password: 'QaOdonto123' },
  });
  if (existe.ok()) return;
  const creacion = await api.post(`${API}/usuarios`, {
    headers: auth(t),
    data: {
      username: 'qa.recep.lectura',
      email: 'qa.recep.lectura@qa.local',
      password: 'QaOdonto123',
      nombres: 'Recepción',
      apellidos: 'QA',
      roles: ['RECEPCION'],
    },
  });
  expect([200, 201, 409]).toContain(creacion.status());
}

test.describe('Finanzas internas (Fase F, §3.3/§3.4/§3.12/§3.13)', () => {
  test('el catálogo abre reportes y conserva la ruta de estadísticas operativas', async ({ page }) => {
    await login(page);
    await page.goto('./reportes');

    await expect(page.locator('h1.page-title')).toHaveText('Centro de reportes');
    await expect(page.locator('[data-testid="catalogo-reportes"] article')).toHaveCount(3);
    await page.locator('[data-testid="catalogo-reportes"] article')
      .filter({ hasText: 'Estadísticas de operación' })
      .getByRole('link', { name: 'Abrir reporte' })
      .click();

    await page.waitForURL('**/reportes/operacion');
    await expect(page.locator('h1.page-title')).toHaveText('Reportes');
  });

  test('el menú publica Finanzas y el dashboard pinta sus bloques', async ({ page }) => {
    await login(page);

    const sidebar = page.locator('.sidebar');
    await expect(sidebar.getByText('Finanzas', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('Dashboard financiero', { exact: true })).toBeVisible();

    await sidebar.locator('a', { hasText: 'Dashboard financiero' }).click();
    await page.waitForURL('**/facturacion', { timeout: 10000 });

    await expect(page.locator('h1.page-title')).toHaveText('Finanzas — Dashboard');

    const bloques = page.locator('[data-testid="dashboard-financiero"]');
    await expect(bloques).toBeVisible({ timeout: 15000 });
    await expect(bloques.locator('.num')).toHaveCount(6);
    await expect(page.locator('[data-testid="dashboard-hoy-mes"] .num')).toHaveCount(4);
    await expect(page.locator('[data-testid="finanzas-error"]')).toHaveCount(0);

    await page.getByRole('button', { name: 'Actualizar' }).click();
    await expect(bloques).toBeVisible({ timeout: 15000 });
  });

  test('la cuenta del paciente refleja el cargo y permite abrir su detalle', async ({
    page,
    request,
  }) => {
    const t = await token(request);

    const cargo = await request.post(`${API}/facturacion/cuentas/1/movimientos`, {
      headers: auth(t),
      data: { tipo: 'CARGO', concepto: 'QA e2e empaste', precioUnitario: 25, cantidad: 1 },
    });
    expect(cargo.ok()).toBeTruthy();

    const detalle = await request.get(`${API}/facturacion/cuentas/1`, { headers: auth(t) });
    expect(detalle.ok()).toBeTruthy();
    const { paciente } = await detalle.json();

    await login(page);
    await page.goto('./facturacion/cuentas');
    await page.waitForSelector('h1.page-title', { timeout: 15000 });

    const tabla = page.locator('[data-testid="tabla-cuentas"]');
    await expect(tabla).toBeVisible({ timeout: 15000 });

    const fila = tabla.locator('tr', { hasText: paciente });
    await accionFila(page, fila, 'Ver');

    const cuenta = page.locator('[data-testid="detalle-cuenta"]');
    await expect(cuenta).toBeVisible({ timeout: 15000 });
    await expect(cuenta.locator('.num')).toHaveCount(3);

    const movimientos = page.locator('[data-testid="tabla-movimientos"]');
    await expect(movimientos).toBeVisible();
    await expect(movimientos).toContainText('QA e2e empaste');

    await expect(page.locator('[data-testid="form-movimiento"]')).toBeVisible();
    await expect(page.locator('[data-testid="cuentas-error"]')).toHaveCount(0);
  });

  test('la caja abre la sesión y registra un egreso manual', async ({ page }) => {
    await login(page);
    await page.goto('./facturacion/caja');
    await page.waitForSelector('h1.page-title', { timeout: 15000 });

    const sinSesion = page.locator('[data-testid="caja-cerrada"]');
    if (await sinSesion.isVisible()) {
      await page.getByRole('button', { name: 'Abrir caja' }).click();
    }

    const sesion = page.locator('[data-testid="caja-sesion"]');
    await expect(sesion).toBeVisible({ timeout: 15000 });
    await expect(sesion.locator('.num')).toHaveCount(6);
    await expect(page.locator('[data-testid="caja-error"]')).toHaveCount(0);

    await page.fill('#mov-caja-concepto', 'QA e2e egreso');
    await page.fill('#mov-caja-monto', '1');
    await page.getByRole('button', { name: 'Registrar' }).click();

    await expect(page.locator('[data-testid="caja-mensaje"]')).toHaveText(
      'Movimiento registrado.',
      { timeout: 15000 },
    );
    await expect(page.locator('[data-testid="tabla-movimientos-caja"]')).toContainText(
      'QA e2e egreso',
    );
    await expect(page.locator('[data-testid="tabla-historial-caja"]')).toBeVisible();
  });

  test('los reportes financieros piden el rango y cambian de pestaña', async ({ page }) => {
    await login(page);
    await page.goto('./facturacion/reportes');
    await page.waitForSelector('h1.page-title', { timeout: 15000 });

    await expect(page.locator('[data-testid="reporte-ventas"]')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('[data-testid="reporte-ventas"] .num')).toHaveCount(4);
    await expect(page.locator('#rep-desde')).toBeVisible();
    await expect(page.locator('#rep-hasta')).toBeVisible();

    await page.getByRole('tab', { name: 'Saldos' }).click();
    await expect(page.locator('[data-testid="reporte-saldos"]')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('#rep-desde')).toHaveCount(0);

    await page.getByRole('tab', { name: 'Caja' }).click();
    await expect(page.locator('[data-testid="reporte-caja"]')).toBeVisible({ timeout: 15000 });

    await expect(page.locator('[data-testid="reportes-fin-error"]')).toHaveCount(0);
  });

  test('recepción ve el resumen financiero, no los reportes detallados', async ({
    page,
    request,
  }) => {
    const t = await token(request);
    await asegurarRecepcion(request, t);

    await login(page, 'qa.recep.lectura', 'QaOdonto123');

    const sidebar = page.locator('.sidebar');
    await expect(sidebar.getByText('Finanzas', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('Dashboard financiero', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('Caja', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('Cuentas', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('Reportes financieros', { exact: true })).toHaveCount(0);

    const sesionRecepcion = await request.post(`${API}/auth/login`, {
      data: { username: 'qa.recep.lectura', password: 'QaOdonto123' },
    });
    expect(sesionRecepcion.ok()).toBeTruthy();
    const tokenRecepcion = (await sesionRecepcion.json()).token as string;
    const resumen = await request.get(`${API}/facturacion/dashboard`, {
      headers: auth(tokenRecepcion),
    });
    expect(resumen.ok()).toBeTruthy();
    const ventas = await request.get(`${API}/facturacion/reportes/ventas`, {
      headers: auth(tokenRecepcion),
    });
    expect(ventas.status()).toBe(403);

    await page.goto('./facturacion');
    await expect(page.locator('[data-testid="dashboard-financiero"]')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('[data-testid="finanzas-error"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="tendencia-financiera"]')).toHaveCount(0);

    await page.goto('./facturacion/reportes');
    await expect(page.locator('[data-testid="reportes-fin-error"]')).toHaveText(
      'No tienes permiso para ver los reportes financieros.',
      { timeout: 15000 },
    );
  });
});
