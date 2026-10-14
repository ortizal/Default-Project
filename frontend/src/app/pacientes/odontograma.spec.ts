import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  CONDICIONES_ODONTOGRAMA,
  DIENTES_PERMANENTE,
  DIENTES_TEMPORAL,
  ESTADOS_ODONTOGRAMA,
  OdontogramaComponent,
  colorEstado,
  condicionesDe,
  dienteCompleto,
  esAnterior,
  etiquetaCambio,
  etiquetaCondicion,
  etiquetaEstado,
  etiquetaSuperficie,
  estadoVisible,
  filasDe,
  porDiente,
  poligonosTooth,
  superficieCentro,
} from './odontograma';
import { CambioOdontograma, DienteOdontograma } from '../core/models';

function condicion(diente: number, superficie: DienteOdontograma['superficie'], estado: DienteOdontograma['estado']): DienteOdontograma {
  return { id: diente, diente, superficie, condicion: 'CARIES', estado, notas: null };
}

describe('Odontograma — dentición y FDI (Fase C)', () => {
  it('dibuja dos filas de 16 piezas en dentición permanente', () => {
    const filas = filasDe('PERMANENTE');
    expect(filas.length).toBe(2);
    expect(filas[0].numeros.length).toBe(16);
    expect(filas[0].numeros[0]).toBe(18);
    expect(filas[0].numeros[8]).toBe(21);
    expect(filas[1].numeros).toEqual(DIENTES_PERMANENTE.inferior);
  });

  it('la dentición temporal tiene 10 piezas por arcada (FDI 51-55 / 61-65)', () => {
    const filas = filasDe('TEMPORAL');
    expect(filas.length).toBe(2);
    expect(filas[0].numeros).toEqual(DIENTES_TEMPORAL.superior);
    expect(filas[1].numeros.length).toBe(10);
  });

  it('la dentición mixta añade las arcadas temporales a las permanentes', () => {
    const filas = filasDe('MIXTA');
    expect(filas.length).toBe(4);
    expect(filas.map((f) => f.arco)).toEqual(['superior', 'superior', 'inferior', 'inferior']);
    expect(filas[1].numeros[0]).toBe(55);
  });

  it('las piezas anteriores usan incisal y las posteriores oclusal', () => {
    expect(esAnterior(11)).toBe(true);
    expect(esAnterior(13)).toBe(true);
    expect(esAnterior(14)).toBe(false);
    expect(esAnterior(51)).toBe(true);
    expect(superficieCentro(11)).toBe('INCISAL');
    expect(superficieCentro(16)).toBe('OCLUSAL');
    expect(superficieCentro(55)).toBe('OCLUSAL');
  });

  it('proporciona las cinco regiones de la caja del diente', () => {
    const p = poligonosTooth(36);
    expect(Object.keys(p)).toEqual(['arriba', 'abajo', 'izq', 'der', 'centro']);
    expect(p.centro).toBe('12,12 24,12 24,24 12,24');
    expect(p.arriba.split(' ').length).toBe(4);
  });
});

describe('Odontograma — condiciones, colores y etiquetas', () => {
  const mapa = porDiente([
    condicion(16, 'OCLUSAL', 'DIAGNOSTICO'),
    condicion(16, 'MESIAL', 'COMPLETADO'),
    condicion(21, null, 'EXISTENTE'),
    condicion(21, 'INCISAL', 'PLANIFICADO'),
  ]);

  it('agrupa las condiciones por número de diente', () => {
    expect(Object.keys(mapa).sort()).toEqual(['16', '21']);
    expect(mapa[16].length).toBe(2);
  });

  it('reparte las condiciones en su región del dibujo', () => {
    expect(condicionesDe(mapa, 16, 'centro').map((d) => d.superficie)).toEqual(['OCLUSAL']);
    expect(condicionesDe(mapa, 16, 'izq').map((d) => d.superficie)).toEqual(['MESIAL']);
    expect(condicionesDe(mapa, 16, 'arriba')).toEqual([]);
    expect(condicionesDe(mapa, 21, 'centro').map((d) => d.superficie)).toEqual(['INCISAL']);
  });

  it('las condiciones de diente completo no se pintan en las regiones', () => {
    expect(dienteCompleto(mapa, 21).map((d) => d.diente)).toEqual([21]);
    expect(condicionesDe(mapa, 21, 'izq')).toEqual([]);
  });

  it('pinta la región con el estado más avanzado', () => {
    expect(estadoVisible(condicionesDe(mapa, 16, 'centro'))).toBe('DIAGNOSTICO');
    expect(estadoVisible([condicion(16, null, 'PLANIFICADO'), condicion(16, null, 'COMPLETADO')])).toBe('COMPLETADO');
    expect(estadoVisible([])).toBeNull();
  });

  it('colorea todos los estados definidos y deja en blanco lo vacío', () => {
    for (const e of ESTADOS_ODONTOGRAMA) {
      expect(colorEstado(e.id)).toMatch(/^#[0-9a-f]{6}$/i);
    }
    expect(colorEstado(null)).toBe('#ffffff');
  });

  it('etiqueta superficies, estados y condiciones para la tabla y la leyenda', () => {
    expect(etiquetaSuperficie(null)).toBe('Diente completo');
    expect(etiquetaSuperficie('INCISAL')).toBe('Incisal');
    expect(etiquetaEstado('PROCESO')).toBe('En proceso');
    expect(etiquetaCondicion('PROTESIS_FIJA')).toBe('Protesis Fija');
    expect(CONDICIONES_ODONTOGRAMA.length).toBeGreaterThanOrEqual(10);
  });

  it('resume los tipos de cambio de la comparación temporal', () => {
    const nuevo: CambioOdontograma = {
      diente: 16,
      superficie: 'OCLUSAL',
      condicion: 'CARIES',
      estadoAnterior: null,
      estadoNuevo: 'DIAGNOSTICO',
      tipo: 'NUEVO',
    };
    const fuera: CambioOdontograma = { ...nuevo, tipo: 'ELIMINADO', estadoAnterior: 'COMPLETADO', estadoNuevo: null };
    const cambio: CambioOdontograma = {
      ...nuevo,
      tipo: 'MODIFICADO',
      estadoAnterior: 'DIAGNOSTICO',
      estadoNuevo: 'PROCESO',
    };
    expect(etiquetaCambio(nuevo)).toContain('+ 16');
    expect(etiquetaCambio(fuera)).toContain('− 16');
    expect(etiquetaCambio(cambio)).toContain('Diagnóstico → En proceso');
  });
});

describe('OdontogramaComponent — composición', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OdontogramaComponent],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();
  });

  it('arranca en dentición permanente y con la edición apagada hasta ngOnInit', () => {
    const fixture = TestBed.createComponent(OdontogramaComponent);
    fixture.componentInstance.pacienteId = 7;
    expect(fixture.componentInstance.filas.length).toBe(2);
    expect(fixture.componentInstance.puedeEditar).toBe(false);
    expect(fixture.componentInstance.modal).toBe(false);
  });
});
