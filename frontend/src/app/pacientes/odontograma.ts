import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, HostListener, Input, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpParams } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { withLoading } from '../core/loading';
import {
  CambioOdontograma,
  ComparacionOdontograma,
  Denticion,
  DienteOdontograma,
  EstadoOdontograma,
  EventoOdontograma,
  Odontograma,
  OdontogramaResumen,
  SuperficieDental,
} from '../core/models';
import { UiBadgeComponent, UiButtonComponent, UiBadgeTone, UiConfirmService } from '../ui';

export const DIENTES_PERMANENTE: Record<'superior' | 'inferior', number[]> = {
  superior: [18, 17, 16, 15, 14, 13, 12, 11, 21, 22, 23, 24, 25, 26, 27, 28],
  inferior: [48, 47, 46, 45, 44, 43, 42, 41, 31, 32, 33, 34, 35, 36, 37, 38],
};

export const DIENTES_TEMPORAL: Record<'superior' | 'inferior', number[]> = {
  superior: [55, 54, 53, 52, 51, 61, 62, 63, 64, 65],
  inferior: [85, 84, 83, 82, 81, 71, 72, 73, 74, 75],
};

export const CONDICIONES_ODONTOGRAMA = [
  'CARIES',
  'RESTAURACION',
  'ENDODONCIA',
  'CORONA',
  'PROTESIS_FIJA',
  'PROTESIS_MOVIL',
  'IMPLANTE',
  'AUSENTE',
  'EXTRAER',
  'FRACTURA',
  'IMPACTADO',
  'MOVILIDAD',
  'RECESION',
  'GINGIVITIS',
];

export const ESTADOS_ODONTOGRAMA: {
  id: EstadoOdontograma;
  label: string;
  color: string;
}[] = [
  { id: 'EXISTENTE', label: 'Existente', color: '#94a3b8' },
  { id: 'DIAGNOSTICO', label: 'Diagnóstico', color: '#ef4444' },
  { id: 'PLANIFICADO', label: 'Planificado', color: '#f59e0b' },
  { id: 'PROCESO', label: 'En proceso', color: '#3b82f6' },
  { id: 'COMPLETADO', label: 'Completado', color: '#22c55e' },
  { id: 'REFERIDO', label: 'Referido', color: '#a855f7' },
];

const PRIORIDAD: Record<EstadoOdontograma, number> = {
  COMPLETADO: 6,
  PROCESO: 5,
  PLANIFICADO: 4,
  DIAGNOSTICO: 3,
  REFERIDO: 2,
  EXISTENTE: 1,
};

export type Region = 'arriba' | 'abajo' | 'izq' | 'der' | 'centro';

export interface FilaOdontograma {
  etiqueta: string;
  arco: 'superior' | 'inferior';
  numeros: number[];
}

/** Filas a dibujar según la dentición seleccionada. */
export function filasDe(modo: Denticion): FilaOdontograma[] {
  const permanente: FilaOdontograma[] = [
    { etiqueta: 'Permanente superior', arco: 'superior', numeros: DIENTES_PERMANENTE.superior },
    { etiqueta: 'Permanente inferior', arco: 'inferior', numeros: DIENTES_PERMANENTE.inferior },
  ];
  const temporal: FilaOdontograma[] = [
    { etiqueta: 'Temporal superior', arco: 'superior', numeros: DIENTES_TEMPORAL.superior },
    { etiqueta: 'Temporal inferior', arco: 'inferior', numeros: DIENTES_TEMPORAL.inferior },
  ];
  if (modo === 'TEMPORAL') return temporal;
  if (modo === 'MIXTA') return [permanente[0], temporal[0], permanente[1], temporal[1]];
  return permanente;
}

/** Piezas anteriores (incisivo/canino) llevan incisal en vez de oclusal. */
export function esAnterior(diente: number): boolean {
  return diente % 10 <= 3;
}

export function superficieCentro(diente: number): SuperficieDental {
  return esAnterior(diente) ? 'INCISAL' : 'OCLUSAL';
}

/** Regiones de la pieza: los polígonos del SVG (caja 36×36). */
export function poligonosTooth(size = 36): Record<Region, string> {
  const a = Math.round(size / 3);
  const b = size - a;
  return {
    arriba: `0,0 ${size},0 ${b},${a} ${a},${a}`,
    abajo: `${a},${b} ${b},${b} ${size},${size} 0,${size}`,
    izq: `0,0 ${a},${a} ${a},${b} 0,${size}`,
    der: `${b},${a} ${size},0 ${size},${size} ${b},${b}`,
    centro: `${a},${a} ${b},${a} ${b},${b} ${a},${b}`,
  };
}

export function regionDe(region: Region, diente: number): SuperficieDental | null {
  switch (region) {
    case 'arriba':
      return 'VESTIBULAR';
    case 'abajo':
      return 'LINGUAL';
    case 'izq':
      return 'MESIAL';
    case 'der':
      return 'DISTAL';
    default:
      return superficieCentro(diente);
  }
}

export function porDiente(dientes: DienteOdontograma[]): Record<number, DienteOdontograma[]> {
  const mapa: Record<number, DienteOdontograma[]> = {};
  for (const d of dientes) {
    (mapa[d.diente] ??= []).push(d);
  }
  return mapa;
}

export function condicionesDe(
  mapa: Record<number, DienteOdontograma[]>,
  diente: number,
  region: Region,
): DienteOdontograma[] {
  const superficie = regionDe(region, diente);
  return (mapa[diente] ?? []).filter(
    (d) =>
      (region === 'centro' && d.superficie !== null && (d.superficie === 'OCLUSAL' || d.superficie === 'INCISAL')) ||
      (region !== 'centro' && d.superficie === superficie),
  );
}

export function dienteCompleto(mapa: Record<number, DienteOdontograma[]>, diente: number): DienteOdontograma[] {
  return (mapa[diente] ?? []).filter((d) => d.superficie === null);
}

/** El color de una región es el estado más avanzado de sus condiciones. */
export function estadoVisible(condiciones: DienteOdontograma[]): EstadoOdontograma | null {
  let mejor: EstadoOdontograma | null = null;
  for (const c of condiciones) {
    if (!mejor || PRIORIDAD[c.estado] > PRIORIDAD[mejor]) mejor = c.estado;
  }
  return mejor;
}

export function colorEstado(estado: EstadoOdontograma | null): string {
  if (!estado) return '#ffffff';
  return ESTADOS_ODONTOGRAMA.find((e) => e.id === estado)?.color ?? '#94a3b8';
}

export function etiquetaSuperficie(superficie: SuperficieDental | null): string {
  switch (superficie) {
    case 'OCLUSAL':
      return 'Oclusal';
    case 'MESIAL':
      return 'Mesial';
    case 'DISTAL':
      return 'Distal';
    case 'VESTIBULAR':
      return 'Vestibular';
    case 'LINGUAL':
      return 'Lingual';
    case 'INCISAL':
      return 'Incisal';
    default:
      return 'Diente completo';
  }
}

export function etiquetaEstado(estado: EstadoOdontograma): string {
  return ESTADOS_ODONTOGRAMA.find((e) => e.id === estado)?.label ?? estado;
}

export function etiquetaCondicion(condicion: string): string {
  return condicion
    .toLowerCase()
    .replace(/_/g, ' ')
    .split(' ')
    .map((p) => (p ? p.charAt(0).toUpperCase() + p.slice(1) : p))
    .join(' ');
}

export function etiquetaCambio(cambio: CambioOdontograma): string {
  const superficie = etiquetaSuperficie(cambio.superficie).toLowerCase();
  const base = `${cambio.diente} · ${superficie} · ${etiquetaCondicion(cambio.condicion)}`;
  if (cambio.tipo === 'NUEVO') return `+ ${base} → ${etiquetaEstado(cambio.estadoNuevo as EstadoOdontograma)}`;
  if (cambio.tipo === 'ELIMINADO') return `− ${base} (estaba en ${etiquetaEstado(cambio.estadoAnterior as EstadoOdontograma)})`;
  return `~ ${base}: ${etiquetaEstado(cambio.estadoAnterior as EstadoOdontograma)} → ${etiquetaEstado(cambio.estadoNuevo as EstadoOdontograma)}`;
}

export const PASO = 44;
export const ALTO_PIEZA = 60;

import { MatMenuModule } from '@angular/material/menu';

import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-odontograma',
  templateUrl: './odontograma.html',
  styleUrl: './odontograma.css',
  imports: [
    MatIconModule,
    MatMenuModule,CommonModule, FormsModule, UiButtonComponent, UiBadgeComponent],
})
export class OdontogramaComponent implements OnInit {
  @Input({ required: true }) pacienteId = 0;

  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);
  private readonly confirmacion = inject(UiConfirmService);

  readonly modos: { id: Denticion; label: string }[] = [
    { id: 'PERMANENTE', label: 'Permanente' },
    { id: 'TEMPORAL', label: 'Temporal' },
    { id: 'MIXTA', label: 'Mixta' },
  ];
  readonly condiciones = CONDICIONES_ODONTOGRAMA;
  readonly estados = ESTADOS_ODONTOGRAMA;
  readonly regiones: Region[] = ['arriba', 'izq', 'centro', 'der', 'abajo'];
  readonly poligonos = poligonosTooth();
  readonly superficies: (SuperficieDental | '')[] = ['', 'OCLUSAL', 'MESIAL', 'DISTAL', 'VESTIBULAR', 'LINGUAL', 'INCISAL'];
  readonly PASO = PASO;
  readonly ALTO_PIEZA = ALTO_PIEZA;

  escribir = false;
  cargando = false;
  error = '';
  exito = '';

  modo: Denticion = 'PERMANENTE';
  historial: OdontogramaResumen[] = [];
  odontograma: Odontograma | null = null;
  versionSel = 0;
  eventos: EventoOdontograma[] = [];
  mapa: Record<number, DienteOdontograma[]> = {};

  modal = false;
  editando = false;
  editandoId: number | null = null;
  form: {
    diente: number;
    superficie: SuperficieDental | '';
    condicion: string;
    estado: EstadoOdontograma;
    notas: string;
  } = { diente: 0, superficie: '', condicion: '', estado: 'DIAGNOSTICO', notas: '' };

  cmpDesde = 0;
  cmpHasta = 0;
  comparacion: ComparacionOdontograma | null = null;

  ngOnInit(): void {
    this.escribir = this.auth.puede('ODONTOGRAMA_WRITE');
    this.cargar();
  }

  get filas(): FilaOdontograma[] {
    return filasDe(this.modo);
  }

  get esVigente(): boolean {
    return !!this.odontograma && this.historial.length > 0 && this.odontograma.version === this.historial[0].version;
  }

  get puedeEditar(): boolean {
    return this.escribir && this.esVigente;
  }

  ancho(fila: FilaOdontograma): number {
    return fila.numeros.length * PASO;
  }

  condicionesDe(diente: number, region: Region): DienteOdontograma[] {
    return condicionesDe(this.mapa, diente, region);
  }

  colorDe(diente: number, region: Region): string {
    return colorEstado(estadoVisible(this.condicionesDe(diente, region)));
  }

  completa(diente: number): boolean {
    return dienteCompleto(this.mapa, diente).length > 0;
  }

  etiquetaRegion(diente: number, region: Region): string {
    const lista = this.condicionesDe(diente, region);
    const superficie = etiquetaSuperficie(regionDe(region, diente)).toLowerCase();
    if (!lista.length) return `Diente ${diente}, superficie ${superficie}: sin condiciones`;
    return `Diente ${diente}, superficie ${superficie}: ` + lista.map((d) => etiquetaCondicion(d.condicion)).join(', ');
  }

  abrir(diente: number, region: Region | null): void {
    if (!this.puedeEditar) return;
    const superficie = region === null ? '' : (regionDe(region, diente) as SuperficieDental | '');
    const existente =
      region === null
        ? dienteCompleto(this.mapa, diente)[0]
        : this.condicionesDe(diente, region)[0];
    this.editando = !!existente;
    this.editandoId = existente ? existente.id : null;
    this.form = {
      diente,
      superficie: existente ? (existente.superficie ?? '') : superficie,
      condicion: existente ? existente.condicion : '',
      estado: existente ? existente.estado : 'DIAGNOSTICO',
      notas: existente ? (existente.notas ?? '') : '',
    };
    this.modal = true;
    this.cdr.markForCheck();
  }

  editarDesdeTabla(d: DienteOdontograma): void {
    if (!this.puedeEditar) return;
    this.editando = true;
    this.editandoId = d.id;
    this.form = {
      diente: d.diente,
      superficie: d.superficie ?? '',
      condicion: d.condicion,
      estado: d.estado,
      notas: d.notas ?? '',
    };
    this.modal = true;
    this.cdr.markForCheck();
  }

  nuevaCondicion(): void {
    if (!this.puedeEditar) return;
    this.editando = false;
    this.editandoId = null;
    this.form = { diente: 0, superficie: '', condicion: '', estado: 'DIAGNOSTICO', notas: '' };
    this.modal = true;
    this.cdr.markForCheck();
  }

  guardar(): void {
    if (!this.odontograma || !this.form.condicion.trim()) return;
    this.error = '';
    const body = {
      diente: this.form.diente,
      superficie: this.form.superficie === '' ? null : this.form.superficie,
      condicion: this.form.condicion.trim().toUpperCase().replace(/\s+/g, '_'),
      estado: this.form.estado,
      notas: this.form.notas.trim() || null,
    };
    this.api.post<DienteOdontograma>(`/historia-clinica/odontogramas/${this.odontograma.id}/dientes`, body).subscribe({
      next: () => {
        this.modal = false;
        this.ok('Condición guardada');
        this.recargarSeleccion();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  async eliminar(): Promise<void> {
    if (!this.odontograma || !this.editando || this.editandoId == null) return;
    const existente = Object.values(this.mapa)
      .flat()
      .find((d) => d.id === this.editandoId);
    if (!existente) return;
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Eliminar condición',
        mensaje: `¿Eliminar "${etiquetaCondicion(existente.condicion)}" del diente ${existente.diente}?`,
        confirmarTexto: 'Eliminar',
      }))
    )
      return;
    this.api.del<void>(`/historia-clinica/odontogramas/${this.odontograma.id}/dientes/${existente.id}`).subscribe({
      next: () => {
        this.modal = false;
        this.ok('Condición eliminada');
        this.recargarSeleccion();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  // ------------------------------------------------------------------
  // Cargas
  // ------------------------------------------------------------------

  cargar(): void {
    this.error = '';
    withLoading(
      this,
      forkJoin({
        historial: this.api.get<OdontogramaResumen[]>(`/historia-clinica/pacientes/${this.pacienteId}/odontogramas`),
        vigente: this.api.get<Odontograma | null>(`/historia-clinica/pacientes/${this.pacienteId}/odontograma`),
      }),
      undefined,
      this.cdr,
    ).subscribe({
      next: ({ historial, vigente }) => {
        this.historial = historial;
        this.versionSel = vigente ? vigente.version : 0;
        this.cmpHasta = historial[0]?.version ?? 0;
        this.cmpDesde = historial[1]?.version ?? 0;
        this.seleccionar(vigente);
        this.comparacion = null;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  cargarVersion(): void {
    if (!this.versionSel) return;
    this.error = '';
    withLoading(
      this,
      this.api.get<Odontograma>(`/historia-clinica/pacientes/${this.pacienteId}/odontogramas/${this.versionSel}`),
      undefined,
      this.cdr,
    ).subscribe({
      next: (o) => {
        this.seleccionar(o);
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  private seleccionar(o: Odontograma | null): void {
    this.odontograma = o;
    this.mapa = o ? porDiente(o.dientes) : {};
    if (o) {
      this.modo = o.denticion;
      this.cargarEventos(o.id);
    } else {
      this.eventos = [];
    }
  }

  private recargarSeleccion(): void {
    if (!this.odontograma) return;
    this.api
      .get<Odontograma>(`/historia-clinica/pacientes/${this.pacienteId}/odontogramas/${this.odontograma.version}`)
      .subscribe({
        next: (o) => {
          this.seleccionar(o);
          this.api.get<OdontogramaResumen[]>(`/historia-clinica/pacientes/${this.pacienteId}/odontogramas`).subscribe({
            next: (h) => {
              this.historial = h;
              this.cdr.markForCheck();
            },
            error: () => undefined,
          });
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  private cargarEventos(odontogramaId: number): void {
    this.api.get<EventoOdontograma[]>(`/historia-clinica/odontogramas/${odontogramaId}/eventos`).subscribe({
      next: (e) => {
        this.eventos = e;
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  async nuevaVersion(): Promise<void> {
    if (!this.escribir) return;
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Nueva versión del odontograma',
        mensaje: 'Se copian las condiciones de la versión vigente y a partir de ahí se trabaja sobre la nueva.',
        confirmarTexto: 'Crear versión',
      }))
    )
      return;
    this.error = '';
    this.api
      .post<Odontograma>(`/historia-clinica/pacientes/${this.pacienteId}/odontogramas`, {
        copiarUltima: true,
        denticion: this.modo,
      })
      .subscribe({
        next: (o) => {
          this.ok(`Versión ${o.version} creada`);
          this.versionSel = o.version;
          this.cargar();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  comparar(): void {
    if (!this.cmpDesde || !this.cmpHasta) return;
    this.error = '';
    const params = new HttpParams().set('desde', this.cmpDesde).set('hasta', this.cmpHasta);
    withLoading(
      this,
      this.api.get<ComparacionOdontograma>(`/historia-clinica/pacientes/${this.pacienteId}/odontograma/comparar`, params),
      undefined,
      this.cdr,
    ).subscribe({
      next: (c) => {
        this.comparacion = c;
        this.cdr.markForCheck();
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

  colorEstado(estado: EstadoOdontograma | null): string {
    return colorEstado(estado);
  }

  etiquetaSuperficie(superficie: SuperficieDental | null): string {
    return etiquetaSuperficie(superficie);
  }

  etiquetaEstado(estado: EstadoOdontograma): string {
    return etiquetaEstado(estado);
  }

  etiquetaCondicion(condicion: string): string {
    return etiquetaCondicion(condicion);
  }

  tonoEstado(estado: EstadoOdontograma): UiBadgeTone {
    switch (estado) {
      case 'COMPLETADO':
        return 'ok';
      case 'DIAGNOSTICO':
        return 'danger';
      case 'PLANIFICADO':
        return 'warn';
      case 'PROCESO':
        return 'info';
      case 'REFERIDO':
        return 'primary';
      default:
        return 'muted';
    }
  }

  accionEvento(accion: EventoOdontograma['accion']): string {
    switch (accion) {
      case 'CREAR_VERSION':
        return 'Versión creada';
      case 'AGREGAR':
        return 'Agregado';
      case 'MODIFICAR':
        return 'Modificado';
      default:
        return 'Eliminado';
    }
  }

  etiquetaCambio(cambio: CambioOdontograma): string {
    return etiquetaCambio(cambio);
  }

  cerrarModal(): void {
    this.modal = false;
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.modal = false;
  }

  trackByFila(_: number, fila: FilaOdontograma): string {
    return fila.etiqueta;
  }

  trackByNumero(_: number, numero: number): number {
    return numero;
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
    if (a?.status === 403) return 'No tiene permiso para editar el odontograma';
    return 'Error de conexión';
  }
}
