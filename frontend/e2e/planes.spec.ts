import { expect, test, Page, APIRequestContext } from '@playwright/test';
import { accionFila } from './actions';

const API = '/api/v1';
const TITULO_EDICION = 'QA edición Fase D';
const TITULO_ACEPTACION = 'QA aceptación Fase D';

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

async function resumenes(api: APIRequestContext, t: string) {
  const r = await api.get(`${API}/planes-tratamiento/pacientes/1`, { headers: auth(t) });
  expect(r.ok()).toBeTruthy();
  return (await r.json()) as {
    id: number;
    titulo: string;
    estado: string;
    lineas: number;
    presupuestos: number;
  }[];
}

async function detalle(api: APIRequestContext, t: string, id: number) {
  const r = await api.get(`${API}/planes-tratamiento/${id}`, { headers: auth(t) });
  expect(r.ok()).toBeTruthy();
  return (await r.json()) as {
    id: number;
    titulo: string;
    estado: string;
    lineas: { id: number; procedimiento: string; subtotal: number }[];
    presupuestos: { id: number; numero: number; estado: string; vigente: boolean; total: number }[];
  };
}

/** Plan idempotente: se crea sólo si ninguna corrida anterior lo dejó. */
async function asegurarPlan(api: APIRequestContext, t: string, titulo: string) {
  const existente = (await resumenes(api, t)).find((p) => p.titulo === titulo);
  if (existente) return existente;
  const creado = await api.post(`${API}/planes-tratamiento/pacientes/1`, {
    headers: auth(t),
    data: { titulo, observaciones: 'Plan creado por el e2e de la Fase D', odontologoId: null, fecha: null },
  });
  expect(creado.ok()).toBeTruthy();
  return (await creado.json()) as { id: number; titulo: string; estado: string };
}

async function asegurarLinea(api: APIRequestContext, t: string, planId: number) {
  const det = await detalle(api, t, planId);
  if (det.lineas.length) return det;
  const creado = await api.post(`${API}/planes-tratamiento/${planId}/lineas`, {
    headers: auth(t),
    data: {
      fase: 1,
      diente: 16,
      superficie: null,
      diagnostico: 'Control',
      procedimiento: 'Tratamiento QA',
      prioridad: 'MEDIA',
      sesiones: 1,
      precioUnitario: 120,
      descuentoPct: 0,
      notas: null,
    },
  });
  expect([200, 201]).toContain(creado.status());
  return detalle(api, t, planId);
}

async function asegurarPresupuestoPresentado(api: APIRequestContext, t: string, planId: number) {
  let det = await detalle(api, t, planId);
  const usable = det.presupuestos.find((p) => p.estado === 'PRESENTADO' || p.estado === 'ACEPTADO');
  if (usable) return { det, presupuesto: usable };
  const creado = await api.post(`${API}/planes-tratamiento/${planId}/presupuestos`, {
    headers: auth(t),
    data: { observaciones: null },
  });
  expect([200, 201]).toContain(creado.status());
  det = await detalle(api, t, planId);
  const presupuesto = det.presupuestos[0];
  expect(presupuesto).toBeTruthy();
  return { det, presupuesto };
}

async function abrirPestanaPlanes(page: Page) {
  await page.goto('./pacientes/1/historia');
  await page.waitForSelector('h1', { timeout: 15000 });
  await page.getByRole('tab', { name: 'Planes' }).click();
}

async function seleccionarPlan(page: Page, titulo: string) {
  const fila = page.locator('tbody tr', { hasText: titulo }).first();
  await expect(fila).toBeVisible({ timeout: 15000 });
  await fila.locator('button.btn-link').first().click();
  const panel = page.locator('.detalle-plan');
  await expect(panel).toBeVisible({ timeout: 15000 });
  await expect(panel).toContainText(titulo);
  return panel;
}

test.describe('Planes de tratamiento (Fase D)', () => {
  test('agrega una línea y emite el presupuesto desde la interfaz', async ({ page, request }) => {
    test.setTimeout(120_000);
    const t = await token(request);
    const plan = await asegurarPlan(request, t, TITULO_EDICION);
    const antes = await detalle(request, t, plan.id);

    await login(page);
    await abrirPestanaPlanes(page);
    const panel = await seleccionarPlan(page, TITULO_EDICION);

    // Línea del plan (sólo borrador admite edición)
    if (antes.estado === 'BORRADOR' && !antes.lineas.some((l) => l.procedimiento === 'Evaluación QA')) {
      await page.getByRole('button', { name: '+ Agregar línea' }).click();
      const dialogo = page.locator('[role="dialog"]').filter({ hasText: 'Nueva línea' });
      await expect(dialogo).toBeVisible();
      await dialogo.locator('#ln-proc').fill('Evaluación QA');
      await dialogo.locator('#ln-precio').fill('150');
      await dialogo.getByRole('button', { name: 'Guardar' }).click();
      await expect(dialogo).toBeHidden();
    }
    await expect(
      panel.locator('table.tbl tbody tr', { hasText: 'Evaluación QA' }),
    ).toBeVisible({ timeout: 15000 });

    // Presupuesto congelado a partir de las líneas vigentes
    if (antes.presupuestos.length === 0) {
      await page.getByRole('button', { name: 'Emitir presupuesto' }).click();
      const confirmacion = page.locator('[role="dialog"]').filter({ hasText: 'Se congela' });
      await expect(confirmacion).toBeVisible();
      await confirmacion.getByRole('button', { name: 'Emitir' }).click();

      const modal = page.locator('[role="dialog"]').filter({ hasText: 'Presupuesto No.' });
      await expect(modal).toBeVisible({ timeout: 15000 });
      await expect(modal).toContainText('$ 150.00'); // 150 × 1 sesión sin descuento
      await modal.getByRole('button', { name: 'Cerrar' }).click();
      await expect(modal).toBeHidden();
    }

    const despues = await detalle(request, t, plan.id);
    expect(despues.lineas.length).toBeGreaterThanOrEqual(1);
    expect(despues.presupuestos.length).toBeGreaterThanOrEqual(1);
    expect(despues.presupuestos[0].estado).toBe('PRESENTADO');
    expect(despues.presupuestos[0].total).toBeGreaterThan(0);
  });

  test('acepta el presupuesto con firma y el plan queda aceptado', async ({ page, request }) => {
    test.setTimeout(120_000);
    const t = await token(request);
    const plan = await asegurarPlan(request, t, TITULO_ACEPTACION);
    await asegurarLinea(request, t, plan.id);
    const { presupuesto } = await asegurarPresupuestoPresentado(request, t, plan.id);
    let det = await detalle(request, t, plan.id);

    if (det.estado === 'ACEPTADO') {
      // Corridas previas ya lo aceptaron: sólo se re-verifica la firma.
      const r = await request.get(
        `${API}/planes-tratamiento/${plan.id}/presupuestos/${presupuesto.id}`,
        { headers: auth(t) },
      );
      expect(r.ok()).toBeTruthy();
      const p = await r.json();
      expect(p.firmaHash).toHaveLength(64);
      expect(p.vigente).toBe(true);
      return;
    }

    await login(page);
    await abrirPestanaPlanes(page);
    await seleccionarPlan(page, TITULO_ACEPTACION);

    const tablaPres = page
      .locator('table.tbl')
      .filter({ has: page.locator('th', { hasText: 'Firmante' }) });
    await accionFila(page, tablaPres.locator('tbody tr').first(), 'Ver detalle');
    const modal = page.locator('[role="dialog"]').filter({ hasText: 'Presupuesto No.' });
    await expect(modal).toBeVisible();
    await modal.getByRole('button', { name: 'Aceptar' }).click();

    const firma = page.locator('[role="dialog"]').filter({ hasText: 'Aceptar presupuesto' });
    await expect(firma).toBeVisible();
    await firma.locator('#ac-firmante').fill('Paciente QA');
    await firma.getByRole('button', { name: 'Aceptar' }).click();
    await expect(firma).toBeHidden({ timeout: 15000 });

    det = await detalle(request, t, plan.id);
    expect(det.estado).toBe('ACEPTADO');

    const r = await request.get(
      `${API}/planes-tratamiento/${plan.id}/presupuestos/${presupuesto.id}`,
      { headers: auth(t) },
    );
    const p = await r.json();
    expect(p.estado).toBe('ACEPTADO');
    expect(p.vigente).toBe(true);
    expect(p.firmante).toBe('Paciente QA');
    expect(p.firmaHash).toHaveLength(64);
  });

  test('recepción con PLAN_TRATAMIENTO_READ ve los planes sin poder editarlos', async ({ page, request }) => {
    test.setTimeout(120_000);
    const t = await token(request);

    // Usuario idempotente: si ya existe de corridas anteriores se reutiliza.
    const existe = await request.post(`${API}/auth/login`, {
      data: { username: 'qa.recep.lectura', password: 'QaOdonto123' },
    });
    if (!existe.ok()) {
      const creacion = await request.post(`${API}/usuarios`, {
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

    await page.goto('./');
    await page.waitForSelector('input[name="username"]', { timeout: 10000 });
    await page.fill('input[name="username"]', 'qa.recep.lectura');
    await page.fill('input[name="password"]', 'QaOdonto123');
    await page.press('input[name="password"]', 'Enter');
    await page.waitForURL('**/dashboard', { timeout: 10000 });

    await abrirPestanaPlanes(page);

    // READ: la lista carga y el detalle (incluido el PDF) es visible.
    await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 15000 });
    await page.locator('tbody tr').first().locator('button.btn-link').first().click();
    const panel = page.locator('.detalle-plan');
    await expect(panel).toBeVisible({ timeout: 15000 });
    await expect(page.getByRole('button', { name: 'Descargar PDF' })).toBeVisible();

    // WRITE ausente: no hay ninguna acción de creación o edición.
    await expect(page.getByRole('button', { name: '+ Nuevo plan' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: '+ Agregar línea' })).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Emitir presupuesto' })).toHaveCount(0);
    await expect(page.locator('td.acciones button.menu-trigger')).toHaveCount(0);
  });
});
