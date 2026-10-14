import { expect, test, Page, APIRequestContext } from '@playwright/test';

const API = '/api/v1';

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
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

async function abrirPestanaTimeline(page: Page) {
  await page.goto('./pacientes/1/historia');
  await page.waitForSelector('h1', { timeout: 15000 });
  await page.getByRole('tab', { name: 'Línea de tiempo' }).click();
  // El total se pinta antes de la respuesta; hay que esperar a los eventos.
  await expect(page.locator('[data-testid="timeline-total"]')).not.toHaveText(/^0 /, { timeout: 15000 });
}

async function totalEventos(page: Page): Promise<number> {
  const texto = await page.locator('[data-testid="timeline-total"]').textContent();
  return Number.parseInt(texto?.replace(/\D+/g, '') ?? '0', 10);
}

function chip(page: Page, nombre: string) {
  return page.getByRole('button', { name: new RegExp(`^${nombre}\\b`) }).first();
}

/** Filtra por un tipo y devuelve cuántos eventos quedan visibles. */
async function filtrar(page: Page, nombre: string, etiqueta: string): Promise<number> {
  await chip(page, nombre).click();
  const insignias = page.locator('.timeline-item ui-badge');
  await expect(insignias.first()).toHaveText(etiqueta, { timeout: 15000 });
  const cantidad = await insignias.count();
  expect(cantidad).toBeGreaterThan(0);
  await expect(insignias).toHaveText(new Array(cantidad).fill(etiqueta));
  return cantidad;
}

test.describe('Línea temporal unificada (Fase E, §4.16)', () => {
  test('agrega todas las fuentes del paciente y filtra por tipo', async ({ page }) => {
    test.setTimeout(120_000);

    await login(page);
    await abrirPestanaTimeline(page);

    const total = await totalEventos(page);
    expect(total).toBeGreaterThanOrEqual(40);

    // Cada fuente con datos del paciente 1 tiene su chip con el conteo.
    for (const nombre of [
      'Diagnóstico',
      'Evolución',
      'Examen',
      'Odontograma',
      'Periodoncia',
      'Plan',
      'Receta',
      'Archivo',
      'Consentimiento',
    ]) {
      await expect(chip(page, nombre)).toBeVisible();
    }

    // Filtrar por odontograma: sólo eventos de odontograma, con su versión.
    const odontogramas = await filtrar(page, 'Odontograma', 'Odontograma');
    expect(odontogramas).toBeGreaterThanOrEqual(3);
    await expect(page.locator('.timeline-item', { hasText: 'Odontograma (v' }).first()).toBeVisible();

    // Filtrar por recetas: sólo recetas.
    const recetas = await filtrar(page, 'Receta', 'Receta');
    expect(recetas).toBeGreaterThanOrEqual(10);
    expect(recetas).toBeLessThan(total);

    // Volver a "Todos" recupera la lista completa.
    await chip(page, 'Todos').click();
    await expect(page.locator('.timeline-item')).toHaveCount(total, { timeout: 15000 });
    expect(await totalEventos(page)).toBe(total);
  });

  test('recepción sólo consulta la línea temporal', async ({ page, request }) => {
    test.setTimeout(120_000);
    const t = await token(request);
    await asegurarRecepcion(request, t);

    await page.goto('./');
    await page.waitForSelector('input[name="username"]', { timeout: 10000 });
    await page.fill('input[name="username"]', 'qa.recep.lectura');
    await page.fill('input[name="password"]', 'QaOdonto123');
    await page.press('input[name="password"]', 'Enter');
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    await abrirPestanaTimeline(page);

    expect(await totalEventos(page)).toBeGreaterThanOrEqual(40);
    await expect(chip(page, 'Odontograma')).toBeVisible();
    await expect(page.locator('.timeline-item').first()).toBeVisible();
  });
});
