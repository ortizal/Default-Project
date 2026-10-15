import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { FacturacionComprobantesComponent } from './comprobantes';
import { AuthService } from '../core/auth.service';
import { ComprobanteElectronico, ComprobanteResumen, Page } from '../core/models';

function fila(): ComprobanteResumen {
  return {
    id: 1,
    tipo: '01',
    numeroComprobante: '001-001-000000001',
    claveAcceso: '0810202601040150117600110010010000000013522407211',
    pacienteId: 1,
    paciente: 'Lucía Martínez',
    identificacion: '1700000001',
    total: 716,
    fechaEmision: '2026-10-08',
    estadoGeneracion: 'ENVIADO',
    estadoTributario: 'AUTORIZADO',
    simulado: true,
    errorMensaje: null,
    cuentaId: 1,
    motivo: null,
    comprobanteReferenciaId: null,
    createdAt: '2026-10-08T16:08:20Z',
  };
}

function detalle(): ComprobanteElectronico {
  return {
    id: 1,
    tipo: '01',
    numeroComprobante: '001-001-000000001',
    claveAcceso: '0810202601040150117600110010010000000013522407211',
    ambiente: 'PRUEBAS',
    modoFirma: 'CERTIFICADO',
    simulado: false,
    estadoGeneracion: 'ENVIADO',
    estadoTributario: 'AUTORIZADO',
    sriEstado: 'AUTORIZADO',
    sriRespuesta: 'Autorizado por SRI (fixture de prueba).',
    errorMensaje: null,
    autorizacionNumero: '0810202601040150117600110010010000000013522407211',
    autorizacionFecha: '2026-10-08T16:08:25Z',
    total: 716,
    fechaEmision: '2026-10-08',
    cuentaId: 1,
    pacienteId: 1,
    comprobanteReferenciaId: null,
    motivo: null,
    detalles: [],
    createdAt: '2026-10-08T16:08:20Z',
  };
}

/** El mismo detalle, con las dos líneas del XML (Fase H.1). */
function detalleConLineas(): ComprobanteElectronico {
  return {
    ...detalle(),
    detalles: [
      {
        linea: 1,
        codigo: 'CONS-001',
        descripcion: 'Consulta odontológica',
        cantidad: 1,
        precioUnitario: 500,
        descuento: 0,
        total: 500,
        impuesto: 75,
      },
      {
        linea: 2,
        codigo: 'LIM-02',
        descripcion: 'Profilaxis',
        cantidad: 2,
        precioUnitario: 61.305,
        descuento: 0,
        total: 122.61,
        impuesto: 18.39,
      },
    ],
  };
}

function listado(): Page<ComprobanteResumen> {
  return {
    content: [fila()],
    page: { size: 20, number: 0, totalElements: 1, totalPages: 1 },
  };
}

describe('Bandeja de comprobantes (Fase G, §3.7)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FacturacionComprobantesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function componente(): FacturacionComprobantesComponent {
    const fixture = TestBed.createComponent(FacturacionComprobantesComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function expectListado() {
    return httpTesting.expectOne((r) => r.url.startsWith('/api/v1/facturacion/comprobantes?'));
  }

  it('lista los comprobantes con su total de páginas', () => {
    const c = componente();

    expect(c.cargando).toBe(true);
    expectListado().flush(listado());

    expect(c.rows.length).toBe(1);
    expect(c.rows[0].numeroComprobante).toBe('001-001-000000001');
    expect(c.rows[0].simulado).toBe(true);
    expect(c.totalElementos).toBe(1);
    expect(c.totalPaginas).toBe(1);
    expect(c.cargando).toBe(false);
  });

  it('filtra por estado y tipo', () => {
    const c = componente();
    expectListado().flush(listado());

    c.estado = 'AUTORIZADO';
    c.tipo = '01';
    c.buscar();

    const req = expectListado();
    expect(req.request.url).toContain('estado=AUTORIZADO');
    expect(req.request.url).toContain('tipo=01');
    req.flush(listado());
    expect(c.rows.length).toBe(1);
  });

  it('limpia los filtros y vuelve a la primera página', () => {
    const c = componente();
    expectListado().flush(listado());

    c.q = '001-001';
    c.pagina = 2;
    c.limpiar();

    const req = expectListado();
    expect(req.request.url).not.toContain('q=');
    expect(req.request.url).toContain('page=0');
    req.flush(listado());
    expect(c.q).toBe('');
    expect(c.pagina).toBe(0);
  });

  it('abre el detalle con la respuesta del SRI', () => {
    const c = componente();
    expectListado().flush(listado());

    c.abrir(1);

    httpTesting.expectOne('/api/v1/facturacion/comprobantes/1').flush(detalle());

    expect(c.sel?.numeroComprobante).toBe('001-001-000000001');
    expect(c.sel?.ambiente).toBe('PRUEBAS');
    expect(c.sel?.estadoTributario).toBe('AUTORIZADO');
  });

  it('reenvía al SRI y recarga el listado', () => {
    const c = componente();
    expectListado().flush(listado());

    c.reenviar(c.rows[0]);

    const post = httpTesting.expectOne('/api/v1/facturacion/comprobantes/1/reenviar');
    expect(post.request.method).toBe('POST');
    post.flush({ ...detalle(), estadoTributario: 'PENDIENTE' });

    expect(c.mensaje).toBe('Comprobante reenviado al SRI.');
    expectListado().flush(listado());
  });

  it('muestra el rechazo del SRI (409) sin borrar la fila', () => {
    const c = componente();
    expectListado().flush(listado());

    c.reenviar(c.rows[0]);

    httpTesting
      .expectOne('/api/v1/facturacion/comprobantes/1/reenviar')
      .flush(
        { message: 'El comprobante ya está autorizado por el SRI: no necesita reenvío' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(c.error).toBe(
      'El comprobante ya está autorizado por el SRI: no necesita reenvío',
    );
    expect(c.rows.length).toBe(1);
    expect(c.mensaje).toBe('');
  });

  it('descarga el XML del comprobante abierto', () => {
    const c = componente();
    expectListado().flush(listado());
    c.sel = detalle();

    c.descargarXml(c.sel);

    const req = httpTesting.expectOne('/api/v1/facturacion/comprobantes/1/xml');
    expect(req.request.method).toBe('GET');
    req.flush(new Blob(['<factura/>'], { type: 'application/xml' }));
    expect(c.error).toBe('');
  });

  it('descarga el RIDE en PDF', () => {
    const c = componente();
    expectListado().flush(listado());
    c.sel = detalle();

    c.descargarRide(c.sel);

    const req = httpTesting.expectOne('/api/v1/facturacion/comprobantes/1/ride');
    expect(req.request.method).toBe('GET');
    req.flush(new Blob(['%PDF-1.4'], { type: 'application/pdf' }));
    expect(c.error).toBe('');
  });

  it('no solicita RIDE sin autorizar, pero un simulado autorizado sí lo descarga', () => {
    const c = componente();
    expectListado().flush(listado());

    const pendiente = { ...detalle(), estadoTributario: 'PENDIENTE' as const };
    c.descargarRide(pendiente);
    expect(c.error).toBe('El RIDE sólo está disponible una vez autorizado el comprobante.');
    httpTesting.expectNone('/api/v1/facturacion/comprobantes/1/ride');

    // El "SRI local" de pruebas autoriza: un comprobante simulado con
    // estado AUTORIZADO también tiene RIDE.
    const simulado = { ...detalle(), simulado: true, estadoTributario: 'AUTORIZADO' as const };
    c.descargarRide(simulado);
    const req = httpTesting.expectOne('/api/v1/facturacion/comprobantes/1/ride');
    req.flush(new Blob(['%PDF-1.4'], { type: 'application/pdf' }));
    expect(c.error).toBe('');
  });

  it('pagina hacia atrás y adelante', () => {
    const c = componente();
    expectListado().flush({
      content: [fila()],
      page: { size: 20, number: 0, totalElements: 40, totalPages: 2 },
    });
    expect(c.totalPaginas).toBe(2);

    c.ir(1);

    const req = expectListado();
    expect(req.request.url).toContain('page=1');
    req.flush({
      content: [fila()],
      page: { size: 20, number: 1, totalElements: 40, totalPages: 2 },
    });
    expect(c.pagina).toBe(1);
  });

  it('sin permiso muestra el mensaje de 403', () => {
    const c = componente();
    expectListado().flush({ message: 'Denegado' }, { status: 403, statusText: 'Forbidden' });

    expect(c.error).toBe('No tienes permiso para ver los comprobantes.');
    expect(c.rows.length).toBe(0);
  });
});

describe('Nota de crédito y anulación (Fase H.2, §3.9/§3.10)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FacturacionComprobantesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function componente(): FacturacionComprobantesComponent {
    const fixture = TestBed.createComponent(FacturacionComprobantesComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function expectListado() {
    return httpTesting.expectOne((r) => r.url.startsWith('/api/v1/facturacion/comprobantes?'));
  }

  function conListado(): FacturacionComprobantesComponent {
    const c = componente();
    expectListado().flush(listado());
    return c;
  }

  function abrirNota(c: FacturacionComprobantesComponent) {
    c.abrirNotaCredito(c.rows[0]);
    httpTesting.expectOne('/api/v1/facturacion/comprobantes/1').flush(detalleConLineas());
  }

  it('sólo considera anulables las facturas y notas autorizadas', () => {
    const c = conListado();

    expect(c.esFacturaAutorizada(c.rows[0])).toBe(true);
    expect(c.esAnulable(c.rows[0])).toBe(true);
    expect(c.esFacturaAutorizada({ ...c.rows[0], tipo: '04' })).toBe(false);
    expect(c.esAnulable({ ...c.rows[0], tipo: '04' })).toBe(true);
    expect(c.esAnulable({ ...c.rows[0], estadoTributario: 'ANULADO' })).toBe(false);
    expect(c.esAnulable({ ...c.rows[0], estadoTributario: 'PENDIENTE' })).toBe(false);
  });

  it('habilita Anular para los comprobantes simulados pero no para los reales', () => {
    const c = conListado();

    // El "SRI local" de pruebas anula de inmediato (fila de listado simulada).
    expect(c.puedeAnularComprobante(c.rows[0])).toBe(true);
    // La anulación real del SRI aún no está integrada.
    expect(
      c.puedeAnularComprobante({ ...c.rows[0], simulado: false, estadoTributario: 'AUTORIZADO' }),
    ).toBe(false);
    expect(c.puedeAnularComprobante({ ...c.rows[0], estadoTributario: 'PENDIENTE' })).toBe(false);
  });

  it('pide FACTURACION_ANULAR para ofrecer las acciones', () => {
    const auth = TestBed.inject(AuthService);
    vi.spyOn(auth, 'puede').mockImplementation((codigo?: string) => codigo !== 'FACTURACION_ANULAR');
    const c = conListado();

    expect(c.puedeEmitir).toBe(true);
    expect(c.puedeAnular).toBe(false);
  });

  it('abre la nota de crédito con las líneas del XML', () => {
    const c = conListado();

    abrirNota(c);

    expect(c.nota).not.toBeNull();
    expect(c.nota?.comprobante.id).toBe(1);
    expect(c.nota?.parcial).toBe(false);
    expect(c.nota?.lineas.length).toBe(2);
    expect(c.nota?.lineas[1].facturada).toBe(2);
    expect(c.nota?.lineas[1].importe).toBe(141);
    expect(c.devuelto(c.nota!)).toBe(716);
  });

  it('emite la nota de crédito total y abre la nota nueva', () => {
    const c = conListado();
    abrirNota(c);

    c.nota!.motivo = 'Devolución total por error de diagnóstico';
    c.emitirNotaCredito();

    const post = httpTesting.expectOne('/api/v1/facturacion/notas-credito');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({
      comprobanteId: 1,
      motivo: 'Devolución total por error de diagnóstico',
      lineas: null,
    });
    post.flush({ ...detalleConLineas(), id: 9, tipo: '04', numeroComprobante: '001-001-000000001' });

    expect(c.nota).toBeNull();
    expectListado().flush(listado());
    httpTesting
      .expectOne('/api/v1/facturacion/comprobantes/9')
      .flush({ ...detalleConLineas(), id: 9, tipo: '04', numeroComprobante: '001-001-000000001' });
    expect(c.mensaje).toBe('Nota de crédito 001-001-000000001 emitida.');
  });

  it('emite la devolución parcial sólo con las líneas marcadas', () => {
    const c = conListado();
    abrirNota(c);

    c.nota!.parcial = true;
    c.nota!.motivo = 'Se devuelve una profilaxis';
    c.nota!.lineas[1].cantidad = 2;
    expect(c.lineasMarcadas(c.nota!)).toBe(1);
    expect(c.devuelto(c.nota!)).toBe(141);

    c.emitirNotaCredito();

    const post = httpTesting.expectOne('/api/v1/facturacion/notas-credito');
    expect(post.request.body.lineas).toEqual([{ linea: 2, cantidad: 2 }]);
    post.flush({ ...detalleConLineas(), id: 9, tipo: '04' });
    expect(c.nota).toBeNull();
    expectListado().flush(listado());
    httpTesting.expectOne('/api/v1/facturacion/comprobantes/9').flush({ ...detalleConLineas(), id: 9, tipo: '04' });
  });

  it('valida el motivo y las cantidades antes de llamar al SRI', () => {
    const c = conListado();
    abrirNota(c);

    c.nota!.motivo = 'abc';
    c.emitirNotaCredito();
    expect(c.nota?.error).toBe('El motivo debe tener entre 5 y 300 caracteres.');

    c.nota!.motivo = 'Se devuelve algo';
    c.nota!.parcial = true;
    c.emitirNotaCredito();
    expect(c.nota?.error).toBe('Indica cuántas unidades devolver en al menos una línea.');

    c.nota!.lineas[0].cantidad = 5;
    c.emitirNotaCredito();
    expect(c.nota?.error).toBe('La línea 1 sólo tiene 1 unidades.');

    httpTesting.expectNone((r) => r.url.includes('notas-credito'));
  });

  it('muestra el rechazo del SRI en el diálogo sin cerrarlo', () => {
    const c = conListado();
    abrirNota(c);

    c.nota!.motivo = 'Devolución total';
    c.emitirNotaCredito();

    httpTesting
      .expectOne('/api/v1/facturacion/notas-credito')
      .flush({ message: 'La factura pedida ya no admite más notas.' }, { status: 409, statusText: 'Conflict' });

    expect(c.nota?.error).toBe('La factura pedida ya no admite más notas.');
    expect(c.nota?.enviando).toBe(false);
    expect(c.nota).not.toBeNull();
  });

  it('anula el comprobante con su motivo y lo refresca', () => {
    const c = conListado();
    c.sel = detalle();

    c.abrirAnulacion(c.rows[0]);
    expect(c.anulacion?.comprobante.id).toBe(1);

    c.anulacion!.motivo = 'Factura emitida por error';
    c.confirmarAnulacion();

    const post = httpTesting.expectOne('/api/v1/facturacion/comprobantes/1/anular');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual({ motivo: 'Factura emitida por error' });
    post.flush({ ...detalle(), estadoTributario: 'ANULADO', motivo: 'Factura emitida por error' });

    expect(c.anulacion).toBeNull();
    expectListado().flush(listado());
    httpTesting
      .expectOne('/api/v1/facturacion/comprobantes/1')
      .flush({ ...detalle(), estadoTributario: 'ANULADO', motivo: 'Factura emitida por error' });
    expect(c.mensaje).toBe('Comprobante 001-001-000000001 anulado.');
    expect(c.sel?.estadoTributario).toBe('ANULADO');
  });

  it('la anulación exige motivo y conserva el diálogo si el backend rechaza', () => {
    const c = conListado();

    c.abrirAnulacion(c.rows[0]);
    c.anulacion!.motivo = 'abc';
    c.confirmarAnulacion();
    expect(c.anulacion?.error).toBe('El motivo debe tener entre 5 y 500 caracteres.');
    httpTesting.expectNone((r) => r.url.includes('/anular'));

    c.anulacion!.motivo = 'Motivo repetido';
    c.confirmarAnulacion();
    httpTesting
      .expectOne('/api/v1/facturacion/comprobantes/1/anular')
      .flush({ message: 'El comprobante ya fue anulado.' }, { status: 409, statusText: 'Conflict' });

    expect(c.anulacion?.error).toBe('El comprobante ya fue anulado.');
    expect(c.anulacion?.enviando).toBe(false);
    expect(c.mensaje).toBe('');
  });

  it('cierra los diálogos sin enviar nada', () => {
    const c = conListado();
    abrirNota(c);
    expect(c.nota).not.toBeNull();

    c.cerrarDialogo();

    expect(c.nota).toBeNull();
    expect(c.anulacion).toBeNull();
    httpTesting.expectNone((r) => r.url.includes('notas-credito'));
  });
});
