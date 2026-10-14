import { expect, test, Page, APIRequestContext } from '@playwright/test';
import { accionFila } from './actions';

const API = '/api/v1';
const DIENTE = 26;
const CONDICION = 'CARIES_QA';

let odontogramaId = 0;
let condicionId = 0;

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

/** Estado conocido: versión vigente con una condición de prueba. */
async function preparar(api: APIRequestContext) {
  const t = await token(api);

  let vigente = await api.get(`${API}/historia-clinica/pacientes/1/odontograma`, {
    headers: { Authorization: `Bearer ${t}` },
  });
  let odontograma = vigente.ok() ? await vigente.json() : null;

  if (!odontograma) {
    const creado = await api.post(`${API}/historia-clinica/pacientes/1/odontogramas`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { observaciones: 'Odontograma de QA', denticion: 'PERMANENTE' },
    });
    expect(creado.ok()).toBeTruthy();
    odontograma = await creado.json();
  }
  // El dibujo se audita en dentición permanente: si la vigente es de otra
  // dentición se abre una versión nueva (copia) con PERMANENTE.
  if (odontograma.denticion !== 'PERMANENTE') {
    const nueva = await api.post(`${API}/historia-clinica/pacientes/1/odontogramas`, {
      headers: { Authorization: `Bearer ${t}` },
      data: { copiarUltima: true, denticion: 'PERMANENTE' },
    });
    expect(nueva.ok()).toBeTruthy();
    odontograma = await nueva.json();
  }
  odontogramaId = odontograma.id;

  // Limpia restos de corridas anteriores y deja una condición fresca.
  for (const d of odontograma.dientes as { id: number; condicion: string }[]) {
    if (d.condicion === CONDICION) {
      await api.delete(`${API}/historia-clinica/odontogramas/${odontogramaId}/dientes/${d.id}`, {
        headers: { Authorization: `Bearer ${t}` },
      });
    }
  }

  const guardado = await api.post(`${API}/historia-clinica/odontogramas/${odontogramaId}/dientes`, {
    headers: { Authorization: `Bearer ${t}` },
    data: { diente: DIENTE, superficie: 'OCLUSAL', condicion: CONDICION, estado: 'DIAGNOSTICO', notas: 'QA' },
  });
  expect(guardado.ok()).toBeTruthy();
  condicionId = (await guardado.json()).id;
}

test.afterAll(async ({ request }) => {
  if (!odontogramaId || !condicionId) return;
  const t = await token(request);
  await request.delete(`${API}/historia-clinica/odontogramas/${odontogramaId}/dientes/${condicionId}`, {
    headers: { Authorization: `Bearer ${t}` },
  });
});

test.describe('Odontograma interactivo (Fase C)', () => {
  test('dibuja las piezas FDI y permite editar una condición', async ({ page, request }) => {
    test.setTimeout(90_000);
    await preparar(request);
    await login(page);

    await page.goto('./pacientes/1/historia');
    await page.waitForSelector('h1', { timeout: 15000 });
    await page.getByRole('tab', { name: 'Odontograma' }).click();

    // Arcadas permanentes: 32 piezas × 5 regiones = 160 polígonos.
    const regiones = page.locator('.odo-svg polygon.region');
    await expect(regiones.first()).toBeVisible({ timeout: 15000 });
    await expect(regiones).toHaveCount(160);

    // La condición preparada aparece en la tabla con su estado.
    const fila = page.locator('table.tbl tbody tr', { hasText: 'Caries Qa' });
    await expect(fila).toHaveCount(1);
    await expect(fila.locator('.badge')).toHaveText('Diagnóstico');

    // Editar desde la tabla: cambia el estado y se refleja al instante.
    await accionFila(page, fila, 'Editar');
    const dialogo = page.locator('[role="dialog"]');
    await expect(dialogo).toBeVisible();
    await expect(dialogo.locator('h2')).toHaveText('Editar condición');
    await dialogo.locator('#odo-estado').selectOption({ label: 'Completado' });
    await dialogo.getByRole('button', { name: 'Guardar' }).click();
    await expect(dialogo).toBeHidden();
    await expect(fila.locator('.badge')).toHaveText('Completado');

    // El log de cambios de la versión registra la modificación.
    const log = page.locator('details.odo-log');
    await log.locator('summary').click();
    await expect(log.locator('li', { hasText: 'Modificado' }).first()).toBeVisible();

    // Comparación entre versiones (sólo si el historial tiene ≥2).
    const opciones = await page.locator('#odo-version option').count();
    if (opciones >= 2) {
      await page.getByRole('button', { name: 'Comparar' }).click();
      await expect(page.locator('.odo-comparacion')).toBeVisible();
    }
  });

  test('recepción con ODONTOGRAMA_READ ve la pestaña sin poder editar', async ({ page, request }) => {
    test.setTimeout(90_000);
    const t = await token(request);

    // ODONTOLOGO tiene ODONTOGRAMA_WRITE (edita); el rol sin escritura es RECEPCION.
    // Usuario idempotente: si de corridas anteriores ya existe, se reutiliza.
    const existe = await request.post(`${API}/auth/login`, {
      data: { username: 'qa.recep.lectura', password: 'QaOdonto123' },
    });
    if (!existe.ok()) {
      const creacion = await request.post(`${API}/usuarios`, {
        headers: { Authorization: `Bearer ${t}` },
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

    await page.goto('./');
    await page.waitForSelector('input[name="username"]', { timeout: 10000 });
    await page.fill('input[name="username"]', 'qa.recep.lectura');
    await page.fill('input[name="password"]', 'QaOdonto123');
    await page.press('input[name="password"]', 'Enter');
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    await page.goto('./pacientes/1/historia');
    await page.waitForSelector('h1', { timeout: 15000 });
    await expect(page.getByRole('tab', { name: 'Odontograma' })).toBeVisible();

    // RECEPCION tiene ODONTOGRAMA_READ (ve la pestaña y el odontograma)
    // pero no ODONTOGRAMA_WRITE: no aparecen los botones de edición.
    await page.getByRole('tab', { name: 'Odontograma' }).click();
    await expect(page.locator('.odo-svg').first()).toBeVisible({ timeout: 15000 });
    await expect(page.getByRole('button', { name: '+ Nueva versión' })).toHaveCount(0);
    const tabla = page
      .locator('table.tbl')
      .filter({ has: page.locator('th', { hasText: 'Superficie' }) });
    await expect(tabla.locator('button.menu-trigger')).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Crear primera versión' })).toHaveCount(0);
  });
});
