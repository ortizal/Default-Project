import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { FacturacionDashboardComponent } from './dashboard';
import { AuthService } from '../core/auth.service';
import { DashboardFinanciero, ReporteVentas } from '../core/models';
import { etiquetaFormaPago, etiquetaMovimiento, moneda, toneEstadoCuenta, toneMovimiento } from './util';

function datos(): DashboardFinanciero {
  return {
    desde: '2025-10-06',
    hasta: '2026-10-06',
    facturado: 1000,
    cobrado: 800,
    descuentos: 50,
    devoluciones: 20,
    facturadoHoy: 100,
    cobradoHoy: 80,
    facturadoMes: 500,
    cobradoMes: 400,
    cuentasPorCobrar: 200,
    cuentasPendientes: 3,
    anticipos: 150,
    cobros: 10,
    ticketPromedio: 80,
    porFormaPago: [{ formaPago: 'EFECTIVO', monto: 800 }],
    porOdontologo: [{ odontologo: 'Dra. Ruiz', facturado: 1000, cargos: 5 }],
    porServicio: [{ servicio: 'Limpieza', facturado: 300, cargos: 3 }],
  };
}

function reporteVentas(desde: string, facturado: number): ReporteVentas {
  return {
    desde,
    hasta: desde,
    facturado,
    cobrado: facturado * 0.8,
    descuentos: 0,
    devoluciones: 0,
    cargos: 1,
    cobros: 1,
    porDia: [{ fecha: desde, facturado, cobrado: facturado * 0.8, movimientos: 1 }],
    porFormaPago: [],
    porServicio: [],
  };
}

describe('Finanzas — utilidades (Fase F, §3.3/§3.4)', () => {
  it('formatea moneda y rótulos de catálogo', () => {
    expect(moneda(40)).toBe('$ 40.00');
    expect(moneda(null)).toBe('—');
    expect(etiquetaFormaPago('EFECTIVO')).toBe('Efectivo');
    expect(etiquetaFormaPago(null)).toBe('—');
    expect(etiquetaMovimiento('ANTICIPO')).toBe('Anticipo');
    expect(etiquetaMovimiento('DEVOLUCION')).toBe('Devolución');
  });

  it('colorea estados de cuenta y movimientos', () => {
    expect(toneEstadoCuenta('PAGADA')).toBe('ok');
    expect(toneEstadoCuenta('ABIERTA')).toBe('warn');
    expect(toneEstadoCuenta('CERRADA')).toBe('muted');
    expect(toneEstadoCuenta('ANULADA')).toBe('danger');
    expect(toneMovimiento('CARGO')).toBe('info');
    expect(toneMovimiento('ABONO')).toBe('ok');
    expect(toneMovimiento('DESCUENTO')).toBe('warn');
  });
});

describe('Dashboard financiero — composición (Fase F, §3.12)', () => {
  let httpTesting: HttpTestingController;
  let permisos: Set<string>;

  beforeEach(async () => {
    permisos = new Set(['REPORTES_FINANCIEROS']);
    await TestBed.configureTestingModule({
      imports: [FacturacionDashboardComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { puede: (permiso?: string) => !permiso || permisos.has(permiso) } },
      ],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.match(() => true);
    httpTesting.verify();
  });

  function componente(): FacturacionDashboardComponent {
    const fixture = TestBed.createComponent(FacturacionDashboardComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function expectDashboard() {
    return httpTesting.expectOne((r) => r.url.startsWith('/api/v1/facturacion/dashboard?'));
  }

  function param(url: string, clave: string): string | null {
    return url.split('?')[1]?.split('&').find((p) => p.startsWith(`${clave}=`))?.slice(clave.length + 1) ?? null;
  }

  function responderVentas(c: FacturacionDashboardComponent, actual = 1000, anterior = 800): void {
    const peticiones = httpTesting.match((r) => r.url.startsWith('/api/v1/facturacion/reportes/ventas?'));
    expect(peticiones).toHaveLength(2);
    const peticionActual = peticiones.find((r) => param(r.request.url, 'desde') === c.desde);
    const peticionAnterior = peticiones.find((r) => r !== peticionActual);
    expect(peticionActual).toBeDefined();
    expect(peticionAnterior).toBeDefined();
    peticionActual!.flush(reporteVentas(c.desde, actual));
    peticionAnterior!.flush(reporteVentas(param(peticionAnterior!.request.url, 'desde')!, anterior));
  }

  it('arranca pidiendo el rango por defecto (1 año)', () => {
    const c = componente();

    expect(c.e).toBeNull();
    expect(c.error).toBe('');
    expect(c.cargando).toBe(true);

    const req = expectDashboard();
    expect(req.request.url).toContain(`desde=${c.desde}`);
    expect(req.request.url).toContain(`hasta=${c.hasta}`);
    req.flush(datos());
    responderVentas(c);

    expect(c.e?.facturado).toBe(1000);
    expect(c.e?.cuentasPorCobrar).toBe(200);
    expect(c.e?.porFormaPago[0].formaPago).toBe('EFECTIVO');
    expect(c.crecimiento).toBe(25);
    expect(c.proyeccion).toBeGreaterThan(0);
    expect(c.riesgoEstimado).toBeNull();
    c.porcentajeRiesgo = 25;
    expect(c.riesgoEstimado).toBe(50);
    expect(c.tendencia).toHaveLength(1);
    expect(c.cargando).toBe(false);
  });

  it('recarga al cambiar el rango', () => {
    const c = componente();
    expectDashboard().flush(datos());
    responderVentas(c);

    c.desde = '2026-01-01';
    c.hasta = '2026-03-31';
    c.cargar();

    const req = expectDashboard();
    expect(req.request.url).toContain('desde=2026-01-01');
    expect(req.request.url).toContain('hasta=2026-03-31');
    req.flush(datos());
    responderVentas(c, 500, 1000);
    expect(c.e?.ticketPromedio).toBe(80);
    expect(c.crecimiento).toBe(-50);
  });

  it('con FACTURACION_READ carga el resumen sin consultar reportes detallados', () => {
    permisos.clear();
    const c = componente();

    expectDashboard().flush(datos());

    expect(c.e?.facturado).toBe(1000);
    expect(c.puedeVerReportes).toBe(false);
    expect(c.ventas).toBeNull();
    expect(c.ventasAnterior).toBeNull();
    httpTesting.expectNone((r) => r.url.startsWith('/api/v1/facturacion/reportes/ventas?'));
  });

  it('sin permiso muestra el mensaje de 403', () => {
    const c = componente();
    expectDashboard().flush(
      { message: 'Denegado' },
      { status: 403, statusText: 'Forbidden' },
    );

    expect(c.error).toBe('No tienes permiso para ver el dashboard financiero.');
    expect(c.e).toBeNull();
  });

  it('rango inválido devuelve el mensaje del backend', () => {
    const c = componente();
    expectDashboard().flush(
      { message: 'La fecha de inicio no puede ser posterior a la fecha final' },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(c.error).toBe('La fecha de inicio no puede ser posterior a la fecha final');
  });
});
