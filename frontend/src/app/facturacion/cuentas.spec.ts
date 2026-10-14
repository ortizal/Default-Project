import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { FacturacionCuentasComponent } from './cuentas';
import { ComprobanteElectronico, CuentaPaciente, CuentaResumen } from '../core/models';

function resumen(): CuentaResumen[] {
  return [
    {
      id: 1,
      pacienteId: 1,
      paciente: 'Ana Pérez',
      cedula: '1712345678',
      estado: 'ABIERTA',
      saldo: 120,
      totalCargos: 200,
      totalAbonos: 80,
      cantidadMovimientos: 2,
      updatedAt: '2026-10-06T10:00:00Z',
    },
    {
      id: 2,
      pacienteId: 2,
      paciente: 'Luis Gómez',
      cedula: null,
      estado: 'CERRADA',
      saldo: 0,
      totalCargos: 50,
      totalAbonos: 50,
      cantidadMovimientos: 2,
      updatedAt: '2026-10-05T10:00:00Z',
    },
  ];
}

function detalle(): CuentaPaciente {
  return {
    id: 1,
    pacienteId: 1,
    paciente: 'Ana Pérez',
    cedula: '1712345678',
    estado: 'ABIERTA',
    saldo: 120,
    totalCargos: 200,
    totalDescuentos: 0,
    totalAbonos: 80,
    cantidadMovimientos: 2,
    movimientos: [
      {
        id: 11,
        cuentaId: 1,
        tipo: 'CARGO',
        origen: 'PROCEDIMIENTO',
        referenciaId: 5,
        concepto: 'Obturación',
        diente: 21,
        superficie: 'OCLUSAL',
        cantidad: 1,
        precioUnitario: 200,
        descuentoPct: 0,
        total: 200,
        formaPago: null,
        fecha: '2026-10-06',
        anulado: false,
        anuladoMotivo: null,
        observaciones: null,
        creadoPor: 1,
        createdAt: '2026-10-06T10:00:00Z',
      },
      {
        id: 12,
        cuentaId: 1,
        tipo: 'ABONO',
        origen: 'CAJA',
        referenciaId: null,
        concepto: 'Abono en efectivo',
        diente: null,
        superficie: null,
        cantidad: 1,
        precioUnitario: 0,
        descuentoPct: 0,
        total: 80,
        formaPago: 'EFECTIVO',
        fecha: '2026-10-06',
        anulado: false,
        anuladoMotivo: null,
        observaciones: null,
        creadoPor: 1,
        createdAt: '2026-10-06T11:00:00Z',
      },
    ],
    createdAt: '2026-10-06T09:00:00Z',
    updatedAt: '2026-10-06T11:00:00Z',
  };
}

/** Comprobante devuelto por el endpoint de emisión. */
function comprobante(): ComprobanteElectronico {
  return {
    id: 1,
    tipo: '01',
    numeroComprobante: '001-001-000000001',
    claveAcceso: '0810202601040150117600110010010000000013522407211',
    ambiente: 'PRUEBAS',
    modoFirma: 'SIMULADA',
    simulado: true,
    estadoGeneracion: 'FIRMADO',
    estadoTributario: 'PENDIENTE',
    sriEstado: 'SIMULADA',
    sriRespuesta: 'Prueba interna: no se consultó al SRI; la autorización tributaria sigue pendiente.',
    errorMensaje: null,
    autorizacionNumero: null,
    autorizacionFecha: null,
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

describe('Cuentas de pacientes (Fase F, §3.3)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FacturacionCuentasComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function componente(): FacturacionCuentasComponent {
    const fixture = TestBed.createComponent(FacturacionCuentasComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function expectListado() {
    return httpTesting.expectOne((r) => r.url.startsWith('/api/v1/facturacion/cuentas?'));
  }

  it('lista las cuentas con su saldo', () => {
    const c = componente();

    expect(c.cargando).toBe(true);
    expect(c.cuentas.length).toBe(0);

    const req = expectListado();
    expect(req.request.url).not.toContain('estado=');
    req.flush(resumen());

    expect(c.cuentas.length).toBe(2);
    expect(c.cuentas[0].paciente).toBe('Ana Pérez');
    expect(c.cargando).toBe(false);
  });

  it('filtra por estado', () => {
    const c = componente();
    expectListado().flush(resumen());

    c.estado = 'CERRADA';
    c.cargar();

    const req = expectListado();
    expect(req.request.url).toContain('estado=CERRADA');
    req.flush([resumen()[1]]);
    expect(c.cuentas.length).toBe(1);
  });

  it('abre el detalle con el libro de movimientos', () => {
    const c = componente();
    expectListado().flush(resumen());

    c.abrir(1);

    const req = httpTesting.expectOne('/api/v1/facturacion/cuentas/1');
    req.flush(detalle());

    expect(c.sel?.paciente).toBe('Ana Pérez');
    expect(c.sel?.movimientos.length).toBe(2);
    expect(c.sel?.movimientos[0].origen).toBe('PROCEDIMIENTO');
    expect(c.bloqueada).toBe(false);
  });

  it('agrega un abono y limpia el formulario', () => {
    const c = componente();
    expectListado().flush(resumen());
    c.sel = detalle();

    c.form = { tipo: 'ABONO', concepto: 'Abono', total: 50, formaPago: 'EFECTIVO' };
    c.agregar();

    const req = httpTesting.expectOne('/api/v1/facturacion/cuentas/1/movimientos');
    expect(req.request.body).toEqual({
      tipo: 'ABONO',
      concepto: 'Abono',
      total: 50,
      formaPago: 'EFECTIVO',
    });
    req.flush(detalle());

    expect(c.mensaje).toBe('Movimiento registrado.');
    expect(c.form.tipo).toBe('CARGO');
    expectListado().flush(resumen());
    httpTesting.expectOne('/api/v1/facturacion/cuentas/1').flush(detalle());
  });

  it('una cuenta cerrada bloquea los movimientos', () => {
    const c = componente();
    expectListado().flush(resumen());
    c.sel = { ...detalle(), estado: 'CERRADA' };

    expect(c.bloqueada).toBe(true);
  });

  it('la caja rechazada por el backend se muestra en el mensaje de error', () => {
    const c = componente();
    expectListado().flush(resumen());
    c.sel = detalle();

    c.form = { tipo: 'ABONO', concepto: 'Abono', total: 50, formaPago: 'EFECTIVO' };
    c.agregar();

    httpTesting
      .expectOne('/api/v1/facturacion/cuentas/1/movimientos')
      .flush(
        { message: 'El monto no puede ser mayor al saldo de la cuenta' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(c.error).toBe('El monto no puede ser mayor al saldo de la cuenta');
    expect(c.mensaje).toBe('');
  });

  it('emite la factura electrónica de la cuenta', () => {
    const c = componente();
    expectListado().flush(resumen());
    c.sel = detalle();

    c.emitir();

    const req = httpTesting.expectOne('/api/v1/facturacion/comprobantes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ cuentaId: 1 });
    req.flush(comprobante());

    expect(c.comprobante?.numeroComprobante).toBe('001-001-000000001');
    expect(c.mensaje).toBe(
      'Comprobante de prueba 001-001-000000001 generado localmente. No fue enviado ni autorizado por el SRI.',
    );

    expectListado().flush(resumen());
    httpTesting.expectOne('/api/v1/facturacion/cuentas/1').flush(detalle());
  });

  it('una cuenta ya facturada muestra el rechazo del backend', () => {
    const c = componente();
    expectListado().flush(resumen());
    c.sel = detalle();

    c.emitir();

    httpTesting
      .expectOne('/api/v1/facturacion/comprobantes')
      .flush(
        { message: 'La cuenta ya tiene un comprobante emitido' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(c.error).toBe('La cuenta ya tiene un comprobante emitido');
    expect(c.mensaje).toBe('');
    expect(c.comprobante).toBeNull();
  });

  it('sin permiso muestra el mensaje de 403', () => {
    const c = componente();
    expectListado().flush({ message: 'Denegado' }, { status: 403, statusText: 'Forbidden' });

    expect(c.error).toBe('No tienes permiso para operar las cuentas.');
    expect(c.cuentas.length).toBe(0);
  });
});
