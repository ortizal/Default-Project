import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  RecetasComponent,
  VIAS,
  etiquetaEstado,
  etiquetaVia,
  lineaVacia,
  lineaValida,
  pautaTexto,
  toneEstado,
} from './recetas';
import { LineaReceta, Receta, RecetaResumen } from '../core/models';

function linea(extra: Partial<LineaReceta> = {}): LineaReceta {
  return {
    id: 1,
    numero: 1,
    medicamento: 'Amoxicilina',
    concentracion: '500 mg',
    presentacion: 'Comprimido',
    dosis: '1 cápsula',
    via: 'ORAL',
    frecuencia: 'Cada 8 h',
    duracion: '7 días',
    instrucciones: 'Tomar con alimentos',
    ...extra,
  };
}

function resumen(extra: Partial<RecetaResumen> = {}): RecetaResumen {
  return {
    id: 30,
    fecha: '2026-10-05',
    estado: 'VIGENTE',
    odontologo: 'Luis Ortiz',
    diagnostico: 'Gingivitis crónica',
    motivoAnulacion: null,
    lineas: 2,
    ...extra,
  };
}

function receta(extra: Partial<Receta> = {}): Receta {
  return {
    ...resumen(),
    pacienteId: 1,
    paciente: 'Ana Pérez',
    odontologoId: 7,
    observaciones: 'Control a los 7 días',
    lineas: [linea()],
    createdAt: '',
    updatedAt: '',
    ...extra,
  };
}

describe('Recetas — helpers (Fase E, §4.12)', () => {
  it('expone las nueve vías de administración del backend', () => {
    expect(VIAS.map((v) => v.id)).toEqual([
      'ORAL',
      'SUBLINGUAL',
      'TOPICA',
      'INTRAMUSCULAR',
      'INTRAVENOSA',
      'RECTAL',
      'OFTALMICA',
      'OTICA',
      'INHALATORIA',
    ]);
    expect(VIAS[2].label).toBe('Tópica');
  });

  it('rotula la vía y el estado', () => {
    expect(etiquetaVia('INTRAMUSCULAR')).toBe('Intramuscular');
    expect(etiquetaVia(null)).toBe('—');
    expect(etiquetaVia(undefined)).toBe('—');
    expect(etiquetaEstado('VIGENTE')).toBe('Vigente');
    expect(etiquetaEstado('ANULADA')).toBe('Anulada');
    expect(toneEstado('VIGENTE')).toBe('ok');
    expect(toneEstado('ANULADA')).toBe('danger');
  });

  it('una línea nueva está vacía y no es válida', () => {
    const l = lineaVacia();
    expect(l.medicamento).toBe('');
    expect(l.dosis).toBe('');
    expect(l.via).toBe('');
    expect(lineaValida(l)).toBe(false);
    expect(lineaValida({ ...l, medicamento: 'Amoxicilina', dosis: '500 mg' })).toBe(true);
    expect(lineaValida({ ...l, medicamento: '  ', dosis: '500 mg' })).toBe(false);
    expect(lineaValida({ ...l, medicamento: 'Amoxicilina', dosis: '   ' })).toBe(false);
  });

  it('resume la pauta en una sola línea', () => {
    expect(pautaTexto(linea())).toBe('Amoxicilina 1 cápsula · oral, Cada 8 h, 7 días');
    expect(pautaTexto(linea({ via: null, frecuencia: null, duracion: null }))).toBe(
      'Amoxicilina 1 cápsula',
    );
  });
});

describe('RecetasComponent — composición', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RecetasComponent],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();
  });

  it('arranca sin permisos, sin receta y sin modales', () => {
    const fixture = TestBed.createComponent(RecetasComponent);
    const c = fixture.componentInstance;

    expect(c.escribir).toBe(false);
    expect(c.receta).toBeNull();
    expect(c.recetas).toEqual([]);
    expect(c.modalReceta).toBe(false);
    expect(c.modalAnular).toBe(false);
    expect(c.motivo).toBe('');
  });

  it('la receta nueva sólo se abre con permiso de escritura', () => {
    const fixture = TestBed.createComponent(RecetasComponent);
    const c = fixture.componentInstance;

    c.nuevaReceta();
    expect(c.modalReceta).toBe(false);

    c.escribir = true;
    c.nuevaReceta();
    expect(c.modalReceta).toBe(true);
    expect(c.editandoReceta).toBe(false);
    expect(c.formReceta.fecha).toBeTruthy();
    expect(c.formReceta.lineas.length).toBe(1);
    expect(c.errorReceta).toContain('necesita nombre y dosis');
    c.formReceta.lineas[0].medicamento = 'Amoxicilina';
    c.formReceta.lineas[0].dosis = '500 mg';
    expect(c.errorReceta).toBe('');
  });

  it('editar recarga cabecera y líneas en el formulario', () => {
    const fixture = TestBed.createComponent(RecetasComponent);
    const c = fixture.componentInstance;

    c.editarReceta();
    expect(c.modalReceta).toBe(false);

    c.escribir = true;
    c.receta = receta();
    c.editarReceta();
    expect(c.modalReceta).toBe(true);
    expect(c.editandoReceta).toBe(true);
    expect(c.formReceta.diagnostico).toBe('Gingivitis crónica');
    expect(c.formReceta.odontologoId).toBe(7);
    expect(c.formReceta.lineas[0].medicamento).toBe('Amoxicilina');
    expect(c.formReceta.lineas[0].via).toBe('ORAL');
  });

  it('valida fecha, cantidad de líneas y medicamentos incompletos', () => {
    const fixture = TestBed.createComponent(RecetasComponent);
    const c = fixture.componentInstance;

    c.formReceta.fecha = '';
    expect(c.errorReceta).toContain('fecha');

    c.formReceta.fecha = '2026-10-05';
    c.formReceta.lineas = [];
    expect(c.errorReceta).toContain('al menos un medicamento');

    c.formReceta.lineas = [lineaVacia(), { ...lineaVacia(), medicamento: 'Ibuprofeno', dosis: '400 mg' }];
    expect(c.errorReceta).toBe('El medicamento 1 necesita nombre y dosis');

    c.formReceta.lineas = [
      { ...lineaVacia(), medicamento: 'Ibuprofeno', dosis: '400 mg' },
      { ...lineaVacia(), medicamento: 'Metronidazol', dosis: '250 mg' },
    ];
    expect(c.errorReceta).toBe('');
  });

  it('agrega y quita medicamentos sin dejar la lista vacía', () => {
    const fixture = TestBed.createComponent(RecetasComponent);
    const c = fixture.componentInstance;

    expect(c.formReceta.lineas.length).toBe(1);
    c.agregarLinea();
    c.agregarLinea();
    expect(c.formReceta.lineas.length).toBe(3);

    c.eliminarLinea(0);
    c.eliminarLinea(0);
    c.eliminarLinea(0);
    expect(c.formReceta.lineas.length).toBe(1);
  });

  it('la anulación exige permiso, receta vigente y motivo', () => {
    const fixture = TestBed.createComponent(RecetasComponent);
    const c = fixture.componentInstance;

    c.abrirAnular();
    expect(c.modalAnular).toBe(false);

    c.escribir = true;
    c.receta = receta({ estado: 'ANULADA', motivoAnulacion: 'Alergia' });
    c.abrirAnular();
    expect(c.modalAnular).toBe(false);

    c.receta = receta();
    c.abrirAnular();
    expect(c.modalAnular).toBe(true);
    expect(c.motivo).toBe('');
  });

  it('cierra los modales con Escape y conserva la lista', () => {
    const fixture = TestBed.createComponent(RecetasComponent);
    const c = fixture.componentInstance;

    c.recetas = [resumen(), resumen({ id: 29, fecha: '2026-09-01' })];
    c.modalReceta = true;
    c.modalAnular = true;
    c.cerrarConEscape();
    expect(c.modalReceta).toBe(false);
    expect(c.modalAnular).toBe(false);
    expect(c.recetas.length).toBe(2);
  });
});
