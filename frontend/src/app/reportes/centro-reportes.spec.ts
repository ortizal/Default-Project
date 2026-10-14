import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { CentroReportesComponent } from './centro-reportes';

describe('Centro de reportes', () => {
  let permisos: Set<string>;

  beforeEach(async () => {
    permisos = new Set(['REPORTES', 'REPORTES_CLINICOS']);
    await TestBed.configureTestingModule({
      imports: [CentroReportesComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { puede: (permiso?: string) => !permiso || permisos.has(permiso) } },
      ],
    }).compileComponents();
  });

  it('filtra el catálogo por permisos y dominio', () => {
    const fixture = TestBed.createComponent(CentroReportesComponent);
    const componente = fixture.componentInstance;
    fixture.detectChanges();

    expect(componente.disponibles.map((reporte) => reporte.categoria)).toEqual(['Operación', 'Clínica']);

    componente.categoria = 'Clínica';
    expect(componente.disponibles.map((reporte) => reporte.nombre)).toEqual(['Reportes clínicos']);
  });

  it('busca por nombre y restablece filtros', () => {
    const componente = TestBed.createComponent(CentroReportesComponent).componentInstance;
    componente.busqueda = 'cancelaciones';
    expect(componente.disponibles).toHaveLength(1);
    expect(componente.disponibles[0].nombre).toBe('Estadísticas de operación');

    componente.limpiar();
    expect(componente.busqueda).toBe('');
    expect(componente.categoria).toBe('Todas');
  });
});