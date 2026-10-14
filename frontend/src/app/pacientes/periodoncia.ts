import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, Input, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { withLoading } from '../core/loading';
import {
  CambioPeriodontal,
  ComparacionPeriodontal,
  ExamenPeriodontal,
  ExamenPeriodontalResumen,
  MedicionSitio,
  PiezaPeriodontal,
  SitioPeriodontal,
} from '../core/models';
import { UiBadgeComponent, UiBadgeTone, UiButtonComponent, UiConfirmService } from '../ui';
import { esFdiValido } from './planes';

// ---------------------------------------------------------------------
// Catálogos y helpers (espejo de PeriodontogramaService en el backend)
// ---------------------------------------------------------------------

export const SITIOS: { id: SitioPeriodontal; label: string; corto: string }[] = [
  { id: 'VESTIBULAR', label: 'Vestibular', corto: 'Ves.' },
  { id: 'MESIAL', label: 'Mesial', corto: 'Mes.' },
  { id: 'DISTAL', label: 'Distal', corto: 'Dis.' },
  { id: 'LINGUAL', label: 'Lingual', corto: 'Lin.' },
];

export const NOMBRES_SITIO: Record<SitioPeriodontal, string> = {
  VESTIBULAR: 'Vestibular',
  MESIAL: 'Mesial',
  DISTAL: 'Distal',
  LINGUAL: 'Lingual',
};

export const NOMBRES_CAMPO: Record<string, string> = {
  sondajeMm: 'Sondaje (mm)',
  margenMm: 'Margen (mm)',
  insercionMm: 'Inserción (mm)',
  sangrado: 'Sangrado',
  placa: 'Placa',
  supuracion: 'Supuración',
  movilidad: 'Movilidad',
  furcacion: 'Furcación',
  notas: 'Notas',
  medicion: 'Medición',
  pieza: 'Pieza',
};

export const GRADOS: { valor: string; label: string }[] = [
  { valor: '', label: '—' },
  { valor: '0', label: '0' },
  { valor: '1', label: '1' },
  { valor: '2', label: '2' },
  { valor: '3', label: '3' },
];

const TONE_CAMBIO: Record<CambioPeriodontal['tipo'], UiBadgeTone> = {
  NUEVO: 'ok',
  MODIFICADO: 'info',
  ELIMINADO: 'danger',
};

export function toneCambio(tipo: CambioPeriodontal['tipo']): UiBadgeTone {
  return TONE_CAMBIO[tipo] ?? 'muted';
}

export function nombreCampo(campo: string | null): string {
  if (!campo) return '—';
  return NOMBRES_CAMPO[campo] ?? campo;
}

/** Texto de una medición en la celda de la matriz: "3/1/4 S P". */
export function formatoMedicion(m: MedicionSitio | undefined | null): string {
  if (!m) return '—';
  const mm = (v: number | null): string => (v == null ? '–' : String(v));
  let texto = `${mm(m.sondajeMm)}/${mm(m.margenMm)}/${mm(m.insercionMm)}`;
  if (m.sangrado) texto += ' S';
  if (m.placa) texto += ' P';
  if (m.supuracion) texto += ' U';
  return texto;
}

export interface FilaPeriodontal {
  diente: number;
  mediciones: Partial<Record<SitioPeriodontal, MedicionSitio>>;
  pieza: PiezaPeriodontal | null;
}

/** Matriz (una fila por pieza) a partir de las mediciones del examen. */
export function filasDe(examen: Pick<ExamenPeriodontal, 'sitios' | 'dientes'>): FilaPeriodontal[] {
  const mapa = new Map<number, FilaPeriodontal>();
  for (const m of examen.sitios) {
    const fila = mapa.get(m.diente) ?? { diente: m.diente, mediciones: {}, pieza: null };
    fila.mediciones[m.sitio] = m;
    mapa.set(m.diente, fila);
  }
  for (const d of examen.dientes) {
    const fila = mapa.get(d.diente) ?? { diente: d.diente, mediciones: {}, pieza: null };
    fila.pieza = d;
    mapa.set(d.diente, fila);
  }
  return [...mapa.values()].sort((a, b) => a.diente - b.diente);
}

/** Promedio de sondaje en mm (una décima) o null si no hay mediciones. */
export function promedioSondaje(sitios: MedicionSitio[]): number | null {
  const valores = sitios.filter((s) => s.sondajeMm != null).map((s) => s.sondajeMm as number);
  if (!valores.length) return null;
  const suma = valores.reduce((a, b) => a + b, 0);
  return Math.round((suma / valores.length) * 10) / 10;
}

/** Piezas con algún signo de alarma (movilidad o furcación > 0). */
export function piezasConMovilidad(dientes: PiezaPeriodontal[]): number {
  return dientes.filter((d) => (d.movilidad ?? 0) > 0 || (d.furcacion ?? 0) > 0).length;
}

interface FormSitio {
  sondajeMm: number | null;
  margenMm: number | null;
  insercionMm: number | null;
  sangrado: boolean;
  placa: boolean;
  supuracion: boolean;
}

interface FormMediciones {
  diente: number;
  movilidad: string;
  furcacion: string;
  notas: string;
  sitios: Record<SitioPeriodontal, FormSitio>;
}

function sitioVacio(): FormSitio {
  return { sondajeMm: null, margenMm: null, insercionMm: null, sangrado: false, placa: false, supuracion: false };
}

function formMedicionesVacio(diente: number): FormMediciones {
  return {
    diente,
    movilidad: '',
    furcacion: '',
    notas: '',
    sitios: {
      VESTIBULAR: sitioVacio(),
      MESIAL: sitioVacio(),
      DISTAL: sitioVacio(),
      LINGUAL: sitioVacio(),
    },
  };
}

import { MatMenuModule } from '@angular/material/menu';

import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-periodoncia',
  templateUrl: './periodoncia.html',
  styleUrl: './periodoncia.css',
  imports: [
    MatIconModule,
    MatMenuModule,CommonModule, FormsModule, UiButtonComponent, UiBadgeComponent],
})
export class PeriodonciaComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);
  private readonly confirmacion = inject(UiConfirmService);

  @Input() pacienteId = 0;

  readonly sitios = SITIOS;
  readonly grados = GRADOS;
  readonly formatoMedicion = formatoMedicion;
  readonly filasDe = filasDe;
  readonly promedioSondaje = promedioSondaje;
  readonly piezasConMovilidad = piezasConMovilidad;
  readonly toneCambio = toneCambio;
  readonly nombreCampo = nombreCampo;

  escribir = false;
  cargando = false;
  error = '';
  exito = '';

  examenes: ExamenPeriodontalResumen[] = [];
  examen: ExamenPeriodontal | null = null;

  modalExamen = false;
  editandoExamen = false;
  formExamen: { fecha: string; observaciones: string; copiarUltima: boolean } = {
    fecha: '',
    observaciones: '',
    copiarUltima: true,
  };

  modalMediciones = false;
  formMed: FormMediciones = formMedicionesVacio(0);

  modalComparar = false;
  desde = 0;
  hasta = 0;
  comparacion: ComparacionPeriodontal | null = null;

  ngOnInit(): void {
    this.escribir = this.auth.puede('PERIODONCIA_WRITE');
    this.cargar();
  }

  get filas(): FilaPeriodontal[] {
    return this.examen ? filasDe(this.examen) : [];
  }

  get resumen(): { sitios: number; sangrados: number; promedio: number | null; piezas: number } {
    const sitios = this.examen?.sitios ?? [];
    const dientes = this.examen?.dientes ?? [];
    return {
      sitios: sitios.length,
      sangrados: sitios.filter((s) => s.sangrado || s.supuracion).length,
      promedio: promedioSondaje(sitios),
      piezas: piezasConMovilidad(dientes),
    };
  }

  // ------------------------------------------------------------------
  // Cargas
  // ------------------------------------------------------------------

  cargar(): void {
    this.error = '';
    withLoading(this, this.api.get<ExamenPeriodontalResumen[]>(`/historia-clinica/pacientes/${this.pacienteId}/periodontogramas`), undefined, this.cdr).subscribe({
      next: (r) => {
        this.examenes = r;
        if (r.length) {
          this.seleccionarId(this.examen?.id ?? r[0].id);
        } else {
          this.examen = null;
        }
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  seleccionar(e: ExamenPeriodontalResumen): void {
    this.seleccionarId(e.id);
  }

  private seleccionarId(id: number): void {
    this.error = '';
    this.api.get<ExamenPeriodontal>(`/historia-clinica/periodontogramas/${id}`).subscribe({
      next: (r) => {
        this.examen = r;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  // ------------------------------------------------------------------
  // Exámenes
  // ------------------------------------------------------------------

  nuevoExamen(): void {
    if (!this.escribir) return;
    this.editandoExamen = false;
    this.formExamen = { fecha: this.hoy(), observaciones: '', copiarUltima: true };
    this.modalExamen = true;
  }

  editarExamen(): void {
    if (!this.escribir || !this.examen) return;
    this.editandoExamen = true;
    this.formExamen = {
      fecha: this.examen.fecha,
      observaciones: this.examen.observaciones ?? '',
      copiarUltima: true,
    };
    this.modalExamen = true;
  }

  guardarExamen(): void {
    if (this.editandoExamen) {
      this.api
        .put<ExamenPeriodontal>(`/historia-clinica/periodontogramas/${this.examen!.id}`, {
          fecha: this.formExamen.fecha || null,
          observaciones: this.formExamen.observaciones.trim() || null,
        })
        .subscribe({
          next: (r) => {
            this.modalExamen = false;
            this.ok('Examen actualizado');
            this.examen = r;
            this.cargar();
          },
          error: (e) => {
            this.error = this.msg(e);
            this.cdr.markForCheck();
          },
        });
      return;
    }
    this.api
      .post<ExamenPeriodontal>(`/historia-clinica/pacientes/${this.pacienteId}/periodontogramas`, {
        fecha: this.formExamen.fecha || null,
        observaciones: this.formExamen.observaciones.trim() || null,
        copiarUltima: this.formExamen.copiarUltima,
      })
      .subscribe({
        next: (r) => {
          this.modalExamen = false;
          this.ok(`Examen v${r.version} creado`);
          this.examen = r;
          this.cargar();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  // ------------------------------------------------------------------
  // Mediciones por pieza
  // ------------------------------------------------------------------

  editarMediciones(fila: FilaPeriodontal): void {
    if (!this.escribir) return;
    const f = formMedicionesVacio(fila.diente);
    for (const s of SITIOS) {
      const m = fila.mediciones[s.id];
      if (!m) continue;
      f.sitios[s.id] = {
        sondajeMm: m.sondajeMm,
        margenMm: m.margenMm,
        insercionMm: m.insercionMm,
        sangrado: m.sangrado,
        placa: m.placa,
        supuracion: m.supuracion,
      };
    }
    if (fila.pieza) {
      f.movilidad = fila.pieza.movilidad == null ? '' : String(fila.pieza.movilidad);
      f.furcacion = fila.pieza.furcacion == null ? '' : String(fila.pieza.furcacion);
      f.notas = fila.pieza.notas ?? '';
    }
    this.formMed = f;
    this.modalMediciones = true;
  }

  get errorMediciones(): string {
    const f = this.formMed;
    if (!esFdiValido(f.diente)) return `El número FDI ${f.diente} no corresponde a una pieza válida`;
    for (const s of SITIOS) {
      const v = f.sitios[s.id];
      if (v.sondajeMm != null && (v.sondajeMm < 0 || v.sondajeMm > 20))
        return `Sondaje fuera de rango (0 a 20) en ${NOMBRES_SITIO[s.id]}`;
      if (v.margenMm != null && (v.margenMm < -5 || v.margenMm > 10))
        return `Margen fuera de rango (-5 a 10) en ${NOMBRES_SITIO[s.id]}`;
      if (v.insercionMm != null && (v.insercionMm < 0 || v.insercionMm > 20))
        return `Inserción fuera de rango (0 a 20) en ${NOMBRES_SITIO[s.id]}`;
    }
    return '';
  }

  guardarMediciones(): void {
    if (!this.examen || this.errorMediciones) return;
    const f = this.formMed;
    const sitios = SITIOS.map((s) => ({
      diente: f.diente,
      sitio: s.id,
      ...f.sitios[s.id],
    }));
    const pieza = {
      diente: f.diente,
      movilidad: f.movilidad === '' ? null : Number(f.movilidad),
      furcacion: f.furcacion === '' ? null : Number(f.furcacion),
      notas: f.notas.trim() || null,
    };
    this.api
      .put<ExamenPeriodontal>(`/historia-clinica/periodontogramas/${this.examen.id}/mediciones`, {
        sitios,
        dientes: [pieza],
      })
      .subscribe({
        next: (r) => {
          this.modalMediciones = false;
          this.ok(`Pieza ${f.diente} guardada`);
          this.examen = r;
          this.cargar();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  async eliminarPieza(fila: FilaPeriodontal): Promise<void> {
    if (!this.examen) return;
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Eliminar mediciones de la pieza',
        mensaje: `¿Eliminar todas las mediciones de la pieza ${fila.diente}?`,
        confirmarTexto: 'Eliminar',
        peligro: true,
      }))
    )
      return;
    this.api
      .del<void>(`/historia-clinica/periodontogramas/${this.examen.id}/dientes/${fila.diente}`)
      .subscribe({
        next: () => {
          this.ok(`Pieza ${fila.diente} eliminada`);
          this.seleccionarId(this.examen!.id);
          this.cargar();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  // ------------------------------------------------------------------
  // Comparación y PDF
  // ------------------------------------------------------------------

  abrirComparacion(): void {
    if (this.examenes.length < 2) return;
    this.hasta = this.examenes[0].version;
    this.desde = this.examenes[1].version;
    this.comparacion = null;
    this.error = '';
    this.modalComparar = true;
  }

  comparar(): void {
    if (!this.desde || !this.hasta) return;
    this.error = '';
    this.api
      .get<ComparacionPeriodontal>(
        `/historia-clinica/pacientes/${this.pacienteId}/periodontograma/comparar?desde=${this.desde}&hasta=${this.hasta}`,
      )
      .subscribe({
        next: (r) => {
          this.comparacion = r;
          this.cdr.markForCheck();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  descargarPdf(): void {
    if (!this.examen) return;
    this.error = '';
    this.api.getBlob(`/historia-clinica/periodontogramas/${this.examen.id}/pdf`).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `periodontograma-v${this.examen!.version}.pdf`;
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

  trackByDiente(_: number, item: { diente: number }): number {
    return item.diente;
  }

  close(e: MouseEvent): void {
    if (e.target !== e.currentTarget) return;
    this.cerrarTodos();
  }

  stop(e: MouseEvent): void {
    e.stopPropagation();
  }

  cerrarTodos(): void {
    this.modalExamen = false;
    this.modalMediciones = false;
    this.modalComparar = false;
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
