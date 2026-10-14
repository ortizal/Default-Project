import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { CentroInteligenciaComponent } from './centro-inteligencia';

describe('Centro de inteligencia', () => {
  let permisos: Set<string>;

  beforeEach(async () => {
    permisos = new Set(['REPORTES_CLINICOS']);
    await TestBed.configureTestingModule({
      imports: [CentroInteligenciaComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { puede: (permiso?: string) => !permiso || permisos.has(permiso) } },
      ],
    }).compileComponents();
  });

  it('solo muestra paneles autorizados al perfil clínico', () => {
    const fixture = TestBed.createComponent(CentroInteligenciaComponent);
    const componente = fixture.componentInstance;
    fixture.detectChanges();

    expect(componente.disponibles.map((panel) => panel.nombre)).toEqual(['Actividad clínica']);
    expect(componente.puedeConsultarReportes).toBe(true);
    expect(fixture.nativeElement.textContent).not.toContain('Finanzas');
  });
});