import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { FacturacionCajaComponent } from './caja';
import { Caja } from '../core/models';
import { UiConfirmService } from '../ui';

function caja(): Caja {
  return {
    id: 7,
    nombre: 'Caja',
    estado: 'ABIERTA',
    fecha: '2026-10-06',
    montoApertura: 20,
    montoDeclarado: null,
    diferencia: null,
    responsable: 'Administrador',
    abiertoPor: 'admin',
    abiertoEn: '2026-10-06T09:00:00Z',
    cerradoPor: null,
    cerradoEn: null,
    observacionesApertura: null,
    observacionesCierre: null,
    totales: {
      apertura: 20,
      ingresos: 144,
      egresos: 5,
      declarado: null,
      esperadoEfectivo: 159,
      diferencia: null,
      porForma: [{ formaPago: 'EFECTIVO', monto: 139 }],
      movimientos: 2,
    },
    movimientos: [
      {
        id: 31,
        cajaId: 7,
        tipo: 'INGRESO',
        concepto: 'Cobro de cuenta',
        formaPago: 'EFECTIVO',
        monto: 144,
        cuentaDetalleId: 11,
        pacienteId: 1,
        paciente: 'Ana Pérez',
        fecha: '2026-10-06',
        anulado: false,
        anuladoMotivo: null,
        creadoPor: 1,
        createdAt: '2026-10-06T10:00:00Z',
      },
      {
        id: 32,
        cajaId: 7,
        tipo: 'EGRESO',
        concepto: 'Insumos',
        formaPago: 'EFECTIVO',
        monto: 5,
        cuentaDetalleId: null,
        pacienteId: null,
        paciente: null,
        fecha: '2026-10-06',
        anulado: false,
        anuladoMotivo: null,
        creadoPor: 1,
        createdAt: '2026-10-06T12:00:00Z',
      },
    ],
    createdAt: '2026-10-06T09:00:00Z',
    updatedAt: '2026-10-06T12:00:00Z',
  };
}

function cerrada(): Caja {
  return {
    ...caja(),
    estado: 'CERRADA',
    montoDeclarado: 154,
    diferencia: -5,
    cerradoPor: 'admin',
    cerradoEn: '2026-10-06T18:00:00Z',
    totales: { ...caja().totales, declarado: 154, diferencia: -5 },
  };
}

describe('Caja del consultorio (Fase F, §3.4)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FacturacionCajaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function componente(): FacturacionCajaComponent {
    const fixture = TestBed.createComponent(FacturacionCajaComponent);
    fixture.detectChanges();
    // ngOnInit pide el catálogo de pacientes para el selector de movimientos.
    httpTesting
      .expectOne((r) => r.url.startsWith('/api/v1/pacientes?'))
      .flush({ content: [] });
    return fixture.componentInstance;
  }

  function expectActual() {
    return httpTesting.expectOne('/api/v1/facturacion/cajas/actual');
  }

  function expectHistorial() {
    return httpTesting.expectOne(
      (r) =>
        r.url === '/api/v1/facturacion/cajas' ||
        r.url.startsWith('/api/v1/facturacion/cajas?'),
    );
  }

  it('sin sesión abierta deja la caja en null (404) y carga el historial', () => {
    const c = componente();

    expectActual().flush({ message: 'No hay caja abierta' }, { status: 404, statusText: 'Not Found' });
    expectHistorial().flush([]);

    expect(c.caja).toBeNull();
    expect(c.error).toBe('');
    expect(c.cargando).toBe(false);
  });

  it('carga la sesión abierta con su conciliación', () => {
    const c = componente();

    expectActual().flush(caja());
    expectHistorial().flush([caja()]);

    expect(c.abierta).toBe(true);
    expect(c.caja?.totales.esperadoEfectivo).toBe(159);
    expect(c.caja?.movimientos.length).toBe(2);
    expect(c.historial.length).toBe(1);
  });

  it('abre la sesión con el fondo inicial', () => {
    const c = componente();
    expectActual().flush({ message: 'No hay caja abierta' }, { status: 404, statusText: 'Not Found' });
    expectHistorial().flush([]);

    c.apertura = { nombre: 'Caja', montoApertura: 20, observaciones: '' };
    c.abrir();

    const req = httpTesting.expectOne('/api/v1/facturacion/cajas');
    expect(req.request.body.montoApertura).toBe(20);
    req.flush(caja());
    expectHistorial().flush([caja()]);

    expect(c.caja?.estado).toBe('ABIERTA');
    expect(c.mensaje).toBe('Caja abierta.');
  });

  it('registra un egreso manual', () => {
    const c = componente();
    expectActual().flush(caja());
    expectHistorial().flush([caja()]);

    c.mov = { tipo: 'EGRESO', concepto: 'Insumos', formaPago: 'EFECTIVO', monto: 30 };
    c.agregar();

    const req = httpTesting.expectOne('/api/v1/facturacion/cajas/7/movimientos');
    expect(req.request.body).toEqual({
      tipo: 'EGRESO',
      concepto: 'Insumos',
      formaPago: 'EFECTIVO',
      monto: 30,
    });
    req.flush(caja());

    expect(c.mensaje).toBe('Movimiento registrado.');
    expect(c.mov.concepto).toBe('');
  });

  it('cierra la sesión y muestra la diferencia', () => {
    const c = componente();
    expectActual().flush(caja());
    expectHistorial().flush([caja()]);

    expect(c.diferenciaEsperada).toBe(-159);

    c.cierre = { montoDeclarado: 154, observaciones: 'Cierre normal' };
    expect(c.diferenciaEsperada).toBe(-5);

    c.cerrar();

    const req = httpTesting.expectOne('/api/v1/facturacion/cajas/actual/cerrar');
    expect(req.request.body).toEqual({ montoDeclarado: 154, observaciones: 'Cierre normal' });
    req.flush(cerrada());
    expectHistorial().flush([cerrada()]);

    expect(c.caja?.estado).toBe('CERRADA');
    expect(c.mensaje).toBe('Caja cerrada con diferencia $ -5.00.');
  });

  it('anula un movimiento pidiendo el motivo en el diálogo', async () => {
    const c = componente();
    expectActual().flush(caja());
    expectHistorial().flush([caja()]);
    const confirm = TestBed.inject(UiConfirmService);

    const pendiente = c.anular(32, 'Insumos');
    expect(confirm.peticion()?.campo?.requerido).toBe(true);
    confirm.texto.set('Cobro duplicado');
    confirm.confirmado();
    await pendiente;

    const req = httpTesting.expectOne((r) =>
      r.url.startsWith('/api/v1/facturacion/cajas/7/movimientos/32/anular'),
    );
    const params = new URLSearchParams(req.request.url.split('?')[1]);
    expect(params.get('motivo')).toBe('Cobro duplicado');
    req.flush(caja());

    expect(c.mensaje).toBe('Movimiento anulado.');
  });

  it('cancelar la anulación en el diálogo no envía nada', async () => {
    const c = componente();
    expectActual().flush(caja());
    expectHistorial().flush([caja()]);
    const confirm = TestBed.inject(UiConfirmService);

    const pendiente = c.anular(31, 'Cobro de cuenta');
    confirm.cancelado();
    await pendiente;

    expect(c.mensaje).toBe('');
    httpTesting.expectNone((r) => r.url.includes('/anular'));
  });

  it('filtra el historial por rango de fechas', () => {
    const c = componente();
    expectActual().flush(caja());
    expectHistorial().flush([caja()]);

    c.historialDesde = '2026-10-01';
    c.historialHasta = '2026-10-31';
    c.buscarHistorial();

    const req = expectHistorial();
    expect(req.request.url).toContain('desde=2026-10-01');
    expect(req.request.url).toContain('hasta=2026-10-31');
    req.flush([caja()]);
    expect(c.historial.length).toBe(1);
  });

  it('despliega y pliega el detalle de una sesión del historial', () => {
    const c = componente();
    expectActual().flush(caja());
    expectHistorial().flush([caja()]);

    expect(c.detalleHistorial).toBeNull();
    c.alternarDetalle(7);
    expect(c.detalleHistorial).toBe(7);
    c.alternarDetalle(7);
    expect(c.detalleHistorial).toBeNull();
  });

  it('el selector de paciente deja el movimiento sin paciente al vaciarse', () => {
    const c = componente();
    expectActual().flush(caja());
    expectHistorial().flush([caja()]);

    c.setPaciente('12');
    expect(c.mov.pacienteId).toBe(12);
    c.setPaciente('');
    expect(c.mov.pacienteId).toBeUndefined();
  });

  it('sin permiso muestra el mensaje de 403', () => {
    const c = componente();
    expectActual().flush({ message: 'Denegado' }, { status: 403, statusText: 'Forbidden' });

    expect(c.error).toBe('No tienes permiso para operar la caja.');
    expect(c.caja).toBeNull();
  });
});
