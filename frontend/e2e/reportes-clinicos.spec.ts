import { expect, test, Page, APIRequestContext } from '@playwright/test';
import { readFileSync } from 'node:fs';

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
  const r = await api.post(`${API}/auth/login`, { data: { username: 'admin', password: 'admin123' } });
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

test.describe('Reportes clínicos (Fase E, §4.17)', () => {
  test('el menú publica la entrada y la página pinta los cuatro bloques', async ({ page }) => {
    await login(page);

    const sidebar = page.locator('.sidebar');
    await expect(sidebar.getByText('Reportes clínicos', { exact: true })).toBeVisible();

    await sidebar.locator('a', { hasText: 'Reportes clínicos' }).click();
    await page.waitForURL('**/reportes-clinicos', { timeout: 10000 });

    // El topbar repite el título en un <h1>: se apunta al de la página.
    await expect(page.locator('h1.page-title')).toHaveText('Reportes clínicos');
    await expect(page.locator('#rc-desde')).toBeVisible();
    await expect(page.locator('#rc-hasta')).toBeVisible();

    const totales = page.locator('[data-testid="reportes-clinicos-totales"]');
    await expect(totales).toBeVisible({ timeout: 15000 });
    await expect(totales.locator('.num')).toHaveCount(6);
    await expect(totales).toContainText('Pacientes atendidos');
    await expect(totales).toContainText('Recetas');

    await expect(page.locator('[data-testid="continuidad"]')).toBeVisible();
    await expect(page.locator('[data-testid="continuidad"]')).toContainText('Sin próxima cita');

    await page.getByRole('button', { name: 'Actualizar' }).click();
    await expect(page.locator('[data-testid="reportes-clinicos-totales"]')).toBeVisible({
      timeout: 15000,
    });
  });

  test('descarga la historia clínica completa en PDF desde la ficha', async ({ page }) => {
    await login(page);

    await page.goto('./pacientes/1/historia');
    await page.waitForSelector('h1', { timeout: 15000 });

    const boton = page.getByRole('button', { name: /Historia completa \(PDF\)/ });
    await expect(boton).toBeVisible();

    const [descarga] = await Promise.all([page.waitForEvent('download'), boton.click()]);
    expect(descarga.suggestedFilename()).toBe('historia-clinica-completa-1.pdf');

    const ruta = await descarga.path();
    expect(ruta).toBeTruthy();
    expect(readFileSync(ruta!).subarray(0, 4).toString()).toBe('%PDF');
  });

  test('sin REPORTES_CLINICOS no aparece el menú ni el botón y la API responde 403', async ({
    page,
    request,
  }, testInfo) => {
    test.skip(testInfo.project.name !== 'chromium', 'Sólo se ejecuta en Chromium');
    const t = await token(request);
    await asegurarRecepcion(request, t);

    await login(page, 'qa.recep.lectura', 'QaOdonto123');
    await expect(page.locator('.sidebar').getByText('Reportes clínicos', { exact: true })).toHaveCount(0);

    await page.goto('./reportes-clinicos');
    await expect(page.locator('[data-testid="reportes-error"]')).toHaveText(
      'No tienes permiso para ver los reportes clínicos.',
      { timeout: 15000 },
    );

    await page.goto('./pacientes/1/historia');
    await page.waitForSelector('h1', { timeout: 15000 });
    await expect(page.getByRole('button', { name: /Historia completa \(PDF\)/ })).toHaveCount(0);
  });
});
