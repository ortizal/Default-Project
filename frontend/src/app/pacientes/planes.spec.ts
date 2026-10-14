import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  ESTADOS_LINEA,
  PlanesComponent,
  SUPERFICIES,
  TRANSICIONES_PLAN,
  TRANSICIONES_PRESUPUESTO,
  esFdiValido,
  etiquetaEstado,
  toneEstado,
  totalesDe,
  transicionesDe,
  transicionesPresupuestoDe,
} from './planes';
import { LineaPlan, Plan } from '../core/models';

function linea(precio: number, sesiones: number, descuentoPct: number, subtotal: number): LineaPlan {
  return {
    id: 1,
    numero: 1,
    fase: 1,
    diente: 26,
    superficie: null,
    diagnostico: null,
    procedimiento: 'Obturación',
    servicioId: null,
    servicio: null,
    prioridad: 'MEDIA',
    sesiones,
    precioUnitario: precio,
    descuentoPct,
    subtotal,
    estado: 'PENDIENTE',
    notas: null,
  };
}

describe('Planes de tratamiento — helpers (Fase D)', () => {
  it('la matriz de transiciones escribe sólo los caminos permitidos por el backend', () => {
    expect(TRANSICIONES_PLAN.BORRADOR).toEqual(['PRESENTADO', 'CANCELADO']);
    expect(TRANSICIONES_PLAN.PRESENTADO).toEqual([
      'ACEPTADO',
      'PARCIALMENTE_ACEPTADO',
      'RECHAZADO',
      'BORRADOR',
      'CANCELADO',
    ]);
    expect(TRANSICIONES_PLAN.EN_EJECUCION).toEqual(['COMPLETADO', 'CANCELADO']);
    expect(TRANSICIONES_PLAN.COMPLETADO).toEqual([]);
    expect(TRANSICIONES_PLAN.CANCELADO).toEqual([]);
  });

  it('transicionesDe devuelve vacío para estados terminales o desconocidos', () => {
    expect(transicionesDe('COMPLETADO')).toEqual([]);
    expect(transicionesDe('CANCELADO')).toEqual([]);
    expect(transicionesDe('RECHAZADO')).toEqual(['BORRADOR', 'CANCELADO']);
  });

  it('un presupuesto se acepta sólo desde presentado y se anula desde borrador', () => {
    expect(TRANSICIONES_PRESUPUESTO.BORRADOR).toEqual(['PRESENTADO', 'ANULADO']);
    expect(transicionesPresupuestoDe('PRESENTADO')).toContain('ACEPTADO');
    expect(transicionesPresupuestoDe('RECHAZADO')).toEqual([]);
    expect(transicionesPresupuestoDe('ANULADO')).toEqual([]);
  });

  it('etiqueta y tono de estado son legibles y accesibles', () => {
    expect(etiquetaEstado('EN_EJECUCION')).toBe('En ejecución');
    expect(etiquetaEstado('PARCIALMENTE_ACEPTADO')).toBe('Parcialmente aceptado');
    expect(etiquetaEstado('DESCONOCIDO')).toBe('DESCONOCIDO');
    expect(toneEstado('ACEPTADO')).toBe('ok');
    expect(toneEstado('RECHAZADO')).toBe('danger');
    expect(toneEstado('BORRADOR')).toBe('muted');
  });

  it('valida la notación FDI (permanentes 11-48 y temporales 51-85)', () => {
    expect(esFdiValido(26)).toBe(true);
    expect(esFdiValido(18)).toBe(true);
    expect(esFdiValido(41)).toBe(true);
    expect(esFdiValido(85)).toBe(true);
    expect(esFdiValido(null)).toBe(true);
    expect(esFdiValido(0)).toBe(false);
    expect(esFdiValido(49)).toBe(false);
    expect(esFdiValido(91)).toBe(false);
    expect(esFdiValido(19)).toBe(false);
  });

  it('calcula bruto, descuento y total como el backend', () => {
    // 80 × 2 sesiones con 10 % de descuento → 144.00
    const t = totalesDe([linea(80, 2, 10, 144)]);

    expect(t.bruto).toBe(160);
    expect(t.descuento).toBe(16);
    expect(t.total).toBe(144);
  });

  it('un plan sin líneas suma cero', () => {
    expect(totalesDe([])).toEqual({ bruto: 0, descuento: 0, total: 0 });
  });

  it('expone superficies y estados de línea coherentes con el dominio', () => {
    expect(SUPERFICIES).toEqual(['', 'OCLUSAL', 'MESIAL', 'DISTAL', 'VESTIBULAR', 'LINGUAL', 'INCISAL']);
    expect(ESTADOS_LINEA.map((e) => e.id)).toEqual([
      'PENDIENTE',
      'EN_EJECUCION',
      'COMPLETADO',
      'CANCELADO',
      'REFERIDO',
    ]);
  });
});

describe('PlanesComponent — composición', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlanesComponent],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();
  });

  it('arranca sin permisos de escritura ni plan seleccionado', () => {
    const fixture = TestBed.createComponent(PlanesComponent);
    const componente = fixture.componentInstance;

    expect(componente.escribir).toBe(false);
    expect(componente.plan).toBeNull();
    expect(componente.planes).toEqual([]);
    expect(componente.puedeEditarPlan).toBe(false);
    expect(componente.enEjecucion).toBe(false);
  });

  it('abre el modal de plan nuevo vacío', () => {
    const fixture = TestBed.createComponent(PlanesComponent);
    const componente = fixture.componentInstance;

    componente.nuevoPlan();
    expect(componente.modalPlan).toBe(true);
    expect(componente.editandoPlan).toBe(false);
    expect(componente.formPlan.titulo).toBe('');
    expect(componente.formPlan.odontologoId).toBeNull();
  });

  it('las líneas sólo se agregan desde el detalle con permiso de escritura', () => {
    const fixture = TestBed.createComponent(PlanesComponent);
    const componente = fixture.componentInstance;

    componente.nuevaLinea();
    expect(componente.modalLinea).toBe(false);

    componente.escribir = true;
    componente.plan = {
      id: 1,
      pacienteId: 7,
      paciente: 'Ana Torres',
      odontologoId: null,
      odontologo: null,
      titulo: 'Plan',
      estado: 'BORRADOR',
      observaciones: null,
      fecha: '2026-10-03',
      subtotal: 0,
      total: 0,
      comentario: null,
      lineas: [],
      presupuestos: [],
    } as Plan;
    componente.nuevaLinea();
    expect(componente.modalLinea).toBe(true);
    expect(componente.editandoLinea).toBeNull();
    expect(componente.formLinea.fase).toBe(1);
    expect(componente.formLinea.prioridad).toBe('MEDIA');
    expect(componente.formLinea.sesiones).toBe(1);
  });
});
