import { expect, test, Page, APIRequestContext } from '@playwright/test';

const API = '/api/v1';
const OBS_ADMIN = 'Consentimiento emitido por el e2e (Fase E, §4.13)';
const OBS_BORRADOR = 'Borrador de prueba para el e2e de consentimientos';

interface Resumen {
  id: number;
  fecha: string;
  titulo: string;
  plantillaCodigo: string;
  plantillaVersion: number;
  estado: string;
  firmante: string | null;
}

interface Detalle extends Resumen {
  contenido: string;
  tutorNombre: string | null;
  tutorId: number | null;
  tutorParentesco: string | null;
  tutorTelefono: string | null;
  firmaImagen: string | null;
  firmaHash: string | null;
  observaciones: string | null;
}

async function login(page: Page) {
  await page.goto('./');
  await page.waitForSelector('input[name="username"]', { timeout: 10000 });
  await page.fill('input[name="username"]', 'admin');
  await page.fill('input[name="password"]', 'admin123');
  await page.press('input[name="password"]', 'Enter');
  await page.waitForURL('**/dashboard', { timeout: 10000 });
}

async function ingresar(page: Page, username: string, password: string) {
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

const auth = (t: string) => ({ Authorization: `Bearer ${t}` });

async function historial(api: APIRequestContext, t: string, paciente = 1): Promise<Resumen[]> {
  const r = await api.get(`${API}/historia-clinica/pacientes/${paciente}/consentimientos`, { headers: auth(t) });
  expect(r.ok()).toBeTruthy();
  return (await r.json()) as Resumen[];
}

async function detalle(api: APIRequestContext, t: string, id: number): Promise<Detalle> {
  const r = await api.get(`${API}/historia-clinica/consentimientos/${id}`, { headers: auth(t) });
  expect(r.ok()).toBeTruthy();
  return (await r.json()) as Detalle;
}

/** Borrador idempotente para el test de sólo lectura (recepción). */
async function asegurarBorrador(api: APIRequestContext, t: string): Promise<Resumen> {
  const todos = await historial(api, t);
  const borradores = todos.filter((c) => c.estado === 'BORRADOR');
  for (const b of borradores) {
    const d = await detalle(api, t, b.id);
    if (d.observaciones === OBS_BORRADOR) return b;
  }
  if (borradores.length) return borradores[0];

  const creado = await api.post(`${API}/historia-clinica/pacientes/1/consentimientos`, {
    headers: auth(t),
    data: {
      plantillaCodigo: 'TRATAMIENTO_GENERAL',
      fecha: '2026-10-05',
      tutorNombre: 'Tutora QA',
      odontologoId: null,
      observaciones: OBS_BORRADOR,
    },
  });
  expect(creado.ok()).toBeTruthy();
  return (await creado.json()) as Resumen;
}

async function buscarPorObservacion(
  api: APIRequestContext,
  t: string,
  observacion: string,
  paciente = 1,
): Promise<Resumen | undefined> {
  const todos = await historial(api, t, paciente);
  const detalles = await Promise.all(todos.map((c) => detalle(api, t, c.id)));
  const encontrado = detalles.find((d) => d.observaciones === observacion);
  return encontrado;
}

async function abrirPestanaConsentimientos(page: Page) {
  await page.goto('./pacientes/1/historia');
  await page.waitForSelector('h1', { timeout: 15000 });
  await page.getByRole('tab', { name: 'Consentimientos' }).click();
}

async function usuarioRecepcion(api: APIRequestContext, t: string) {
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

test.describe('Consentimientos (Fase E, §4.13)', () => {
  test('emite, firma y descarga un consentimiento desde la interfaz', async ({ page, request }) => {
    test.setTimeout(150_000);
    const t = await token(request);

    await login(page);
    await abrirPestanaConsentimientos(page);

    // Las plantillas del catálogo están disponibles para emitir.
    await page.getByRole('button', { name: '+ Nuevo consentimiento' }).click();
    const dialogo = page.locator('[role="dialog"]').filter({ hasText: 'Nuevo consentimiento' });
    await expect(dialogo).toBeVisible();
    await expect(dialogo.locator('#con-plantilla option')).not.toHaveCount(0);
    await dialogo.locator('#con-plantilla').selectOption('TRATAMIENTO_GENERAL');
    await dialogo.locator('#con-tutor').fill('María (e2e)');
    await dialogo.locator('#con-obs').fill(OBS_ADMIN);
    await dialogo.getByRole('button', { name: 'Emitir borrador' }).click();
    await expect(dialogo).toBeHidden({ timeout: 15000 });
    await expect(page.locator('.msg.ok')).toContainText('Consentimiento No.', { timeout: 15000 });

    const emitido = await buscarPorObservacion(request, t, OBS_ADMIN);
    expect(emitido).toBeTruthy();
    expect(emitido!.estado).toBe('BORRADOR');

    // Detalle: texto congelado de la plantilla y acciones de borrador.
    const panel = page.locator('section.detalle');
    await expect(panel).toBeVisible({ timeout: 15000 });
    await expect(panel).toContainText('TRATAMIENTO_GENERAL');
    await expect(panel.locator('.contenido p').first()).toBeVisible();
    await expect(panel).toContainText('Borrador');
    await expect(panel.getByRole('button', { name: 'Editar' })).toBeVisible();

    // El catálogo de plantillas se consulta y se cierra sin guardar.
    await page.getByRole('button', { name: 'Plantillas' }).click();
    const plantillas = page.locator('[role="dialog"]').filter({ hasText: 'Plantillas de consentimiento' });
    await expect(plantillas).toBeVisible();
    await expect(plantillas.locator('#tpl-contenido')).not.toHaveValue('');
    await plantillas.getByRole('button', { name: 'Cancelar' }).click();
    await expect(plantillas).toBeHidden();

    // Firma: nombre obligatorio y lienzo opcional; al firmar se congela.
    await panel.getByRole('button', { name: 'Firmar', exact: true }).click();
    const firma = page.locator('[role="dialog"]').filter({ hasText: 'Firmar consentimiento No.' });
    await expect(firma).toBeVisible();
    await firma.locator('#firm-nombre').fill('');
    await expect(firma.getByRole('button', { name: 'Firmar consentimiento' })).toBeDisabled();

    const lienzo = firma.locator('#firma-lienzo');
    const caja = await lienzo.boundingBox();
    expect(caja).toBeTruthy();
    await page.mouse.move(caja!.x + 30, caja!.y + 70);
    await page.mouse.down();
    await page.mouse.move(caja!.x + 180, caja!.y + 40, { steps: 12 });
    await page.mouse.move(caja!.x + 330, caja!.y + 90, { steps: 12 });
    await page.mouse.up();
    await firma.locator('#firm-nombre').fill('María (e2e)');
    await firma.getByRole('button', { name: 'Firmar consentimiento' }).click();
    await expect(firma).toBeHidden({ timeout: 15000 });

    const firmado = await detalle(request, t, emitido!.id);
    expect(firmado.estado).toBe('FIRMADO');
    expect(firmado.firmante).toBe('María (e2e)');
    expect(firmado.firmaHash).toMatch(/^[0-9a-f]{64}$/);
    expect(firmado.firmaImagen).toMatch(/^data:image\/png;base64,/);

    await expect(panel).toContainText('Firmado');
    await expect(panel.locator('.hash')).toHaveText(new RegExp('^[0-9a-f]{64}$'));
    await expect(panel.getByRole('button', { name: 'Editar' })).toHaveCount(0);
    await expect(panel.getByRole('button', { name: 'Eliminar' })).toHaveCount(0);

    // PDF con huella y nombre de archivo
    const pdf = await request.get(`${API}/historia-clinica/consentimientos/${emitido!.id}/pdf`, {
      headers: auth(t),
    });
    expect(pdf.ok()).toBeTruthy();
    expect(pdf.headers()['content-type']).toContain('application/pdf');
    expect(pdf.headers()['content-disposition']).toContain(`consentimiento-${emitido!.id}.pdf`);
    const cuerpo = await pdf.body();
    expect(cuerpo.subarray(0, 5).toString('latin1')).toBe('%PDF-');
    expect(cuerpo.byteLength).toBeGreaterThan(800);

    // Un consentimiento firmado no admite edición ni borrado por API.
    const put = await request.put(`${API}/historia-clinica/consentimientos/${emitido!.id}`, {
      headers: auth(t),
      data: { titulo: 'Intento de edición' },
    });
    expect(put.status()).toBe(409);
    const del = await request.delete(`${API}/historia-clinica/consentimientos/${emitido!.id}`, {
      headers: auth(t),
    });
    expect(del.status()).toBe(409);
  });

  test('recepción consulta consentimientos sin poder emitirlos ni firmarlos', async ({ page, request }) => {
    test.setTimeout(150_000);
    const t = await token(request);
    await asegurarBorrador(request, t);
    await usuarioRecepcion(request, t);

    await ingresar(page, 'qa.recep.lectura', 'QaOdonto123');
    await abrirPestanaConsentimientos(page);

    // CONSENTIMIENTOS_READ: la lista carga y el detalle se puede leer e imprimir.
    await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 15000 });
    await page.locator('tbody tr').first().locator('button.btn-link').first().click();
    const panel = page.locator('section.detalle');
    await expect(panel).toBeVisible({ timeout: 15000 });
    await expect(panel.locator('.contenido p').first()).toBeVisible();
    await expect(page.getByRole('button', { name: 'Descargar PDF' })).toBeVisible();

    // Sin CONSENTIMIENTOS_WRITE no hay emisión, edición, firma ni plantillas.
    await expect(page.getByRole('button', { name: '+ Nuevo consentimiento' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Plantillas' })).toHaveCount(0);
    await expect(panel.getByRole('button', { name: 'Editar' })).toHaveCount(0);
    await expect(panel.getByRole('button', { name: 'Firmar' })).toHaveCount(0);
    await expect(panel.getByRole('button', { name: 'Eliminar' })).toHaveCount(0);
  });
});

test.describe('Menores de edad (Fase E, §4.14)', () => {
  const OBS_MENOR = `Menor de edad sin tutor en el formulario (§4.14, ${Date.now()})`;

  test('el consentimiento de un menor exige tutor y sólo su tutor puede firmar', async ({ page, request }) => {
    test.setTimeout(150_000);
    const t = await token(request);

    // El backend rechaza emitir de un menor sin tutor y con tutor ajeno.
    const sinTutor = await request.post(`${API}/historia-clinica/pacientes/11/consentimientos`, {
      headers: auth(t),
      data: { plantillaCodigo: 'TRATAMIENTO_GENERAL', fecha: '2026-10-05' },
    });
    expect(sinTutor.status()).toBe(400);
    expect((await sinTutor.json()).code).toBe('TUTOR_REQUERIDO');

    const tutorAjeno = await request.post(`${API}/historia-clinica/pacientes/11/consentimientos`, {
      headers: auth(t),
      data: { plantillaCodigo: 'TRATAMIENTO_GENERAL', fecha: '2026-10-05', tutorId: 999999 },
    });
    expect(tutorAjeno.status()).toBe(404);
    expect((await tutorAjeno.json()).code).toBe('TUTOR_NO_ENCONTRADO');

    await login(page);
    await page.goto('./pacientes/11/historia');
    await page.waitForSelector('h1', { timeout: 15000 });
    await page.getByRole('tab', { name: 'Consentimientos' }).click();

    // El formulario ofrece los tutores registrados del paciente.
    await page.getByRole('button', { name: '+ Nuevo consentimiento' }).click();
    const dialogo = page.locator('[role="dialog"]').filter({ hasText: 'Nuevo consentimiento' });
    await expect(dialogo).toBeVisible();
    await dialogo.locator('#con-plantilla').selectOption('TRATAMIENTO_GENERAL');
    const opcionesTutor = dialogo.getByRole('option', { name: /Annie Charlotte Guerrero Almeida \(MADRE\)/ });
    await expect(opcionesTutor).toHaveCount(1);
    await dialogo.locator('#con-tutor-sel').selectOption({ label: 'Annie Charlotte Guerrero Almeida (MADRE)' });
    await expect(dialogo.locator('#con-tutor')).toHaveCount(0);
    await dialogo.locator('#con-obs').fill(OBS_MENOR);
    await dialogo.getByRole('button', { name: 'Emitir borrador' }).click();
    await expect(dialogo).toBeHidden({ timeout: 15000 });
    await expect(page.locator('.msg.ok')).toContainText('Consentimiento No.', { timeout: 15000 });

    // El consentimiento guarda el snapshot del tutor.
    const emitido = await buscarPorObservacion(request, t, OBS_MENOR, 11);
    expect(emitido).toBeTruthy();
    const d = await detalle(request, t, emitido!.id);
    expect(d.estado).toBe('BORRADOR');
    expect(d.tutorNombre).toBe('Annie Charlotte Guerrero Almeida');
    expect(d.tutorId).toBe(4);
    expect(d.tutorParentesco).toBe('MADRE');

    // El detalle de la pestaña muestra al tutor.
    const panel = page.locator('section.detalle');
    await expect(panel).toBeVisible({ timeout: 15000 });
    await expect(panel).toContainText('Tutor: Annie Charlotte Guerrero Almeida');
    await expect(panel).toContainText('MADRE');

    // Sólo el tutor puede firmar.
    await panel.getByRole('button', { name: 'Firmar', exact: true }).click();
    const firma = page.locator('[role="dialog"]').filter({ hasText: 'Firmar consentimiento No.' });
    await expect(firma).toBeVisible();
    await firma.locator('#firm-nombre').fill('Pedro Pérez');
    await firma.getByRole('button', { name: 'Firmar consentimiento' }).click();
    const avisoFirma = firma.locator('.msg.error', { hasText: 'Sólo el tutor registrado' });
    await expect(avisoFirma).toHaveCount(1, { timeout: 10000 });
    await expect(firma).toBeVisible();

    await firma.locator('#firm-nombre').fill('  annie   charlotte GUERRERO ALMEIDA ');
    await firma.getByRole('button', { name: 'Firmar consentimiento' }).click();
    await expect(firma).toBeHidden({ timeout: 15000 });

    const firmado = await detalle(request, t, emitido!.id);
    expect(firmado.estado).toBe('FIRMADO');
    expect(firmado.firmante).toBe('Annie Charlotte Guerrero Almeida');
    expect(firmado.firmaHash).toMatch(/^[0-9a-f]{64}$/);

    const pdf = await request.get(`${API}/historia-clinica/consentimientos/${emitido!.id}/pdf`, {
      headers: auth(t),
    });
    expect(pdf.ok()).toBeTruthy();
    const cuerpo = await pdf.body();
    expect(cuerpo.subarray(0, 5).toString('latin1')).toBe('%PDF-');
    expect(cuerpo.byteLength).toBeGreaterThan(800);
  });
});
