import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { AuditoriaComponent } from './auditoria';
import { Auditoria, Page } from '../core/models';

function registros(): Page<Auditoria> {
  return {
    content: [
      {
        id: 1,
        usuarioId: 1,
        accion: 'GENERAR_COMPROBANTE_SRI',
        modulo: 'FACTURACION',
        entidad: 'COMPROBANTE_ELECTRONICO',
        entidadId: 7,
        datosAnteriores: null,
        datosNuevos: '{"claveAcceso":"0810202601040150117600110010010000000013522407211"}',
        ip: '127.0.0.1',
        createdAt: '2026-10-08T16:08:21Z',
      },
    ],
    page: { size: 20, number: 0, totalElements: 1, totalPages: 1 },
  };
}

describe('Auditoría (§3.7: enlace desde la bandeja)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuditoriaComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'auditoria', component: AuditoriaComponent }]),
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  async function navegar(entidadId: number): Promise<AuditoriaComponent> {
    await TestBed.inject(Router).navigate(['/auditoria'], {
      queryParams: { entidad: 'COMPROBANTE_ELECTRONICO', entidadId, modulo: 'FACTURACION' },
    });
    const fixture = TestBed.createComponent(AuditoriaComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function expectListado() {
    return httpTesting.expectOne((r) => r.url.startsWith('/api/v1/auditoria?'));
  }

  it('lee el filtro de entidad de la URL', async () => {
    const c = await navegar(7);

    expect(c.entidad).toBe('COMPROBANTE_ELECTRONICO');
    expect(c.entidadId).toBe(7);
    expect(c.modulo).toBe('FACTURACION');

    const req = expectListado();
    expect(req.request.url).toContain('entidad=COMPROBANTE_ELECTRONICO');
    expect(req.request.url).toContain('entidadId=7');
    expect(req.request.url).toContain('modulo=FACTURACION');
    req.flush(registros());
    expect(c.rows.length).toBe(1);
    expect(c.totalPag).toBe(1);
  });

  it('quita el filtro de una fila concreta', async () => {
    const c = await navegar(7);
    expectListado().flush(registros());

    c.quitarFiltroEntidad();

    const req = expectListado();
    expect(req.request.url).not.toContain('entidad=');
    expect(req.request.url).not.toContain('entidadId=');
    req.flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
    expect(c.entidadId).toBeNull();
  });

  it('busca por texto libre', async () => {
    const c = await navegar(7);
    expectListado().flush(registros());

    c.q = 'GENERAR';
    c.buscar();

    const req = expectListado();
    expect(req.request.url).toContain('q=GENERAR');
    req.flush(registros());
    expect(c.rows[0].accion).toBe('GENERAR_COMPROBANTE_SRI');
  });
});
