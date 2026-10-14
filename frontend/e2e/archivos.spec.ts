import { expect, test, Page, APIRequestContext } from '@playwright/test';
import { abrirAcciones, accionFila } from './actions';

const API = '/api/v1';
const OBS = 'E2E §4.15 — archivos clínicos';

interface Archivo {
  id: number;
  categoria: string;
  nombre: string;
  mime: string;
  tamano: number;
  sha256: string | null;
  fecha: string | null;
  observaciones: string | null;
}

const PNG_1x1 = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64',
);

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

async function listado(api: APIRequestContext, t: string, paciente = 1): Promise<Archivo[]> {
  const r = await api.get(`${API}/historia-clinica/pacientes/${paciente}/archivos`, {
    headers: auth(t),
  });
  expect(r.ok()).toBeTruthy();
  return (await r.json()) as Archivo[];
}

/** Limpia los archivos dejados por corridas anteriores de este spec. */
async function limpiarPrevios(api: APIRequestContext, t: string): Promise<void> {
  const previos = await listado(api, t);
  for (const a of previos.filter((x) => x.nombre.startsWith('e2e-archivos-'))) {
    await api.delete(`${API}/historia-clinica/archivos/${a.id}`, { headers: auth(t) });
  }
}

async function abrirPestanaArchivos(page: Page) {
  await page.goto('./pacientes/1/historia');
  await page.waitForSelector('h1', { timeout: 15000 });
  await page.getByRole('tab', { name: 'Archivos' }).click();
}

test.describe('Archivos clínicos (Fase E, §4.15)', () => {
  test('sube, previsualiza, descarga y elimina un archivo desde la interfaz', async ({
    page,
    request,
  }) => {
    test.setTimeout(150_000);
    const t = await token(request);
    await limpiarPrevios(request, t);

    const nombre = `e2e-archivos-${Date.now()}.png`;

    await login(page);
    await abrirPestanaArchivos(page);

    // Formulario de subida con la clasificación clínica.
    await expect(page.locator('#arc-archivo')).toBeVisible({ timeout: 15000 });
    await page.setInputFiles('#arc-archivo', {
      name: nombre,
      mimeType: 'image/png',
      buffer: PNG_1x1,
    });
    await page.selectOption('#arc-categoria', 'FOTOGRAFIA');
    await page.fill('#arc-fecha', '2026-10-05');
    await page.fill('#arc-obs', OBS);
    await page.getByRole('button', { name: 'Subir archivo' }).click();
    await expect(page.locator('.msg.ok')).toContainText('subido correctamente', { timeout: 15000 });

    // El listado refleja la metadata y el texto cliente-side valida el formato.
    await expect(page.locator('tbody tr', { hasText: nombre }).first()).toBeVisible({ timeout: 15000 });
    await page.setInputFiles('#arc-archivo', {
      name: 'no-permitido.exe',
      mimeType: 'application/octet-stream',
      buffer: Buffer.from('MZ'),
    });
    await expect(page.locator('.msg.error')).toContainText('Formato no permitido');
    await expect(page.getByRole('button', { name: 'Subir archivo' })).toBeDisabled();

    const subido = (await listado(request, t)).find((a) => a.nombre === nombre);
    expect(subido).toBeTruthy();
    expect(subido!.categoria).toBe('FOTOGRAFIA');
    expect(subido!.fecha).toBe('2026-10-05');
    expect(subido!.observaciones).toBe(OBS);
    expect(subido!.mime).toBe('image/png');
    expect(subido!.tamano).toBe(PNG_1x1.length);
    expect(subido!.sha256).toMatch(/^[0-9a-f]{64}$/);

    // Vista previa de la imagen en el diálogo.
    const fila = page.locator('tbody tr', { hasText: nombre }).first();
    await accionFila(page, fila, 'Ver');
    const preview = page.locator('[role="dialog"]').filter({ hasText: nombre });
    await expect(preview).toBeVisible();
    await expect(preview.locator('img')).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(preview).toBeHidden();

    // Descarga con el nombre original.
    await abrirAcciones(page, fila);
    const [descarga] = await Promise.all([
      page.waitForEvent('download'),
      page.getByRole('menuitem', { name: 'Descargar' }).click(),
    ]);
    expect(descarga.suggestedFilename()).toBe(nombre);

    const contenido = await request.get(`${API}/historia-clinica/archivos/${subido!.id}/contenido`, {
      headers: auth(t),
    });
    expect(contenido.ok()).toBeTruthy();
    expect(contenido.headers()['content-type']).toContain('image/png');
    expect((await contenido.body()).subarray(1, 4).toString('latin1')).toBe('PNG');

    // Borrado con confirmación: desaparece de la lista y de la BD.
    await accionFila(page, fila, 'Eliminar');
    const confirmacion = page.locator('[role="dialog"]').filter({ hasText: 'Eliminar archivo' });
    await expect(confirmacion).toBeVisible();
    await confirmacion.getByRole('button', { name: 'Eliminar' }).click();
    await expect(page.locator('tbody tr', { hasText: nombre })).toHaveCount(0, { timeout: 15000 });

    const restantes = await listado(request, t);
    expect(restantes.find((a) => a.nombre === nombre)).toBeUndefined();

    // El contenido ya no existe en el almacenamiento.
    if (subido) {
      const trasBorrar = await request.get(
        `${API}/historia-clinica/archivos/${subido.id}/contenido`,
        { headers: auth(t) },
      );
      expect(trasBorrar.status()).toBe(404);
    }
  });

  test('recepción consulta y descarga archivos sin poder subirlos ni borrarlos', async ({
    page,
    request,
  }) => {
    test.setTimeout(150_000);
    const t = await token(request);
    const previos = await listado(request, t);
    let nombre = previos.find((a) => a.observaciones === OBS)?.nombre;
    if (!nombre) {
      nombre = `e2e-archivos-lectura-${Date.now()}.png`;
      const creado = await request.post(
        `${API}/historia-clinica/pacientes/1/archivos`,
        {
          headers: auth(t),
          multipart: {
            archivo: { name: nombre, mimeType: 'image/png', buffer: PNG_1x1 },
            categoria: 'FOTOGRAFIA',
            observaciones: OBS,
          },
        },
      );
      expect(creado.ok()).toBeTruthy();
    }

    await ingresar(page, 'qa.recep.lectura', 'QaOdonto123');
    await abrirPestanaArchivos(page);

    const filaLectura = page.locator('tbody tr', { hasText: nombre }).first();
    await expect(filaLectura).toBeVisible({ timeout: 15000 });

    // El menú de la fila ofrece la descarga (lectura) pero no el borrado.
    await abrirAcciones(page, filaLectura);
    await expect(page.getByRole('menuitem', { name: 'Descargar' })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: 'Eliminar' })).toHaveCount(0);
    await page.keyboard.press('Escape');
    await expect(page.locator('[role="menu"]')).toHaveCount(0);

    // Sin ARCHIVOS_CLINICOS_WRITE no hay formulario ni borrado.
    await expect(page.locator('#arc-archivo')).toHaveCount(0);
    await expect(page.locator('#arc-categoria')).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Subir archivo' })).toHaveCount(0);
  });
});
