import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import {
  ETIQUETAS_TIMELINE,
  TONES_TIMELINE,
  HistoriaClinicaComponent,
  TABS_HISTORIA,
  TIPOS_ANTECEDENTE,
  agruparAntecedentes,
  editableEvolucion,
  etiquetaDiagnostico,
  etiquetaEventoTimeline,
  nombrePaciente,
  ordenarTimeline,
  tiposTimelinePresentes,
} from './historia-clinica';
import { Antecedente, TimelineEvento, TipoTimeline } from '../core/models';

function antecedente(tipo: Antecedente['tipo'], nombre: string): Antecedente {
  return { tipo, nombre, estado: 'ACTIVO' };
}

describe('Historia clínica — helpers (Fase B)', () => {
  it('expone las doce pestañas implementadas hasta la Fase E', () => {
    expect(TABS_HISTORIA.map((t) => t.id)).toEqual([
      'resumen',
      'antecedentes',
      'examen',
      'odontograma',
      'periodoncia',
      'recetas',
      'consentimientos',
      'archivos',
      'diagnosticos',
      'evoluciones',
      'timeline',
      'planes',
    ]);
  });

  it('los tipos de antecedente cubren el abanico del plan (§4.3)', () => {
    expect(TIPOS_ANTECEDENTE.map((t) => t.id)).toEqual([
      'ENFERMEDAD',
      'CIRUGIA',
      'HOSPITALIZACION',
      'ALERGIA',
      'MEDICACION',
      'ANTECEDENTE_ODONTOLOGICO',
      'HABITO',
    ]);
  });

  it('agrupa los antecedentes por tipo y descarta los grupos vacíos', () => {
    const grupos = agruparAntecedentes([
      antecedente('ALERGIA', 'Látex'),
      antecedente('ENFERMEDAD', 'Asma'),
      antecedente('ALERGIA', 'Penicilina'),
    ]);

    expect(grupos.length).toBe(2);
    expect(grupos[0].tipo).toBe('ENFERMEDAD');
    expect(grupos[0].items.map((a) => a.nombre)).toEqual(['Asma']);
    expect(grupos[1].tipo).toBe('ALERGIA');
    expect(grupos[1].items.map((a) => a.nombre)).toEqual(['Látex', 'Penicilina']);
  });

  it('devuelve vacío cuando no hay antecedentes', () => {
    expect(agruparAntecedentes([])).toEqual([]);
  });

  it('una evolución sólo es editable mientras esté abierta', () => {
    expect(editableEvolucion({ estado: 'ABIERTA' })).toBe(true);
    expect(editableEvolucion({ estado: 'CERRADA' })).toBe(false);
  });

  it('ordena la línea temporal de más reciente a más antigua', () => {
    const eventos: TimelineEvento[] = [
      { tipo: 'DIAGNOSTICO', fecha: '2026-01-01T00:00:00Z', titulo: 'Gingivitis', referenciaId: 1 },
      { tipo: 'EVOLUCION', fecha: '2026-03-01T10:00:00Z', titulo: 'Control', referenciaId: 2 },
      { tipo: 'EXAMEN', fecha: '2026-02-01T00:00:00Z', titulo: 'Examen', referenciaId: 3 },
    ];

    expect(ordenarTimeline(eventos).map((e) => e.tipo)).toEqual(['EVOLUCION', 'EXAMEN', 'DIAGNOSTICO']);
  });

  it('arma el nombre del paciente y la etiqueta del diagnóstico', () => {
    expect(nombrePaciente({ nombres: 'Lucía', apellidos: 'Martínez' })).toBe('Lucía Martínez');
    expect(nombrePaciente(null)).toBe('');
    expect(etiquetaDiagnostico({ severidad: 'SEVERA', estado: 'CONFIRMADO' })).toBe('Severa · Confirmado');
  });
});

describe('HistoriaClinicaComponent — composición', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistoriaClinicaComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } },
        },
      ],
    }).compileComponents();
  });

  it('se construye con la ruta activa y arranca en la pestaña Resumen', () => {
    const fixture = TestBed.createComponent(HistoriaClinicaComponent);
    const componente = fixture.componentInstance;
    expect(componente.tab).toBe('resumen');
    expect(componente.tabs.length).toBe(12);
    // Los permisos se resuelven en ngOnInit (que dispara la carga remota).
    expect(componente.puedeEscribir).toBe(false);
  });
});

describe('Línea temporal unificada (§4.16)', () => {
  const tipos: TipoTimeline[] = [
    'CITA',
    'EXAMEN',
    'DIAGNOSTICO',
    'EVOLUCION',
    'ODONTOGRAMA',
    'PERIODONCIA',
    'PLAN',
    'PROCEDIMIENTO',
    'RECETA',
    'ARCHIVO',
    'CONSENTIMIENTO',
  ];

  it('cubre los nueve tipos del plan más consentimientos, con etiqueta y tono', () => {
    expect(Object.keys(ETIQUETAS_TIMELINE)).toEqual(tipos);
    expect(tipos.every((t) => etiquetaEventoTimeline(t).length > 0)).toBe(true);
    expect(etiquetaEventoTimeline('PROCEDIMIENTO')).toBe('Procedimiento');
    expect(etiquetaEventoTimeline('CONSENTIMIENTO')).toBe('Consentimiento');
    expect(etiquetaEventoTimeline('CITA')).toBe('Consulta');
  });

  it('ningún tono queda indefinido (el color siempre va con texto)', () => {
    for (const tipo of tipos) {
      expect(TONES_TIMELINE[tipo]).toBeTruthy();
    }
  });

  it('ordena los tipos presentes según el abanico del plan', () => {
    const eventos: TimelineEvento[] = [
      { tipo: 'RECETA', fecha: '2026-05-01T10:00:00Z', titulo: 'Ibuprofeno', referenciaId: 1 },
      { tipo: 'EVOLUCION', fecha: '2026-06-01T10:00:00Z', titulo: 'Control', referenciaId: 2 },
      { tipo: 'ARCHIVO', fecha: '2026-04-01T10:00:00Z', titulo: 'rx.png', referenciaId: 3 },
      { tipo: 'RECETA', fecha: '2026-03-01T10:00:00Z', titulo: 'Amoxicilina', referenciaId: 4 },
    ];

    expect(tiposTimelinePresentes(eventos)).toEqual(['EVOLUCION', 'RECETA', 'ARCHIVO']);
  });
});

describe('HistoriaClinicaComponent — filtro de la línea temporal (§4.16)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistoriaClinicaComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '7' }) } },
        },
      ],
    }).compileComponents();
  });

  it('arranca sin filtro y filtra por tipo al elegir un chip', () => {
    const fixture = TestBed.createComponent(HistoriaClinicaComponent);
    const componente = fixture.componentInstance;
    componente.timeline = [
      { tipo: 'EVOLUCION', fecha: '2026-06-01T10:00:00Z', titulo: 'Control', referenciaId: 1 },
      { tipo: 'RECETA', fecha: '2026-05-01T10:00:00Z', titulo: 'Ibuprofeno', referenciaId: 2 },
      { tipo: 'RECETA', fecha: '2026-04-01T10:00:00Z', titulo: 'Amoxicilina', referenciaId: 3 },
    ];

    expect(componente.filtroTimeline).toBe('TODOS');
    expect(componente.timelineFiltrada.length).toBe(3);
    expect(componente.tiposTimeline).toEqual(['EVOLUCION', 'RECETA']);
    expect(componente.conteoTipoTimeline('RECETA')).toBe(2);

    componente.seleccionarFiltroTimeline('RECETA');

    expect(componente.timelineFiltrada.map((e) => e.referenciaId)).toEqual([2, 3]);

    componente.seleccionarFiltroTimeline('TODOS');
    expect(componente.timelineFiltrada.length).toBe(3);
  });
});
