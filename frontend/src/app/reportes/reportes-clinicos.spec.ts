import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import {
  ReportesClinicosComponent,
  etiquetaPrioridad,
  moneda,
  pieza,
  tonePrioridad,
} from './reportes-clinicos';
import { ReporteClinicoData } from '../core/models';

function datos(): ReporteClinicoData {
  return {
    desde: '2025-10-06',
    hasta: '2026-10-06',
    totales: {
      evoluciones: 1,
      examenes: 1,
      diagnosticos: 1,
      odontogramas: 3,
      periodontos: 2,
      recetas: 13,
      consentimientos: 32,
      archivos: 1,
      planes: 3,
      citasAtendidas: 0,
      pacientesAtendidos: 4,
    },
    diagnosticosFrecuentes: [{ nombre: 'Caries', total: 4, pacientes: 3 }],
    tratamientosPendientes: [
      {
        pacienteId: 1,
        paciente: 'Ana Pérez',
        planId: 2,
        plan: 'Rehabilitación',
        lineaId: 5,
        procedimiento: 'Empaste',
        diente: 21,
        superficie: 'OCLUSAL',
        prioridad: 'URGENTE',
        estado: 'PENDIENTE',
        precio: 40,
      },
    ],
    continuidad: {
      citasAtendidas: 0,
      citasConfirmadas: 2,
      citasCanceladas: 1,
      citasNoAsistio: 0,
      pacientesActivos: 4,
      pacientesSinProximaCita: 7,
      sinActividad90d: [{ pacienteId: 3, paciente: 'Luis Gómez' }],
    },
    produccionClinica: [{ procedimiento: 'Extracción', cantidad: 2, total: 90 }],
  };
}

describe('Reportes clínicos — helpers (Fase E, §4.17)', () => {
  it('rotula y colorea las prioridades', () => {
    expect(etiquetaPrioridad('URGENTE')).toBe('Urgente');
    expect(etiquetaPrioridad(null)).toBe('—');
    expect(tonePrioridad('URGENTE')).toBe('danger');
    expect(tonePrioridad('ALTA')).toBe('warn');
    expect(tonePrioridad('MEDIA')).toBe('info');
    expect(tonePrioridad(null)).toBe('muted');
  });

  it('formatea moneda y pieza dental', () => {
    expect(moneda(40)).toBe('$ 40.00');
    expect(moneda(null)).toBe('—');
    expect(pieza(21, 'OCLUSAL')).toBe('21 · oclusal');
    expect(pieza(null, null)).toBe('—');
  });
});

describe('Reportes clínicos — composición (Fase E, §4.17)', () => {
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReportesClinicosComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  function componente(): ReportesClinicosComponent {
    const fixture = TestBed.createComponent(ReportesClinicosComponent);
    fixture.detectChanges();
    return fixture.componentInstance;
  }

  function expectReportes() {
    return httpTesting.expectOne(
      (r) => r.url.startsWith('/api/v1/historia-clinica/reportes?'),
    );
  }

  it('arranca pidiendo el rango por defecto (1 año, 10 filas)', () => {
    const c = componente();

    expect(c.e).toBeNull();
    expect(c.error).toBe('');
    expect(c.cargando).toBe(true);
    expect(c.limite).toBe(10);

    const req = expectReportes();
    expect(req.request.url).toContain(`desde=${c.desde}`);
    expect(req.request.url).toContain(`hasta=${c.hasta}`);
    expect(req.request.url).toContain('limite=10');
    req.flush(datos());
    expect(c.e?.totales.pacientesAtendidos).toBe(4);
    expect(c.cargando).toBe(false);
  });

  it('recarga al cambiar el límite y pinta los cuatro bloques', () => {
    const c = componente();
    expectReportes().flush(datos());

    c.limite = 50;
    c.cargar();

    const req = expectReportes();
    expect(req.request.url).toContain('limite=50');
    req.flush(datos());

    expect(c.e?.diagnosticosFrecuentes[0].nombre).toBe('Caries');
    expect(c.e?.tratamientosPendientes[0].prioridad).toBe('URGENTE');
    expect(c.e?.produccionClinica[0].procedimiento).toBe('Extracción');
    expect(c.e?.continuidad.sinActividad90d[0].paciente).toBe('Luis Gómez');
  });

  it('sin permiso muestra el mensaje de 403', () => {
    const c = componente();
    const req = expectReportes();
    req.flush({ message: 'Denegado' }, { status: 403, statusText: 'Forbidden' });

    expect(c.error).toBe('No tienes permiso para ver los reportes clínicos.');
    expect(c.e).toBeNull();
  });

  it('rango inválido devuelve el mensaje del backend', () => {
    const c = componente();
    const req = expectReportes();
    req.flush(
      { message: 'La fecha de inicio no puede ser posterior a la fecha final' },
      { status: 400, statusText: 'Bad Request' },
    );

    expect(c.error).toBe('La fecha de inicio no puede ser posterior a la fecha final');
  });
});
