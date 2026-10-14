import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ReportesFinancierosComponent } from './reportes-financieros';
import { ReporteCaja, ReporteProduccion, ReporteSaldos, ReporteVentas } from '../core/models';

function ventas(): ReporteVentas {
  return {
    desde: '2026-07-08',
    hasta: '2026-10-06',
    facturado: 1000,
    cobrado: 800,
    descuentos: 50,
    devoluciones: 20,
    cargos: 4,
    cobros: 3,
    porDia: [{ fecha: '2026-10-06', facturado: 200, cobrado: 160, movimientos: 2 }],
    porFormaPago: [{ formaPago: 'EFECTIVO', monto: 500 }],
    porServicio: [{ servicio: 'Limpieza', facturado: 300, cargos: 2 }],
  };
}

function produccion(): ReporteProduccion {
  return {
    desde: '2026-07-08',
    hasta: '2026-10-06',
    total: 1000,
    cargos: 4,
    porOdontologo: [{ odontologo: 'Dra. Ruiz', facturado: 1000, cargos: 4, descuentos: 50 }],
  };
}

function saldos(): ReporteSaldos {
  return {
    totalPorCobrar: 200,
    cantidadCuentas: 1,
    cuentas: [
      {
        id: 1,
        pacienteId: 1,
        paciente: 'Ana Pérez',
        cedula: '1712345678',
        estado: 'PARCIAL',
        saldo: 200,
        totalCargos: 300,
        totalAbonos: 100,
        cantidadMovimientos: 3,
        updatedAt: '2026-10-06T10:00:00Z',
      },
    ],
  };
}

function reporteCaja(): ReporteCaja {
  return {
    desde: '2026-07-08',
    hasta: '2026-10-06',
    cantidadSesiones: 1,
    aperturas: 20,
    ingresos: 144,
    egresos: 5,
    diferenciaTotal: -5,
    sesiones: [
      {
        id: 7,
        nombre: 'Caja',
        fecha: '2026-10-06',
        estado: 'CERRADA',
        apertura: 20,
        declarado: 154,
        diferencia: -5,
        movimientos: 2,
      },
    ],
  };
}

describe('Reportes financieros (Fase F, §3.13)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportesFinancierosComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function componente(): ReportesFinancierosComponent {
    const fixture = TestBed.createComponent(ReportesFinancierosComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function expectReporte(ruta: string) {
    return httpTesting.expectOne((r) => r.url.startsWith(`/api/v1/facturacion/reportes/${ruta}`));
  }

  it('arranca en la pestaña de ventas con el rango por defecto', () => {
    const c = componente();

    expect(c.pestana).toBe('ventas');
    expect(c.ventas).toBeNull();
    expect(c.cargando).toBe(true);

    const req = expectReporte('ventas');
    expect(req.request.url).toContain(`desde=${c.desde}`);
    expect(req.request.url).toContain(`hasta=${c.hasta}`);
    req.flush(ventas());

    expect(c.ventas?.facturado).toBe(1000);
    expect(c.ventas?.porDia[0].fecha).toBe('2026-10-06');
    expect(c.cargando).toBe(false);
  });

  it('cambia a producción y pide su reporte con el rango', () => {
    const c = componente();
    expectReporte('ventas').flush(ventas());

    c.seleccionar('produccion');

    const req = expectReporte('produccion');
    expect(req.request.url).toContain(`desde=${c.desde}`);
    req.flush(produccion());
    expect(c.produccion?.porOdontologo[0].odontologo).toBe('Dra. Ruiz');
  });

  it('la pestaña de saldos no envía rango', () => {
    const c = componente();
    expectReporte('ventas').flush(ventas());

    c.seleccionar('saldos');

    const req = expectReporte('saldos');
    expect(req.request.url).toBe('/api/v1/facturacion/reportes/saldos');
    req.flush(saldos());
    expect(c.saldos?.totalPorCobrar).toBe(200);
    expect(c.saldos?.cuentas[0].estado).toBe('PARCIAL');
  });

  it('carga el reporte de caja con sesiones y diferencia', () => {
    const c = componente();
    expectReporte('ventas').flush(ventas());

    c.seleccionar('caja');
    expectReporte('caja').flush(reporteCaja());

    expect(c.caja?.cantidadSesiones).toBe(1);
    expect(c.caja?.sesiones[0].diferencia).toBe(-5);
  });

  it('sin permiso muestra el mensaje de 403', () => {
    const c = componente();
    expectReporte('ventas').flush({ message: 'Denegado' }, { status: 403, statusText: 'Forbidden' });

    expect(c.error).toBe('No tienes permiso para ver los reportes financieros.');
    expect(c.ventas).toBeNull();
  });
});
