import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  GRADOS,
  NOMBRES_CAMPO,
  NOMBRES_SITIO,
  PeriodonciaComponent,
  SITIOS,
  filasDe,
  formatoMedicion,
  nombreCampo,
  piezasConMovilidad,
  promedioSondaje,
  toneCambio,
} from './periodoncia';
import { ExamenPeriodontal, MedicionSitio, PiezaPeriodontal } from '../core/models';

function medicion(diente: number, sitio: MedicionSitio['sitio'], extra: Partial<MedicionSitio> = {}): MedicionSitio {
  return {
    id: 1,
    diente,
    sitio,
    sondajeMm: null,
    margenMm: null,
    insercionMm: null,
    sangrado: false,
    placa: false,
    supuracion: false,
    ...extra,
  };
}

function pieza(diente: number, movilidad: number | null, furcacion: number | null): PiezaPeriodontal {
  return { id: 1, diente, movilidad, furcacion, notas: null };
}

function examen(sitios: MedicionSitio[], dientes: PiezaPeriodontal[]): ExamenPeriodontal {
  return {
    id: 1,
    historiaId: 10,
    version: 1,
    fecha: '2026-10-05',
    odontologo: null,
    observaciones: null,
    sitios,
    dientes,
    createdAt: '',
    updatedAt: '',
  };
}

describe('Periodoncia — helpers (Fase E, §4.11)', () => {
  it('expone los cuatro sitios en el orden clínico', () => {
    expect(SITIOS.map((s) => s.id)).toEqual(['VESTIBULAR', 'MESIAL', 'DISTAL', 'LINGUAL']);
    expect(SITIOS[0].label).toBe('Vestibular');
    expect(NOMBRES_SITIO.DISTAL).toBe('Distal');
    expect(GRADOS.map((g) => g.valor)).toEqual(['', '0', '1', '2', '3']);
  });

  it('formatea la medición como sondaje/margen/inserción más los signos', () => {
    expect(formatoMedicion(null)).toBe('—');
    expect(formatoMedicion(medicion(16, 'MESIAL'))).toBe('–/–/–');
    expect(
      formatoMedicion(
        medicion(16, 'MESIAL', { sondajeMm: 4, margenMm: 1, insercionMm: 6, sangrado: true, placa: true }),
      ),
    ).toBe('4/1/6 S P');
    expect(formatoMedicion(medicion(16, 'DISTAL', { sondajeMm: 3, supuracion: true }))).toBe('3/–/– U');
  });

  it('construye la matriz una fila por pieza, ordenada por FDI', () => {
    const filas = filasDe(
      examen(
        [
          medicion(46, 'DISTAL', { sondajeMm: 5 }),
          medicion(16, 'MESIAL', { sondajeMm: 3, sangrado: true }),
        ],
        [pieza(46, 2, 1)],
      ),
    );

    expect(filas.map((f) => f.diente)).toEqual([16, 46]);
    expect(filas[0].pieza).toBeNull();
    expect(filas[0].mediciones['MESIAL']?.sangrado).toBe(true);
    expect(filas[1].pieza?.movilidad).toBe(2);
    expect(filas[1].pieza?.furcacion).toBe(1);
    expect(filas[1].mediciones['DISTAL']?.sondajeMm).toBe(5);
  });

  it('promedia el sondaje a una décima y devuelve null sin mediciones', () => {
    expect(promedioSondaje([])).toBeNull();
    expect(promedioSondaje([medicion(16, 'MESIAL', { sondajeMm: 4 })])).toBe(4);
    expect(
      promedioSondaje([
        medicion(16, 'MESIAL', { sondajeMm: 4 }),
        medicion(16, 'DISTAL', { sondajeMm: 5 }),
        medicion(16, 'LINGUAL', { sondajeMm: null }),
      ]),
    ).toBe(4.5);
  });

  it('cuenta las piezas con movilidad o furcación', () => {
    expect(piezasConMovilidad([])).toBe(0);
    expect(piezasConMovilidad([pieza(16, 0, 0), pieza(26, null, null)])).toBe(0);
    expect(piezasConMovilidad([pieza(46, 1, 0), pieza(26, 0, 2), pieza(36, null, null)])).toBe(2);
  });

  it('los cambios y los campos se rotulan en castellano', () => {
    expect(toneCambio('NUEVO')).toBe('ok');
    expect(toneCambio('MODIFICADO')).toBe('info');
    expect(toneCambio('ELIMINADO')).toBe('danger');
    expect(nombreCampo('sondajeMm')).toBe('Sondaje (mm)');
    expect(nombreCampo('furcacion')).toBe('Furcación');
    expect(nombreCampo(null)).toBe('—');
    expect(nombreCampo('otro')).toBe('otro');
    expect(Object.keys(NOMBRES_CAMPO)).toContain('supuracion');
  });
});

describe('PeriodonciaComponent — composición', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PeriodonciaComponent],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();
  });

  it('arranca sin permisos, sin examen y sin modales', () => {
    const fixture = TestBed.createComponent(PeriodonciaComponent);
    const c = fixture.componentInstance;

    expect(c.escribir).toBe(false);
    expect(c.examen).toBeNull();
    expect(c.examenes).toEqual([]);
    expect(c.modalExamen).toBe(false);
    expect(c.modalMediciones).toBe(false);
    expect(c.modalComparar).toBe(false);
    expect(c.filas).toEqual([]);
  });

  it('el examen nuevo sólo se abre con permiso de escritura', () => {
    const fixture = TestBed.createComponent(PeriodonciaComponent);
    const c = fixture.componentInstance;

    c.nuevoExamen();
    expect(c.modalExamen).toBe(false);

    c.escribir = true;
    c.nuevoExamen();
    expect(c.modalExamen).toBe(true);
    expect(c.editandoExamen).toBe(false);
    expect(c.formExamen.copiarUltima).toBe(true);
    expect(c.formExamen.fecha).toBeTruthy();
  });

  it('editar mediciones carga las mediciones y la pieza en el formulario', () => {
    const fixture = TestBed.createComponent(PeriodonciaComponent);
    const c = fixture.componentInstance;
    const fila = filasDe(
      examen(
        [medicion(46, 'DISTAL', { sondajeMm: 5, margenMm: 2, insercionMm: 7, sangrado: true })],
        [pieza(46, 1, 0)],
      ),
    )[0];

    c.editarMediciones(fila);
    expect(c.modalMediciones).toBe(false);

    c.escribir = true;
    c.editarMediciones(fila);
    expect(c.modalMediciones).toBe(true);
    expect(c.formMed.diente).toBe(46);
    expect(c.formMed.movilidad).toBe('1');
    expect(c.formMed.furcacion).toBe('0');
    expect(c.formMed.sitios.DISTAL.sondajeMm).toBe(5);
    expect(c.formMed.sitios.DISTAL.sangrado).toBe(true);
    expect(c.formMed.sitios.MESIAL.sondajeMm).toBeNull();
    expect(c.errorMediciones).toBe('');
  });

  it('valida FDI y rangos antes de guardar', () => {
    const fixture = TestBed.createComponent(PeriodonciaComponent);
    const c = fixture.componentInstance;

    c.formMed.diente = 49;
    expect(c.errorMediciones).toContain('FDI');

    c.formMed.diente = 46;
    expect(c.errorMediciones).toBe('');

    c.formMed.sitios.MESIAL.sondajeMm = 30;
    expect(c.errorMediciones).toContain('Sondaje fuera de rango');

    c.formMed.sitios.MESIAL.sondajeMm = null;
    c.formMed.sitios.MESIAL.margenMm = -9;
    expect(c.errorMediciones).toContain('Margen fuera de rango');
  });

  it('la comparación sólo se abre con al menos dos exámenes', () => {
    const fixture = TestBed.createComponent(PeriodonciaComponent);
    const c = fixture.componentInstance;

    c.abrirComparacion();
    expect(c.modalComparar).toBe(false);

    c.examenes = [
      { id: 2, version: 2, fecha: '2026-11-01', odontologo: null, observaciones: null, sitios: 8, dientes: 1 },
      { id: 1, version: 1, fecha: '2026-10-01', odontologo: null, observaciones: null, sitios: 4, dientes: 0 },
    ];
    c.abrirComparacion();
    expect(c.modalComparar).toBe(true);
    expect(c.hasta).toBe(2);
    expect(c.desde).toBe(1);
    expect(c.comparacion).toBeNull();
  });

  it('el resumen refleja las mediciones del examen seleccionado', () => {
    const fixture = TestBed.createComponent(PeriodonciaComponent);
    const c = fixture.componentInstance;

    c.examen = examen(
      [
        medicion(16, 'MESIAL', { sondajeMm: 4, sangrado: true }),
        medicion(16, 'DISTAL', { sondajeMm: 6, supuracion: true }),
      ],
      [pieza(16, 2, 0)],
    );

    expect(c.resumen.sitios).toBe(2);
    expect(c.resumen.sangrados).toBe(2);
    expect(c.resumen.promedio).toBe(5);
    expect(c.resumen.piezas).toBe(1);
  });
});
