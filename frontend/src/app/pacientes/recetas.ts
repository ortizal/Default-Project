import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, Input, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { withLoading } from '../core/loading';
import { LineaReceta, Odontologo, Receta, RecetaResumen, ViaReceta } from '../core/models';
import { UiBadgeComponent, UiBadgeTone, UiButtonComponent } from '../ui';

// ---------------------------------------------------------------------
// Catálogos y helpers (espejo de RecetaService en el backend)
// ---------------------------------------------------------------------

export const VIAS: { id: ViaReceta; label: string }[] = [
  { id: 'ORAL', label: 'Oral' },
  { id: 'SUBLINGUAL', label: 'Sublingual' },
  { id: 'TOPICA', label: 'Tópica' },
  { id: 'INTRAMUSCULAR', label: 'Intramuscular' },
  { id: 'INTRAVENOSA', label: 'Intravenosa' },
  { id: 'RECTAL', label: 'Rectal' },
  { id: 'OFTALMICA', label: 'Oftálmica' },
  { id: 'OTICA', label: 'Ótica' },
  { id: 'INHALATORIA', label: 'Inhalatoria' },
];

export function etiquetaVia(via: ViaReceta | null | undefined): string {
  if (!via) return '—';
  return VIAS.find((v) => v.id === via)?.label ?? via;
}

export function etiquetaEstado(estado: RecetaResumen['estado']): string {
  return estado === 'ANULADA' ? 'Anulada' : 'Vigente';
}

export function toneEstado(estado: RecetaResumen['estado']): UiBadgeTone {
  return estado === 'ANULADA' ? 'danger' : 'ok';
}

export interface FormLinea {
  medicamento: string;
  concentracion: string;
  presentacion: string;
  dosis: string;
  via: string;
  frecuencia: string;
  duracion: string;
  instrucciones: string;
}

export function lineaVacia(): FormLinea {
  return {
    medicamento: '',
    concentracion: '',
    presentacion: '',
    dosis: '',
    via: '',
    frecuencia: '',
    duracion: '',
    instrucciones: '',
  };
}

export function lineaValida(l: FormLinea): boolean {
  return !!l.medicamento.trim() && !!l.dosis.trim();
}

/** Pauta en una línea: "Amoxicilina 500 mg — oral, cada 8 h, 7 días". */
export function pautaTexto(l: Pick<LineaReceta, 'medicamento' | 'dosis' | 'via' | 'frecuencia' | 'duracion'>): string {
  const partes = [l.medicamento, l.dosis];
  const detalle = [l.via ? l.via.toLowerCase() : '', l.frecuencia ?? '', l.duracion ?? '']
    .filter((p) => !!p && !!p.trim())
    .join(', ');
  return `${partes.join(' ')}${detalle ? ' · ' + detalle : ''}`;
}

import { MatMenuModule } from '@angular/material/menu';

import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-recetas',
  templateUrl: './recetas.html',
  styleUrl: './recetas.css',
  imports: [
    MatIconModule,
    MatMenuModule,CommonModule, FormsModule, UiButtonComponent, UiBadgeComponent],
})
export class RecetasComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);

  @Input() pacienteId = 0;

  readonly vias = VIAS;
  readonly etiquetaVia = etiquetaVia;
  readonly etiquetaEstado = etiquetaEstado;
  readonly toneEstado = toneEstado;
  readonly pautaTexto = pautaTexto;

  escribir = false;
  cargando = false;
  error = '';
  exito = '';

  recetas: RecetaResumen[] = [];
  receta: Receta | null = null;
  odontologos: Odontologo[] = [];

  modalReceta = false;
  editandoReceta = false;
  formReceta: {
    fecha: string;
    odontologoId: number | null;
    diagnostico: string;
    observaciones: string;
    lineas: FormLinea[];
  } = { fecha: '', odontologoId: null, diagnostico: '', observaciones: '', lineas: [lineaVacia()] };

  modalAnular = false;
  motivo = '';

  ngOnInit(): void {
    this.escribir = this.auth.puede('RECETAS_WRITE');
    this.cargar();
    if (this.escribir) this.cargarOdontologos();
  }

  // ------------------------------------------------------------------
  // Cargas
  // ------------------------------------------------------------------

  cargar(): void {
    this.error = '';
    withLoading(this, this.api.get<RecetaResumen[]>(`/historia-clinica/pacientes/${this.pacienteId}/recetas`), undefined, this.cdr).subscribe({
      next: (r) => {
        this.recetas = r;
        if (r.length) this.seleccionarId(this.receta?.id ?? r[0].id);
        else this.receta = null;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  seleccionar(r: RecetaResumen): void {
    this.seleccionarId(r.id);
  }

  private seleccionarId(id: number): void {
    this.error = '';
    this.api.get<Receta>(`/historia-clinica/recetas/${id}`).subscribe({
      next: (r) => {
        this.receta = r;
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
  // Alta y edición
  // ------------------------------------------------------------------

  nuevaReceta(): void {
    if (!this.escribir) return;
    this.editandoReceta = false;
    this.formReceta = {
      fecha: this.hoy(),
      odontologoId: null,
      diagnostico: '',
      observaciones: '',
      lineas: [lineaVacia()],
    };
    this.modalReceta = true;
  }

  editarReceta(): void {
    if (!this.escribir || !this.receta) return;
    this.editandoReceta = true;
    this.formReceta = {
      fecha: this.receta.fecha,
      odontologoId: this.receta.odontologoId,
      diagnostico: this.receta.diagnostico ?? '',
      observaciones: this.receta.observaciones ?? '',
      lineas: this.receta.lineas.map((l) => ({
        medicamento: l.medicamento,
        concentracion: l.concentracion ?? '',
        presentacion: l.presentacion ?? '',
        dosis: l.dosis,
        via: l.via ?? '',
        frecuencia: l.frecuencia ?? '',
        duracion: l.duracion ?? '',
        instrucciones: l.instrucciones ?? '',
      })),
    };
    this.modalReceta = true;
  }

  agregarLinea(): void {
    this.formReceta.lineas.push(lineaVacia());
  }

  eliminarLinea(indice: number): void {
    if (this.formReceta.lineas.length === 1) return;
    this.formReceta.lineas.splice(indice, 1);
  }

  get errorReceta(): string {
    const f = this.formReceta;
    if (!f.fecha) return 'La fecha es obligatoria';
    if (!f.lineas.length) return 'La receta debe tener al menos un medicamento';
    const incompleta = f.lineas.findIndex((l) => !lineaValida(l));
    if (incompleta >= 0) return `El medicamento ${incompleta + 1} necesita nombre y dosis`;
    return '';
  }

  guardarReceta(): void {
    if (this.errorReceta) return;
    const f = this.formReceta;
    const body = {
      fecha: f.fecha || null,
      odontologoId: f.odontologoId,
      diagnostico: f.diagnostico.trim() || null,
      observaciones: f.observaciones.trim() || null,
      lineas: f.lineas.map((l) => ({
        medicamento: l.medicamento.trim(),
        concentracion: l.concentracion.trim() || null,
        presentacion: l.presentacion.trim() || null,
        dosis: l.dosis.trim(),
        via: l.via === '' ? null : l.via,
        frecuencia: l.frecuencia.trim() || null,
        duracion: l.duracion.trim() || null,
        instrucciones: l.instrucciones.trim() || null,
      })),
    };
    const destino = this.editandoReceta
      ? this.api.put<Receta>(`/historia-clinica/recetas/${this.receta!.id}`, body)
      : this.api.post<Receta>(`/historia-clinica/pacientes/${this.pacienteId}/recetas`, body);
    destino.subscribe({
      next: (r) => {
        this.modalReceta = false;
        this.ok(this.editandoReceta ? 'Receta actualizada' : `Receta No. ${r.id} creada`);
        this.receta = r;
        this.cargar();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  // ------------------------------------------------------------------
  // Anulación y PDF
  // ------------------------------------------------------------------

  abrirAnular(): void {
    if (!this.escribir || !this.receta || this.receta.estado === 'ANULADA') return;
    this.motivo = '';
    this.error = '';
    this.modalAnular = true;
  }

  anular(): void {
    if (!this.receta || !this.motivo.trim()) return;
    this.api
      .put<Receta>(`/historia-clinica/recetas/${this.receta.id}/estado`, {
        estado: 'ANULADA',
        motivo: this.motivo.trim(),
      })
      .subscribe({
        next: (r) => {
          this.modalAnular = false;
          this.ok('Receta anulada');
          this.receta = r;
          this.cargar();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  descargarPdf(): void {
    if (!this.receta) return;
    this.error = '';
    this.api.getBlob(`/historia-clinica/recetas/${this.receta.id}/pdf`).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `receta-${this.receta!.id}.pdf`;
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

  trackByIndice(indice: number): number {
    return indice;
  }

  close(e: MouseEvent): void {
    if (e.target !== e.currentTarget) return;
    this.cerrarTodos();
  }

  stop(e: MouseEvent): void {
    e.stopPropagation();
  }

  cerrarTodos(): void {
    this.modalReceta = false;
    this.modalAnular = false;
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrarTodos();
  }

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
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
