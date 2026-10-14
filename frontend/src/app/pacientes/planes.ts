import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, Input, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { withLoading } from '../core/loading';
import {
  EstadoLineaPlan,
  EstadoPlanTratamiento,
  EstadoPresupuesto,
  LineaPlan,
  Odontologo,
  Plan,
  PlanResumen,
  Presupuesto,
  PrioridadLinea,
} from '../core/models';
import { UiBadgeComponent, UiBadgeTone, UiButtonComponent, UiConfirmService } from '../ui';

// ---------------------------------------------------------------------
// Matrices (espejo exacto de PlanTratamientoService en el backend)
// ---------------------------------------------------------------------

export const TRANSICIONES_PLAN: Record<EstadoPlanTratamiento, EstadoPlanTratamiento[]> = {
  BORRADOR: ['PRESENTADO', 'CANCELADO'],
  PRESENTADO: ['ACEPTADO', 'PARCIALMENTE_ACEPTADO', 'RECHAZADO', 'BORRADOR', 'CANCELADO'],
  ACEPTADO: ['EN_EJECUCION', 'CANCELADO'],
  PARCIALMENTE_ACEPTADO: ['EN_EJECUCION', 'CANCELADO'],
  RECHAZADO: ['BORRADOR', 'CANCELADO'],
  EN_EJECUCION: ['COMPLETADO', 'CANCELADO'],
  COMPLETADO: [],
  CANCELADO: [],
};

export const TRANSICIONES_PRESUPUESTO: Record<EstadoPresupuesto, EstadoPresupuesto[]> = {
  BORRADOR: ['PRESENTADO', 'ANULADO'],
  PRESENTADO: ['ACEPTADO', 'RECHAZADO', 'ANULADO'],
  ACEPTADO: ['ANULADO'],
  RECHAZADO: [],
  ANULADO: [],
};

export const ESTADOS_PLAN: EstadoPlanTratamiento[] = [
  'BORRADOR',
  'PRESENTADO',
  'ACEPTADO',
  'PARCIALMENTE_ACEPTADO',
  'RECHAZADO',
  'EN_EJECUCION',
  'COMPLETADO',
  'CANCELADO',
];

export const PRIORIDADES: { id: PrioridadLinea; label: string }[] = [
  { id: 'BAJA', label: 'Baja' },
  { id: 'MEDIA', label: 'Media' },
  { id: 'ALTA', label: 'Alta' },
  { id: 'URGENTE', label: 'Urgente' },
];

export const SUPERFICIES = ['', 'OCLUSAL', 'MESIAL', 'DISTAL', 'VESTIBULAR', 'LINGUAL', 'INCISAL'];

export const ESTADOS_LINEA: { id: EstadoLineaPlan; label: string }[] = [
  { id: 'PENDIENTE', label: 'Pendiente' },
  { id: 'EN_EJECUCION', label: 'En ejecución' },
  { id: 'COMPLETADO', label: 'Completado' },
  { id: 'CANCELADO', label: 'Cancelado' },
  { id: 'REFERIDO', label: 'Referido' },
];

/** Destinos permitidos para un estado del plan. */
export function transicionesDe(estado: EstadoPlanTratamiento): EstadoPlanTratamiento[] {
  return TRANSICIONES_PLAN[estado] ?? [];
}

/** Destinos permitidos para un presupuesto. */
export function transicionesPresupuestoDe(estado: EstadoPresupuesto): EstadoPresupuesto[] {
  return TRANSICIONES_PRESUPUESTO[estado] ?? [];
}

const ETIQUETAS: Record<string, string> = {
  BORRADOR: 'Borrador',
  PRESENTADO: 'Presentado',
  ACEPTADO: 'Aceptado',
  PARCIALMENTE_ACEPTADO: 'Parcialmente aceptado',
  RECHAZADO: 'Rechazado',
  EN_EJECUCION: 'En ejecución',
  COMPLETADO: 'Completado',
  CANCELADO: 'Cancelado',
  ANULADO: 'Anulado',
  PENDIENTE: 'Pendiente',
};

export function etiquetaEstado(estado: string): string {
  return ETIQUETAS[estado] ?? estado;
}

const TONES: Record<string, UiBadgeTone> = {
  BORRADOR: 'muted',
  PRESENTADO: 'info',
  ACEPTADO: 'ok',
  PARCIALMENTE_ACEPTADO: 'warn',
  RECHAZADO: 'danger',
  EN_EJECUCION: 'primary',
  COMPLETADO: 'ok',
  CANCELADO: 'muted',
  ANULADO: 'muted',
  PENDIENTE: 'muted',
};

const VERBOS: Record<string, string> = {
  BORRADOR: 'Guardar borrador',
  PRESENTADO: 'Presentar',
  ACEPTADO: 'Aceptar',
  RECHAZADO: 'Rechazar',
  ANULADO: 'Anular',
};

/** Verbo de la acción disponible para un estado de presupuesto (en infinitivo). */
export function verboPresupuesto(estado: EstadoPresupuesto): string {
  return VERBOS[estado] ?? etiquetaEstado(estado);
}

/** Tono del badge según el estado (un solo mapeo para plan, línea y presupuesto). */
export function toneEstado(estado: string): UiBadgeTone {
  return TONES[estado] ?? 'muted';
}

/** Pieza válida en notación FDI (permanentes 11-48, temporales 51-85). */
export function esFdiValido(diente: number | null | undefined): boolean {
  if (diente == null || !Number.isFinite(diente)) return true;
  const cuadrante = Math.trunc(diente / 10);
  const pieza = diente % 10;
  return (cuadrante >= 1 && cuadrante <= 4 && pieza >= 1 && pieza <= 8)
    || (cuadrante >= 5 && cuadrante <= 8 && pieza >= 1 && pieza <= 5);
}

export interface Totales {
  bruto: number;
  descuento: number;
  total: number;
}

/** Totales en el cliente para previsualizar el presupuesto antes de emitirlo. */
export function totalesDe(
  lineas: Pick<LineaPlan, 'precioUnitario' | 'sesiones' | 'descuentoPct' | 'subtotal'>[],
): Totales {
  let bruto = 0;
  let total = 0;
  for (const l of lineas) {
    bruto += (l.precioUnitario ?? 0) * Math.max(1, l.sesiones ?? 1);
    total += l.subtotal ?? 0;
  }
  return { bruto: Math.round(bruto * 100) / 100, descuento: Math.round((bruto - total) * 100) / 100, total: Math.round(total * 100) / 100 };
}

interface FormLinea {
  numero: number | null;
  fase: number;
  diente: number | null;
  superficie: string;
  diagnostico: string;
  procedimiento: string;
  servicioId: number | null;
  prioridad: PrioridadLinea;
  sesiones: number;
  precioUnitario: number;
  descuentoPct: number;
  notas: string;
}

function formLineaVacia(): FormLinea {
  return {
    numero: null,
    fase: 1,
    diente: null,
    superficie: '',
    diagnostico: '',
    procedimiento: '',
    servicioId: null,
    prioridad: 'MEDIA',
    sesiones: 1,
    precioUnitario: 0,
    descuentoPct: 0,
    notas: '',
  };
}

import { MatMenuModule } from '@angular/material/menu';

import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-planes',
  templateUrl: './planes.html',
  styleUrl: './planes.css',
  imports: [
    MatIconModule,
    MatMenuModule,CommonModule, FormsModule, UiButtonComponent, UiBadgeComponent],
})
export class PlanesComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);
  private readonly confirmacion = inject(UiConfirmService);

  @Input() pacienteId = 0;

  readonly prioridades = PRIORIDADES;
  readonly superficies = SUPERFICIES;
  readonly estadosLinea = ESTADOS_LINEA;
  readonly etiquetaEstado = etiquetaEstado;
  readonly toneEstado = toneEstado;
  readonly verboPresupuesto = verboPresupuesto;
  readonly transicionesDe = transicionesDe;
  readonly transicionesPresupuestoDe = transicionesPresupuestoDe;
  readonly totalesDe = totalesDe;

  escribir = false;
  cargando = false;
  error = '';
  exito = '';

  planes: PlanResumen[] = [];
  plan: Plan | null = null;
  odontologos: Odontologo[] = [];

  modalPlan = false;
  editandoPlan = false;
  formPlan: { titulo: string; observaciones: string; odontologoId: number | null } = {
    titulo: '',
    observaciones: '',
    odontologoId: null,
  };

  modalEstado = false;
  destino: EstadoPlanTratamiento | null = null;
  comentarioEstado = '';

  modalLinea = false;
  editandoLinea: number | null = null;
  formLinea: FormLinea = formLineaVacia();

  modalPresupuesto = false;
  presupuesto: Presupuesto | null = null;

  modalAceptacion = false;
  firmante = '';
  comentarioAceptacion = '';

  ngOnInit(): void {
    this.escribir = this.auth.puede('PLAN_TRATAMIENTO_WRITE');
    this.cargar();
    if (this.escribir) this.cargarOdontologos();
  }

  get totales(): Totales {
    return totalesDe(this.plan?.lineas ?? []);
  }

  get puedeEditarPlan(): boolean {
    return this.escribir && !!this.plan && ['BORRADOR', 'PRESENTADO', 'RECHAZADO'].includes(this.plan.estado);
  }

  get enEjecucion(): boolean {
    return !!this.plan && this.plan.estado === 'EN_EJECUCION';
  }

  // ------------------------------------------------------------------
  // Cargas
  // ------------------------------------------------------------------

  cargar(): void {
    this.error = '';
    withLoading(this, this.api.get<PlanResumen[]>(`/planes-tratamiento/pacientes/${this.pacienteId}`), undefined, this.cdr).subscribe({
      next: (r) => {
        this.planes = r;
        if (this.plan) this.seleccionarId(this.plan.id);
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  seleccionar(p: PlanResumen): void {
    this.seleccionarId(p.id);
  }

  private seleccionarId(id: number): void {
    this.error = '';
    this.api.get<Plan>(`/planes-tratamiento/${id}`).subscribe({
      next: (r) => {
        this.plan = r;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  private cargarOdontologos(): void {
    this.api.get<Odontologo[]>('/odontologos/activos').subscribe({
      next: (r) => {
        this.odontologos = r;
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  // ------------------------------------------------------------------
  // Plan
  // ------------------------------------------------------------------

  nuevoPlan(): void {
    this.editandoPlan = false;
    this.formPlan = { titulo: '', observaciones: '', odontologoId: null };
    this.modalPlan = true;
  }

  editarPlan(): void {
    if (!this.plan) return;
    this.editandoPlan = true;
    this.formPlan = {
      titulo: this.plan.titulo,
      observaciones: this.plan.observaciones ?? '',
      odontologoId: this.plan.odontologoId,
    };
    this.modalPlan = true;
  }

  guardarPlan(): void {
    const titulo = this.formPlan.titulo.trim();
    if (!titulo) return;
    const body = {
      titulo,
      observaciones: this.formPlan.observaciones.trim() || null,
      odontologoId: this.formPlan.odontologoId,
      fecha: null,
    };
    const ruta = this.editandoPlan
      ? this.api.put<Plan>(`/planes-tratamiento/${this.plan!.id}`, body)
      : this.api.post<Plan>(`/planes-tratamiento/pacientes/${this.pacienteId}`, body);
    ruta.subscribe({
      next: (r) => {
        this.modalPlan = false;
        this.ok(this.editandoPlan ? 'Plan actualizado' : 'Plan creado en borrador');
        this.cargar();
        this.seleccionarId(r.id);
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  async eliminarPlan(): Promise<void> {
    if (!this.plan) return;
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Eliminar plan de tratamiento',
        mensaje: `¿Eliminar el plan "${this.plan.titulo}"? Sólo se permite si está en borrador.`,
        confirmarTexto: 'Eliminar',
        peligro: true,
      }))
    )
      return;
    this.api.del<void>(`/planes-tratamiento/${this.plan.id}`).subscribe({
      next: () => {
        this.plan = null;
        this.ok('Plan eliminado');
        this.cargar();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  abrirCambioEstado(): void {
    if (!this.plan) return;
    this.destino = transicionesDe(this.plan.estado)[0] ?? null;
    this.comentarioEstado = '';
    this.modalEstado = true;
  }

  cambiarEstadoPlan(): void {
    if (!this.plan || !this.destino) return;
    this.api
      .put<Plan>(`/planes-tratamiento/${this.plan.id}/estado`, {
        estado: this.destino,
        comentario: this.comentarioEstado.trim() || null,
      })
      .subscribe({
        next: (r) => {
          this.modalEstado = false;
          this.ok(`Plan en estado ${etiquetaEstado(r.estado)}`);
          this.plan = r;
          this.cargar();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  // ------------------------------------------------------------------
  // Líneas
  // ------------------------------------------------------------------

  nuevaLinea(): void {
    if (!this.puedeEditarPlan) return;
    this.editandoLinea = null;
    this.formLinea = formLineaVacia();
    this.modalLinea = true;
  }

  editarLinea(l: LineaPlan): void {
    if (!this.puedeEditarPlan) return;
    this.editandoLinea = l.id;
    this.formLinea = {
      numero: l.numero,
      fase: l.fase,
      diente: l.diente,
      superficie: l.superficie ?? '',
      diagnostico: l.diagnostico ?? '',
      procedimiento: l.procedimiento,
      servicioId: l.servicioId,
      prioridad: l.prioridad,
      sesiones: l.sesiones,
      precioUnitario: l.precioUnitario,
      descuentoPct: l.descuentoPct,
      notas: l.notas ?? '',
    };
    this.modalLinea = true;
  }

  guardarLinea(): void {
    if (!this.plan) return;
    const f = this.formLinea;
    if (!f.procedimiento.trim()) {
      this.error = 'El procedimiento es obligatorio';
      this.cdr.markForCheck();
      return;
    }
    if (!esFdiValido(f.diente)) {
      this.error = `El número FDI ${f.diente} no corresponde a una pieza válida`;
      this.cdr.markForCheck();
      return;
    }
    this.error = '';
    const body = {
      numero: f.numero && f.numero > 0 ? f.numero : null,
      fase: f.fase,
      diente: f.diente,
      superficie: f.superficie === '' ? null : f.superficie,
      diagnostico: f.diagnostico.trim() || null,
      procedimiento: f.procedimiento.trim(),
      servicioId: f.servicioId,
      prioridad: f.prioridad,
      sesiones: f.sesiones,
      precioUnitario: f.precioUnitario,
      descuentoPct: f.descuentoPct,
      notas: f.notas.trim() || null,
    };
    const peticion$ = this.editandoLinea
      ? this.api.put<LineaPlan>(`/planes-tratamiento/${this.plan.id}/lineas/${this.editandoLinea}`, body)
      : this.api.post<LineaPlan>(`/planes-tratamiento/${this.plan.id}/lineas`, body);
    peticion$.subscribe({
      next: () => {
        this.modalLinea = false;
        this.ok('Línea guardada');
        this.seleccionarId(this.plan!.id);
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  async eliminarLinea(l: LineaPlan): Promise<void> {
    if (!this.plan) return;
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Eliminar línea',
        mensaje: `¿Eliminar "${l.procedimiento}" del plan?`,
        confirmarTexto: 'Eliminar',
        peligro: true,
      }))
    )
      return;
    this.api.del<void>(`/planes-tratamiento/${this.plan.id}/lineas/${l.id}`).subscribe({
      next: () => {
        this.ok('Línea eliminada');
        this.seleccionarId(this.plan!.id);
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  cambiarEstadoLinea(l: LineaPlan, estado: EstadoLineaPlan): void {
    if (!this.plan || estado === l.estado) return;
    this.api
      .put<LineaPlan>(`/planes-tratamiento/${this.plan.id}/lineas/${l.id}/estado`, { estado })
      .subscribe({
        next: () => {
          this.ok(`Línea "${l.procedimiento}": ${etiquetaEstado(estado)}`);
          this.seleccionarId(this.plan!.id);
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cargar();
          this.cdr.markForCheck();
        },
      });
  }

  // ------------------------------------------------------------------
  // Presupuestos
  // ------------------------------------------------------------------

  async emitirPresupuesto(): Promise<void> {
    if (!this.plan || !this.plan.lineas.length) return;
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Emitir presupuesto',
        mensaje: `Se congela el estado actual de las ${this.plan.lineas.length} líneas (total $ ${totalesDe(this.plan.lineas).total.toFixed(2)}).`,
        confirmarTexto: 'Emitir',
      }))
    )
      return;
    this.api
      .post<Presupuesto>(`/planes-tratamiento/${this.plan.id}/presupuestos`, { observaciones: null })
      .subscribe({
        next: (r) => {
          this.ok(`Presupuesto No. ${r.numero} emitido`);
          this.seleccionarId(this.plan!.id);
          this.verPresupuesto(r);
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  verPresupuesto(p: { id: number }): void {
    if (!this.plan) return;
    this.api.get<Presupuesto>(`/planes-tratamiento/${this.plan.id}/presupuestos/${p.id}`).subscribe({
      next: (r) => {
        this.presupuesto = r;
        this.modalPresupuesto = true;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  accionesPresupuesto(): EstadoPresupuesto[] {
    return this.presupuesto ? transicionesPresupuestoDe(this.presupuesto.estado) : [];
  }

  accionPresupuesto(destino: EstadoPresupuesto): void {
    if (!this.presupuesto) return;
    if (destino === 'ACEPTADO') {
      this.firmante = '';
      this.comentarioAceptacion = '';
      this.modalAceptacion = true;
      return;
    }
    void this.aplicarPresupuesto(destino, null, null);
  }

  aceptarPresupuesto(): void {
    if (!this.firmante.trim()) return;
    void this.aplicarPresupuesto('ACEPTADO', this.firmante.trim(), this.comentarioAceptacion.trim() || null, false);
  }

  private async aplicarPresupuesto(
    destino: EstadoPresupuesto,
    firmante: string | null,
    comentario: string | null,
    confirmar = true,
  ): Promise<void> {
    if (!this.plan || !this.presupuesto) return;
    if (
      confirmar &&
      !(await this.confirmacion.abrir({
        titulo: `Presupuesto → ${etiquetaEstado(destino)}`,
        mensaje: `¿${destino === 'ACEPTADO' ? 'aceptar' : destino === 'RECHAZADO' ? 'rechazar' : 'anular'} el presupuesto No. ${this.presupuesto.numero}?`,
        confirmarTexto: etiquetaEstado(destino),
        peligro: destino !== 'ACEPTADO',
      }))
    )
      return;
    this.api
      .put<Presupuesto>(
        `/planes-tratamiento/${this.plan.id}/presupuestos/${this.presupuesto.id}/estado`,
        { estado: destino, firmante, comentario },
      )
      .subscribe({
        next: (r) => {
          this.modalAceptacion = false;
          this.modalPresupuesto = false;
          this.ok(`Presupuesto ${etiquetaEstado(r.estado).toLowerCase()}`);
          this.seleccionarId(this.plan!.id);
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  descargarPdf(): void {
    if (!this.plan) return;
    this.error = '';
    this.api.getBlob(`/planes-tratamiento/${this.plan.id}/pdf`).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `plan-tratamiento-${this.plan!.id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  // ------------------------------------------------------------------
  // Utilidades
  // ------------------------------------------------------------------

  trackById(_: number, item: { id: number }): number {
    return item.id;
  }

  trackByNumero(_: number, item: { numero: number }): number {
    return item.numero;
  }

  close(e: MouseEvent): void {
    if (e.target !== e.currentTarget) return;
    this.cerrarTodos();
  }

  stop(e: MouseEvent): void {
    e.stopPropagation();
  }

  cerrarTodos(): void {
    this.modalPlan = false;
    this.modalEstado = false;
    this.modalLinea = false;
    this.modalPresupuesto = false;
    this.modalAceptacion = false;
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrarTodos();
  }

  private ok(mensaje: string): void {
    this.exito = mensaje;
    this.error = '';
    setTimeout(() => {
      this.exito = '';
      this.cdr.markForCheck();
    }, 3000);
    this.cdr.markForCheck();
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string; mensaje?: string }; status?: number };
    if (a?.status === 409 || a?.status === 400 || a?.status === 404)
      return a.error?.message ?? a.error?.mensaje ?? 'Datos inválidos';
    return 'Error de conexión';
  }
}
