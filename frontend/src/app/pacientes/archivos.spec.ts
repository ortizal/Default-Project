import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { UiConfirmService } from '../ui';
import { ArchivosComponent, esImagen, etiquetaCategoria, extensionArchivo, formatoBytes, toneCategoria } from './archivos';
import { ArchivoClinico } from '../core/models';

function archivo(extra: Partial<ArchivoClinico> = {}): ArchivoClinico {
  return {
    id: 12,
    pacienteId: 7,
    categoria: 'RADIOGRAFIA',
    nombre: 'panoramica.png',
    mime: 'image/png',
    tamano: 2048,
    sha256: 'a'.repeat(64),
    fecha: '2026-10-01',
    observaciones: null,
    subidoEn: '2026-10-05T12:00:00Z',
    ...extra,
  };
}

describe('Archivos — helpers (Fase E, §4.15)', () => {
  it('formatea el tamaño del archivo', () => {
    expect(formatoBytes(0)).toBe('0 B');
    expect(formatoBytes(512)).toBe('512 B');
    expect(formatoBytes(1536)).toBe('1.5 KB');
    expect(formatoBytes(3 * 1024 * 1024)).toBe('3.0 MB');
  });

  it('lee la extensión y decide si se puede previsualizar', () => {
    expect(extensionArchivo('rx.PANORAMICA.dcm')).toBe('dcm');
    expect(extensionArchivo('sin-extension')).toBe('');
    expect(esImagen('image/png')).toBe(true);
    expect(esImagen('application/pdf')).toBe(false);
  });

  it('rotula y colorea las categorías', () => {
    expect(etiquetaCategoria('PANORAMICA')).toBe('Panorámica');
    expect(etiquetaCategoria('TOMOGRAFIA')).toBe('Tomografía');
    expect(toneCategoria('RESULTADO')).toBe('ok');
    expect(toneCategoria('DOCUMENTO')).toBe('muted');
    expect(toneCategoria('RADIOGRAFIA')).toBe('info');
  });
});

describe('Archivos — composición (Fase E, §4.15)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArchivosComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  function componente(): ArchivosComponent {
    return TestBed.createComponent(ArchivosComponent).componentInstance;
  }

  it('arranca sin permiso de escritura, sin archivos y sin previsualización', () => {
    const c = componente();

    expect(c.escribir).toBe(false);
    expect(c.archivos).toEqual([]);
    expect(c.subiendo).toBe(false);
    expect(c.modalPreview).toBe(false);
    expect(c.previewUrl).toBe('');
    expect(c.categoria).toBe('RADIOGRAFIA');
  });

  it('valida el archivo antes de subirlo', () => {
    const c = componente();

    expect(c.errorAlta).toBe('Selecciona un archivo');

    c.archivo = new File(['x'], 'malware.exe', { type: 'application/x-msdownload' });
    expect(c.errorAlta).toContain('Formato no permitido');

    c.archivo = { name: 'rx.png', size: 21 * 1024 * 1024 } as File;
    expect(c.errorAlta).toBe('El archivo no puede superar 20 MB');

    c.archivo = new File(['x'], 'rx.png', { type: 'image/png' });
    expect(c.errorAlta).toBe('');
  });

  it('toma el primer archivo del input y limpia el aviso', () => {
    const c = componente();
    c.error = 'algo';

    const file = new File(['x'], 'frente.jpg', { type: 'image/jpeg' });
    c.seleccionarArchivo({ target: { files: [file] } } as unknown as Event);

    expect(c.archivo).toBe(file);
    expect(c.error).toBe('');
  });

  it('sube el archivo como multipart con su categoría y su fecha', () => {
    const c = componente();
    c.pacienteId = 7;
    c.archivo = new File(['x'], 'panoramica.png', { type: 'image/png' });
    c.categoria = 'PANORAMICA';
    c.fecha = '2026-10-01';
    c.observaciones = '  Control anual  ';

    c.subir();

    const req = httpTesting.expectOne('/api/v1/historia-clinica/pacientes/7/archivos');
    expect(req.request.method).toBe('POST');
    const body = req.request.body as FormData;
    expect(body).toBeInstanceOf(FormData);
    expect((body.get('archivo') as File).name).toBe('panoramica.png');
    expect(body.get('categoria')).toBe('PANORAMICA');
    expect(body.get('fecha')).toBe('2026-10-01');
    expect(body.get('observaciones')).toBe('Control anual');
    expect(c.subiendo).toBe(true);

    req.flush(archivo());
    expect(c.subiendo).toBe(false);
    expect(c.exito).toContain('panoramica.png');
    expect(c.archivo).toBeNull();
  });

  it('no hace la petición si el archivo no es válido', () => {
    const c = componente();
    c.pacienteId = 7;

    c.subir();

    httpTesting.expectNone('/api/v1/historia-clinica/pacientes/7/archivos');
    expect(c.subiendo).toBe(false);
  });

  it('descarga los archivos que no son imagen', () => {
    const c = componente();
    const spy = vi.spyOn(c, 'descargar');

    c.ver(archivo({ mime: 'application/pdf', nombre: 'resultado.pdf' }));

    expect(spy).toHaveBeenCalled();
  });

  it('abre la previsualización de una imagen y Escape la cierra', () => {
    const c = componente();
    (URL as unknown as { createObjectURL: () => string }).createObjectURL = () => 'blob:archivo';
    (URL as unknown as { revokeObjectURL: () => void }).revokeObjectURL = () => undefined;

    c.ver(archivo());

    const req = httpTesting.expectOne('/api/v1/historia-clinica/archivos/12/contenido');
    expect(req.request.method).toBe('GET');
    req.flush(new Blob(['imagen'], { type: 'image/png' }));

    expect(c.modalPreview).toBe(true);
    expect(c.previewNombre).toBe('panoramica.png');
    expect(c.previewUrl).toBe('blob:archivo');

    c.cerrarConEscape();
    expect(c.modalPreview).toBe(false);
    expect(c.previewUrl).toBe('');
  });

  it('sólo borra el archivo cuando se confirma', async () => {
    const c = componente();
    c.pacienteId = 7;
    const confirmacion = TestBed.inject(UiConfirmService);
    const abrir = vi.spyOn(confirmacion, 'abrir').mockResolvedValue(false);

    await c.eliminar(archivo());
    httpTesting.expectNone('/api/v1/historia-clinica/archivos/12');
    expect(abrir).toHaveBeenCalled();

    abrir.mockResolvedValue(true);
    const promesa = c.eliminar(archivo());
    await new Promise((r) => setTimeout(r, 0));

    const req = httpTesting.expectOne('/api/v1/historia-clinica/archivos/12');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
    await promesa;

    const lista = httpTesting.expectOne('/api/v1/historia-clinica/pacientes/7/archivos');
    lista.flush([archivo()]);
  });

  it('recarga el listado con la metadata del servidor', () => {
    const c = componente();
    c.pacienteId = 7;
    c.cargar();

    const req = httpTesting.expectOne('/api/v1/historia-clinica/pacientes/7/archivos');
    req.flush([archivo()]);

    expect(c.archivos.length).toBe(1);
    expect(c.archivos[0].categoria).toBe('RADIOGRAFIA');
    expect(c.archivos[0].sha256).toBe('a'.repeat(64));
  });
});
