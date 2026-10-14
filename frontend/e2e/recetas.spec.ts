import { expect, test, Page, APIRequestContext } from '@playwright/test';

const API = '/api/v1';
const DIAGNOSTICO_API = 'Gingivitis crónica (QA Fase E)';
const DIAGNOSTICO_UI = 'Receta creada desde la interfaz (QA e2e)';

interface Resumen {
  id: number;
  fecha: string;
  estado: string;
  diagnostico: string | null;
  lineas: number;
}

interface Detalle extends Resumen {
  motivoAnulacion: string | null;
  lineas: { numero: number; medicamento: string; dosis: string; via: string | null }[];
}

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

async function resumenes(api: APIRequestContext, t: string): Promise<Resumen[]> {
  const r = await api.get(`${API}/historia-clinica/pacientes/1/recetas`, { headers: auth(t) });
  expect(r.ok()).toBeTruthy();
  return (await r.json()) as Resumen[];
}

async function detalle(api: APIRequestContext, t: string, id: number): Promise<Detalle> {
  const r = await api.get(`${API}/historia-clinica/recetas/${id}`, { headers: auth(t) });
  expect(r.ok()).toBeTruthy();
  return (await r.json()) as Detalle;
}

/** Receta idempotente: se crea sólo si ninguna corrida anterior la dejó. */
async function asegurarReceta(api: APIRequestContext, t: string): Promise<Resumen> {
  const existente = (await resumenes(api, t)).find((r) => r.diagnostico === DIAGNOSTICO_API);
  if (existente) return existente;
  const creado = await api.post(`${API}/historia-clinica/pacientes/1/recetas`, {
    headers: auth(t),
    data: {
      fecha: '2026-10-05',
      odontologoId: null,
      diagnostico: DIAGNOSTICO_API,
      observaciones: 'Receta creada por el e2e de la Fase E',
      lineas: [
        {
          medicamento: 'Amoxicilina',
          concentracion: '500 mg',
          presentacion: 'Cápsula',
          dosis: '1 cápsula',
          via: 'ORAL',
          frecuencia: 'Cada 8 h',
          duracion: '7 días',
          instrucciones: 'Tomar con alimentos',
        },
      ],
    },
  });
  expect(creado.ok()).toBeTruthy();
  return (await creado.json()) as Resumen;
}

async function abrirPestanaRecetas(page: Page) {
  await page.goto('./pacientes/1/historia');
  await page.waitForSelector('h1', { timeout: 15000 });
  await page.getByRole('tab', { name: 'Recetas' }).click();
}

async function seleccionarReceta(page: Page, diagnostico: string) {
  const fila = page.locator('tbody tr', { hasText: diagnostico }).first();
  await expect(fila).toBeVisible({ timeout: 15000 });
  await fila.locator('button.btn-link').first().click();
  const panel = page.locator('.detalle-receta');
  await expect(panel).toBeVisible({ timeout: 15000 });
  await expect(panel).toContainText(diagnostico);
  return panel;
}

async function usuarioRecepcion(api: APIRequestContext, t: string) {
  // Usuario idempotente: si ya existe de corridas anteriores se reutiliza.
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

async function ingresar(page: Page, username: string, password: string) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', username);
  await page.fill('input[name="password"]', password);
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

test.describe('Recetas (Fase E, §4.12)', () => {
  test('crea, anula y descarga una receta desde la interfaz', async ({ page, request }) => {
    test.setTimeout(120_000);
    const t = await token(request);
    const receta = await asegurarReceta(request, t);

    await login(page);
    await abrirPestanaRecetas(page);
    const panel = await seleccionarReceta(page, DIAGNOSTICO_API);
    await expect(panel.locator('table.tbl tbody tr', { hasText: 'Amoxicilina' })).toBeVisible();

    // Alta desde la interfaz con un medicamento nuevo
    await page.getByRole('button', { name: '+ Nueva receta' }).click();
    const dialogo = page.locator('[role="dialog"]').filter({ hasText: 'Nueva receta' });
    await expect(dialogo).toBeVisible();
    await dialogo.locator('#rec-diagnostico').fill(DIAGNOSTICO_UI);
    await dialogo.locator('#lin-med-0').fill('Ibuprofeno');
    await dialogo.locator('#lin-dosis-0').fill('400 mg');
    await dialogo.locator('#lin-frec-0').fill('Cada 8 h');
    await dialogo.locator('#lin-dur-0').fill('5 días');
    await dialogo.getByRole('button', { name: 'Guardar' }).click();
    await expect(dialogo).toBeHidden({ timeout: 15000 });
    await expect(page.locator('.msg.ok')).toContainText('Receta No.', { timeout: 15000 });

    const creadas = (await resumenes(request, t)).filter((r) => r.diagnostico === DIAGNOSTICO_UI);
    expect(creadas.length).toBeGreaterThan(0);
    const creada = creadas[0];

    // La anulación exige motivo y deja la receta marcada
    const filaCreada = await seleccionarReceta(page, DIAGNOSTICO_UI);
    await filaCreada.getByRole('button', { name: 'Anular' }).click();
    const anulacion = page.locator('[role="dialog"]').filter({ hasText: 'Anular receta No.' });
    await expect(anulacion).toBeVisible();
    await expect(anulacion.getByRole('button', { name: 'Anular receta' })).toBeDisabled();
    await anulacion.locator('#anul-motivo').fill('Doble prescripción (e2e)');
    await anulacion.getByRole('button', { name: 'Anular receta' }).click();
    await expect(anulacion).toBeHidden({ timeout: 15000 });

    const despues = await detalle(request, t, creada.id);
    expect(despues.estado).toBe('ANULADA');
    expect(despues.motivoAnulacion).toBe('Doble prescripción (e2e)');
    await expect(page.locator('.detalle-receta')).toContainText('Receta anulada');

    // Descarga del PDF (cabecera clínica + pauta)
    const pdf = await request.get(`${API}/historia-clinica/recetas/${receta.id}/pdf`, {
      headers: auth(t),
    });
    expect(pdf.ok()).toBeTruthy();
    expect(pdf.headers()['content-type']).toContain('application/pdf');
    expect(pdf.headers()['content-disposition']).toContain(`receta-${receta.id}.pdf`);
    const cuerpo = await pdf.body();
    expect(cuerpo.subarray(0, 5).toString('latin1')).toBe('%PDF-');
    expect(cuerpo.byteLength).toBeGreaterThan(500);
  });

  test('recepción consulta e imprime recetas sin poder emitirlas', async ({ page, request }) => {
    test.setTimeout(120_000);
    const t = await token(request);
    await asegurarReceta(request, t);
    await usuarioRecepcion(request, t);

    await ingresar(page, 'qa.recep.lectura', 'QaOdonto123');
    await abrirPestanaRecetas(page);

    // RECETAS_READ: la lista carga y el detalle es visible.
    await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 15000 });
    await page.locator('tbody tr').first().locator('button.btn-link').first().click();
    const panel = page.locator('.detalle-receta');
    await expect(panel).toBeVisible({ timeout: 15000 });
    await expect(page.getByRole('button', { name: 'Descargar PDF' })).toBeVisible();

    // Sin RECETAS_WRITE no hay ninguna acción de emisión o anulación.
    await expect(page.getByRole('button', { name: '+ Nueva receta' })).toHaveCount(0);
    await expect(panel.getByRole('button', { name: 'Editar' })).toHaveCount(0);
    await expect(panel.getByRole('button', { name: 'Anular' })).toHaveCount(0);
  });
});
