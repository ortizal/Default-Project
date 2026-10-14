import { expect, test, Page, APIRequestContext } from '@playwright/test';
import { accionFila } from './actions';

const API = '/api/v1';
const DIENTE = 46;
const SITIO = 'MESIAL';

let examenId = 0;

async function login(page: Page, username: string, password: string) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="password"]', password);
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

async function token(api: APIRequestContext): Promise<string> {
  const r = await api.post(`${API}/auth/login`, { data: { username: 'admin', password: 'admin123' } });
  expect(r.ok()).toBeTruthy();
  return (await r.json()).token as string;
}

/**
 * Estado conocido para la prueba idempotente: al menos dos exámenes
 * (para poder comparar) y la pieza 46 con la medición MESIAL a 4 mm.
 */
async function preparar(api: APIRequestContext) {
  const t = await token(api);
  const headers = { Authorization: `Bearer ${t}` };

  let historial = await api.get(`${API}/historia-clinica/pacientes/1/periodontogramas`, { headers });
  expect(historial.ok()).toBeTruthy();
  let examenes = (await historial.json()) as { id: number }[];

  if (examenes.length === 0) {
    const creado = await api.post(`${API}/historia-clinica/pacientes/1/periodontogramas`, {
      headers,
      data: { observaciones: 'Periodontograma de QA', copiarUltima: false },
    });
    expect(creado.ok()).toBeTruthy();
    examenes = [await creado.json()];
  }
  if (examenes.length === 1) {
    const segundo = await api.post(`${API}/historia-clinica/pacientes/1/periodontogramas`, {
      headers,
      data: { observaciones: 'Control de QA', copiarUltima: false },
    });
    expect(segundo.ok()).toBeTruthy();
  }

  const vigente = await api.get(`${API}/historia-clinica/pacientes/1/periodontograma`, { headers });
  expect(vigente.ok()).toBeTruthy();
  const examen = await vigente.json();
  examenId = examen.id;

  const guardado = await api.put(`${API}/historia-clinica/periodontogramas/${examenId}/mediciones`, {
    headers,
    data: {
      sitios: [
        { diente: DIENTE, sitio: SITIO, sondajeMm: 4, margenMm: null, insercionMm: null, sangrado: false, placa: false, supuracion: false },
      ],
      dientes: [],
    },
  });
  expect(guardado.ok()).toBeTruthy();
}

test.describe('Periodontograma (Fase E)', () => {
  test('registra mediciones de una pieza y compara dos exámenes', async ({ page, request }) => {
    test.setTimeout(120_000);
    await preparar(request);
    await login(page, 'admin', 'admin123');

    await page.goto('./pacientes/1/historia');
    await page.waitForSelector('h1', { timeout: 15000 });
    await page.getByRole('tab', { name: 'Periodoncia' }).click();

    await expect(page.getByText('Exámenes periodontales (')).toBeVisible({ timeout: 15000 });
    await expect(page.locator('table.periodontograma th', { hasText: 'Diente' })).toBeVisible();

    // La medición preparada aparece en la celda diente/sitio.
    const celda = page.locator('td[data-diente="46"][data-sitio="MESIAL"]');
    await expect(celda).toBeVisible({ timeout: 15000 });
    await expect(celda).toContainText('4');

    // Editar mediciones: cambia el sondaje a 4.5 mm y se refleja al instante.
    const fila = page
      .locator('table.periodontograma tbody tr')
      .filter({ has: page.locator('th.diente', { hasText: /^46$/ }) });
    await accionFila(page, fila, 'Editar mediciones');

    const dialogo = page.locator('[role="dialog"]');
    await expect(dialogo).toBeVisible();
    await expect(dialogo.locator('h2')).toHaveText('Mediciones de la pieza 46');
    await dialogo.locator('#med-sondaje-MESIAL').fill('4.5');
    await dialogo.getByRole('button', { name: 'Guardar', exact: true }).click();
    await expect(dialogo).toBeHidden();
    await expect(celda).toContainText('4.5/–/–', { timeout: 15000 });

    // Comparación entre las dos versiones preparadas.
    await page.getByRole('button', { name: 'Comparar versiones' }).click();
    const cmp = page.locator('[role="dialog"]');
    await expect(cmp.locator('h2')).toHaveText('Comparar exámenes periodontales');
    await cmp.getByRole('button', { name: 'Comparar', exact: true }).click();
    await expect(cmp.locator('.totales')).toBeVisible({ timeout: 15000 });
    await expect(cmp.getByText('Cambios:')).toBeVisible();

    // El reporte periodontal se descarga en PDF desde el servidor.
    const pdf = await request.get(`${API}/historia-clinica/periodontogramas/${examenId}/pdf`, {
      headers: { Authorization: `Bearer ${await token(request)}` },
    });
    expect(pdf.ok()).toBeTruthy();
    expect(pdf.headers()['content-type']).toContain('application/pdf');
    expect(pdf.headers()['content-disposition']).toContain('attachment');
  });

  test('recepción con PERIODONCIA_READ ve la pestaña sin poder editar', async ({ page, request }) => {
    test.setTimeout(90_000);
    await preparar(request);
    const t = await token(request);

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

    await login(page, 'qa.recep.lectura', 'QaOdonto123');
    await page.goto('./pacientes/1/historia');
    await page.waitForSelector('h1', { timeout: 15000 });
    await expect(page.getByRole('tab', { name: 'Periodoncia' })).toBeVisible();

    await page.getByRole('tab', { name: 'Periodoncia' }).click();
    await expect(page.locator('table.periodontograma')).toBeVisible({ timeout: 15000 });

    // PERIODONCIA_READ sin escritura: no hay altas ni botones de edición.
    await expect(page.getByRole('button', { name: '+ Nuevo examen' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Editar examen' })).toHaveCount(0);
    await expect(page.locator('table.periodontograma button.menu-trigger')).toHaveCount(0);
  });
});
