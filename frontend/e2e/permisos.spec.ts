import { test, expect, Page, APIRequestContext } from '@playwright/test';

const API = '/api/v1';
const USUARIO = 'qa.odonto';
const CLAVE = 'qaMenu123';

async function login(page: Page, usuario: string, clave: string) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', usuario);
  await page.fill('input[name="password"]', clave);
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

/** Crea (de forma idempotente) el usuario de prueba con rol ODONTOLOGO. */
async function prepararUsuarioOdontologo(api: APIRequestContext) {
  const admin = await api.post(`${API}/auth/login`, {
    data: { username: 'admin', password: 'admin123' },
  });
  expect(admin.ok()).toBeTruthy();
  const { token } = await admin.json();

  // Si ya existe de una corrida anterior devuelve 409: se continúa igual.
  await api.post(`${API}/usuarios`, {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      username: USUARIO,
      email: `${USUARIO}@qa.local`,
      password: CLAVE,
      nombres: 'Menú',
      apellidos: 'QA',
      roles: ['ODONTOLOGO'],
    },
  });
}

test.describe('Menú dinámico por permisos (Fase A)', () => {
  test('el odontólogo ve sólo los ítems que su rol permite', async ({ page, request }) => {
    await prepararUsuarioOdontologo(request);
    await login(page, USUARIO, CLAVE);

    // La sesión trae los permisos del backend (21 para ODONTOLOGO).
    const permisos = await page.evaluate(
      () => (JSON.parse(localStorage.getItem('dentalcrm.auth') ?? '{}').permisos ?? []) as string[],
    );
    expect(permisos.length).toBe(21);
    expect(permisos).toContain('HISTORIA_CLINICA_READ');
    expect(permisos).toContain('ODONTOGRAMA_READ');
    expect(permisos).not.toContain('USUARIOS_READ');
    expect(permisos).not.toContain('FACTURACION_EMITIR');
    await expect(page.locator('h1.page-title')).toHaveText('Centro de inteligencia');
    await expect(page.locator('[data-testid="centro-inteligencia"] a.intelligence-link')).toHaveCount(1);
    await expect(page.locator('[data-testid="centro-inteligencia"]')).toContainText('Actividad clínica');
    await expect(page.locator('[data-testid="centro-inteligencia"]')).not.toContainText('Finanzas');

    const sidebar = page.locator('.sidebar');
    await expect(sidebar.getByText('Pacientes', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('Agenda', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('Odontólogos', { exact: true })).toBeVisible();

    // Su rol sólo tiene permisos de "Operación" y, desde §4.17, de "Gestión"
    // (REPORTES_CLINICOS): el resto de secciones ni se pinta.
    await expect(sidebar.locator('.section')).toHaveText(['Operación', 'Gestión']);
    await expect(sidebar.getByText('Reportes clínicos', { exact: true })).toBeVisible();

    await expect(sidebar.getByText('Reportes', { exact: true })).toHaveCount(0);
    await expect(sidebar.getByText('Usuarios', { exact: true })).toHaveCount(0);
    await expect(sidebar.getByText('Auditoría', { exact: true })).toHaveCount(0);
    await expect(sidebar.getByText('Datos del consultorio', { exact: true })).toHaveCount(0);
    await expect(sidebar.getByText('Publicidad WhatsApp', { exact: true })).toHaveCount(0);

    await page.goto('./reportes');
    await expect(page.locator('h1.page-title')).toHaveText('Centro de reportes');
    await expect(page.locator('[data-testid="catalogo-reportes"] article')).toHaveCount(1);
    await expect(page.locator('[data-testid="catalogo-reportes"]')).toContainText('Reportes clínicos');
    await expect(page.locator('[data-testid="catalogo-reportes"]')).not.toContainText('Estadísticas de operación');
    await expect(page.locator('[data-testid="catalogo-reportes"]')).not.toContainText('Reportes financieros');
  });

  test('el super admin sigue viendo el menú completo', async ({ page }) => {
    await login(page, 'admin', 'admin123');

    const sidebar = page.locator('.sidebar');
    await expect(sidebar.getByText('Usuarios', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('Auditoría', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('Datos del consultorio', { exact: true })).toBeVisible();
    await expect(sidebar.getByText('Pacientes', { exact: true })).toBeVisible();
  });
});
