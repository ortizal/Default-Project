import { Component, OnInit, ChangeDetectorRef, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { AppDateComponent } from '../core/app-date.component';
import { withLoading } from '../core/loading';
import {
  Antecedente,
  DiagnosticoClinico,
  EstadoAntecedente,
  EstadoDiagnostico,
  Evolucion,
  ExamenClinico,
  HistoriaResumen,
  Odontologo,
  SeveridadDiagnostico,
  TimelineEvento,
  TipoAntecedente,
  TipoTimeline,
  nombreEstado,
} from '../core/models';
import { OdontogramaComponent } from './odontograma';
import { PeriodonciaComponent } from './periodoncia';
import { RecetasComponent } from './recetas';
import { ConsentimientosComponent } from './consentimientos';
import { ArchivosComponent } from './archivos';
import { PlanesComponent } from './planes';
import { UiBadgeComponent, UiBadgeTone, UiButtonComponent, UiConfirmService, UiPageHeaderComponent } from '../ui';

export type TabHistoria =
  | 'resumen'
  | 'antecedentes'
  | 'examen'
  | 'odontograma'
  | 'periodoncia'
  | 'recetas'
  | 'consentimientos'
  | 'archivos'
  | 'diagnosticos'
  | 'evoluciones'
  | 'timeline'
  | 'planes';

export const TABS_HISTORIA: { id: TabHistoria; label: string }[] = [
  { id: 'resumen', label: 'Resumen' },
  { id: 'antecedentes', label: 'Antecedentes' },
  { id: 'examen', label: 'Examen' },
  { id: 'odontograma', label: 'Odontograma' },
  { id: 'periodoncia', label: 'Periodoncia' },
  { id: 'recetas', label: 'Recetas' },
  { id: 'consentimientos', label: 'Consentimientos' },
  { id: 'archivos', label: 'Archivos' },
  { id: 'diagnosticos', label: 'Diagnósticos' },
  { id: 'evoluciones', label: 'Evoluciones' },
  { id: 'timeline', label: 'Línea de tiempo' },
  { id: 'planes', label: 'Planes' },
];

export const TIPOS_ANTECEDENTE: { id: TipoAntecedente; label: string }[] = [
  { id: 'ENFERMEDAD', label: 'Enfermedad' },
  { id: 'CIRUGIA', label: 'Cirugía' },
  { id: 'HOSPITALIZACION', label: 'Hospitalización' },
  { id: 'ALERGIA', label: 'Alergia' },
  { id: 'MEDICACION', label: 'Medicación' },
  { id: 'ANTECEDENTE_ODONTOLOGICO', label: 'Antecedente odontológico' },
  { id: 'HABITO', label: 'Hábito' },
];

/** Antecedentes agrupados por tipo, en el orden canónico del formulario. */
export function agruparAntecedentes(
  antecedentes: Antecedente[],
): { tipo: TipoAntecedente; label: string; items: Antecedente[] }[] {
  return TIPOS_ANTECEDENTE.map((t) => ({
    tipo: t.id,
    label: t.label,
    items: antecedentes.filter((a) => a.tipo === t.id),
  })).filter((g) => g.items.length > 0);
}

export function nombrePaciente(p?: { nombres: string; apellidos: string } | null): string {
  if (!p) return '';
  return `${p.nombres} ${p.apellidos}`.trim();
}

/** Una evolución sólo es editable mientras esté abierta. */
export function editableEvolucion(e: Pick<Evolucion, 'estado'>): boolean {
  return e.estado === 'ABIERTA';
}

/** La línea temporal llega ordenada del backend; aquí se re-ordena por seguridad. */
export function ordenarTimeline(eventos: TimelineEvento[]): TimelineEvento[] {
  return [...eventos].sort((a, b) => (a.fecha < b.fecha ? 1 : a.fecha > b.fecha ? -1 : 0));
}

/** Etiqueta legible de cada tipo de evento (plan §4.16). */
export const ETIQUETAS_TIMELINE: Record<TipoTimeline, string> = {
  CITA: 'Consulta',
  EXAMEN: 'Examen',
  DIAGNOSTICO: 'Diagnóstico',
  EVOLUCION: 'Evolución',
  ODONTOGRAMA: 'Odontograma',
  PERIODONCIA: 'Periodoncia',
  PLAN: 'Plan',
  PROCEDIMIENTO: 'Procedimiento',
  RECETA: 'Receta',
  ARCHIVO: 'Archivo',
  CONSENTIMIENTO: 'Consentimiento',
};

/** Tono del badge por tipo; el color nunca va solo, siempre con la etiqueta. */
export const TONES_TIMELINE: Record<TipoTimeline, UiBadgeTone> = {
  CITA: 'ok',
  EXAMEN: 'info',
  DIAGNOSTICO: 'warn',
  EVOLUCION: 'primary',
  ODONTOGRAMA: 'accent',
  PERIODONCIA: 'info',
  PLAN: 'primary',
  PROCEDIMIENTO: 'accent',
  RECETA: 'warn',
  ARCHIVO: 'muted',
  CONSENTIMIENTO: 'muted',
};

export function etiquetaEventoTimeline(tipo: TipoTimeline): string {
  return ETIQUETAS_TIMELINE[tipo] ?? tipo;
}

export function toneEventoTimeline(tipo: TipoTimeline): UiBadgeTone {
  return TONES_TIMELINE[tipo] ?? 'muted';
}

/** Tipos realmente presentes, en el orden en que conviene mostrarlos. */
export function tiposTimelinePresentes(eventos: TimelineEvento[]): TipoTimeline[] {
  const presentes = new Set(eventos.map((e) => e.tipo));
  return (Object.keys(ETIQUETAS_TIMELINE) as TipoTimeline[]).filter((t) => presentes.has(t));
}

/** Severidad y estado legibles para listas y alertas. */
export function etiquetaDiagnostico(d: Pick<DiagnosticoClinico, 'severidad' | 'estado'>): string {
  return `${nombreEstado(d.severidad)} · ${nombreEstado(d.estado)}`;
}

import { MatMenuModule } from '@angular/material/menu';

import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-historia-clinica',
  templateUrl: './historia-clinica.html',
  styleUrl: './historia-clinica.css',
  imports: [
    MatIconModule,
    MatMenuModule,
    CommonModule,
    FormsModule,
    AppDateComponent,
    UiPageHeaderComponent,
    UiButtonComponent,
    UiBadgeComponent,
    OdontogramaComponent,
    PeriodonciaComponent,
    RecetasComponent,
    ConsentimientosComponent,
    ArchivosComponent,
    PlanesComponent,
  ],
})
export class HistoriaClinicaComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly confirmacion = inject(UiConfirmService);

  readonly etiquetaEvento = etiquetaEventoTimeline;
  readonly tonoEvento = toneEventoTimeline;

  readonly tiposAntecedente = TIPOS_ANTECEDENTE;
  readonly estadosAntecedente: EstadoAntecedente[] = ['ACTIVO', 'CONTROLADO', 'RESUELTO', 'INACTIVO'];
  readonly severidades: SeveridadDiagnostico[] = ['LEVE', 'MODERADA', 'SEVERA'];
  readonly estadosDiagnostico: EstadoDiagnostico[] = ['SOSPECHA', 'CONFIRMADO', 'RESUELTO', 'DESCARTADO'];

  pacienteId = 0;
  tab: TabHistoria = 'resumen';
  resumen: HistoriaResumen | null = null;
  antecedentes: Antecedente[] = [];
  examenes: ExamenClinico[] = [];
  diagnosticos: DiagnosticoClinico[] = [];
  evoluciones: Evolucion[] = [];
  timeline: TimelineEvento[] = [];
  filtroTimeline: TipoTimeline | 'TODOS' = 'TODOS';
  odontologos: Odontologo[] = [];

  cargando = false;
  error = '';
  exito = '';

  puedeEscribir = false;
  puedeCerrar = false;
  puedeReportes = false;

  modalAntecedente = false;
  formAntecedente: Partial<Antecedente> = {};
  modalExamen = false;
  formExamen: Partial<ExamenClinico> = {};
  modalDiagnostico = false;
  formDiagnostico: Partial<DiagnosticoClinico> = {};
  modalEvolucion = false;
  formEvolucion: Partial<Evolucion> = {};
  modalAdenda = false;
  adendaTexto = '';
  private adendaEvolucionId = 0;

  editandoAntecedente: number | null = null;
  editandoExamen: number | null = null;
  editandoDiagnostico: number | null = null;
  editandoEvolucion: number | null = null;
  private cargados = new Set<TabHistoria>();

  ngOnInit(): void {
    this.pacienteId = Number(this.route.snapshot.paramMap.get('id'));
    this.puedeEscribir = this.auth.puede('HISTORIA_CLINICA_WRITE');
    this.puedeCerrar = this.auth.puede('HISTORIA_CLINICA_CLOSE');
    this.puedeReportes = this.auth.puede('REPORTES_CLINICOS');
    this.cargarResumen();
    if (this.puedeEscribir) this.cargarOdontologos();
  }

  /** Odontograma y planes sólo se muestran si el rol tiene su permiso de lectura. */
  get tabs(): { id: TabHistoria; label: string }[] {
    return TABS_HISTORIA.filter((t) => {
      if (t.id === 'odontograma') return this.auth.puede('ODONTOGRAMA_READ');
      if (t.id === 'periodoncia') return this.auth.puede('PERIODONCIA_READ');
      if (t.id === 'recetas') return this.auth.puede('RECETAS_READ');
      if (t.id === 'consentimientos') return this.auth.puede('CONSENTIMIENTOS_READ');
      if (t.id === 'archivos') return this.auth.puede('ARCHIVOS_CLINICOS_READ');
      if (t.id === 'planes') return this.auth.puede('PLAN_TRATAMIENTO_READ');
      return true;
    });
  }

  get tituloPaciente(): string {
    return nombrePaciente(this.resumen?.paciente);
  }

  get tiposTimeline(): TipoTimeline[] {
    return tiposTimelinePresentes(this.timeline);
  }

  get timelineFiltrada(): TimelineEvento[] {
    if (this.filtroTimeline === 'TODOS') {
      return this.timeline;
    }
    const filtro = this.filtroTimeline;
    return this.timeline.filter((e) => e.tipo === filtro);
  }

  seleccionarFiltroTimeline(tipo: TipoTimeline | 'TODOS'): void {
    this.filtroTimeline = tipo;
  }

  conteoTipoTimeline(tipo: TipoTimeline): number {
    return this.timeline.filter((e) => e.tipo === tipo).length;
  }

  // ------------------------------------------------------------------
  // Cargas
  // ------------------------------------------------------------------

  cargarResumen(): void {
    this.error = '';
    withLoading(this, this.api.get<HistoriaResumen>(`/historia-clinica/pacientes/${this.pacienteId}`), undefined, this.cdr).subscribe({
      next: (r) => {
        this.resumen = r;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  descargarHistoriaCompleta(): void {
    this.error = '';
    this.api
      .getBlob(`/historia-clinica/pacientes/${this.pacienteId}/reportes/historia-completa.pdf`)
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `historia-clinica-completa-${this.pacienteId}.pdf`;
          a.click();
          URL.revokeObjectURL(url);
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  seleccionar(tab: TabHistoria): void {
    this.tab = tab;
    if (this.cargados.has(tab)) return;
    switch (tab) {
      case 'antecedentes':
        this.cargarAntecedentes();
        break;
      case 'examen':
        this.cargarExamenes();
        break;
      case 'diagnosticos':
        this.cargarDiagnosticos();
        break;
      case 'evoluciones':
        this.cargarEvoluciones();
        break;
      case 'timeline':
        this.cargarTimeline();
        break;
      case 'periodoncia':
      case 'recetas':
      case 'consentimientos':
      case 'planes':
        break;
      default:
        this.cargarResumen();
    }
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

  cargarAntecedentes(): void {
    this.peticion<Antecedente[]>(`/historia-clinica/pacientes/${this.pacienteId}/antecedentes`, (r) => {
      this.antecedentes = r;
      this.cargados.add('antecedentes');
    });
  }

  cargarExamenes(): void {
    this.peticion<ExamenClinico[]>(`/historia-clinica/pacientes/${this.pacienteId}/examenes`, (r) => {
      this.examenes = r;
      this.cargados.add('examen');
    });
  }

  cargarDiagnosticos(): void {
    this.peticion<DiagnosticoClinico[]>(`/historia-clinica/pacientes/${this.pacienteId}/diagnosticos`, (r) => {
      this.diagnosticos = r;
      this.cargados.add('diagnosticos');
    });
  }

  cargarEvoluciones(): void {
    this.peticion<Evolucion[]>(`/historia-clinica/pacientes/${this.pacienteId}/evoluciones`, (r) => {
      this.evoluciones = r;
      this.cargados.add('evoluciones');
    });
  }

  cargarTimeline(): void {
    this.peticion<TimelineEvento[]>(`/historia-clinica/pacientes/${this.pacienteId}/timeline`, (r) => {
      this.timeline = ordenarTimeline(r);
      this.cargados.add('timeline');
    });
  }

  private peticion<T>(ruta: string, ok: (r: T) => void): void {
    this.error = '';
    withLoading(this, this.api.get<T>(ruta), undefined, this.cdr).subscribe({
      next: (r) => {
        ok(r);
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  // ------------------------------------------------------------------
  // Antecedentes
  // ------------------------------------------------------------------

  nuevoAntecedente(): void {
    this.editandoAntecedente = null;
    this.formAntecedente = { tipo: 'ENFERMEDAD', estado: 'ACTIVO' };
    this.modalAntecedente = true;
  }

  editarAntecedente(a: Antecedente): void {
    this.editandoAntecedente = a.id ?? null;
    this.formAntecedente = { ...a };
    this.modalAntecedente = true;
  }

  guardarAntecedente(): void {
    const id = this.editandoAntecedente;
    const ruta = id
      ? `/historia-clinica/antecedentes/${id}`
      : `/historia-clinica/pacientes/${this.pacienteId}/antecedentes`;
    const peticion$ = id ? this.api.put<Antecedente>(ruta, this.formAntecedente) : this.api.post<Antecedente>(ruta, this.formAntecedente);
    peticion$.subscribe({
      next: () => {
        this.modalAntecedente = false;
        this.ok('Antecedente guardado');
        this.cargarAntecedentes();
        this.cargarResumen();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  async eliminarAntecedente(a: Antecedente): Promise<void> {
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Eliminar antecedente',
        mensaje: `¿Eliminar "${a.nombre}" de los antecedentes?`,
        confirmarTexto: 'Eliminar',
      }))
    )
      return;
    this.api.del<void>(`/historia-clinica/antecedentes/${a.id}`).subscribe({
      next: () => {
        this.cargarAntecedentes();
        this.cargarResumen();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  get antecedentesAgrupados(): { tipo: TipoAntecedente; label: string; items: Antecedente[] }[] {
    return agruparAntecedentes(this.antecedentes);
  }

  // ------------------------------------------------------------------
  // Exámenes
  // ------------------------------------------------------------------

  nuevoExamen(): void {
    this.editandoExamen = null;
    this.formExamen = { fecha: this.hoy() };
    this.modalExamen = true;
  }

  editarExamen(e: ExamenClinico): void {
    this.editandoExamen = e.id ?? null;
    this.formExamen = { ...e };
    this.modalExamen = true;
  }

  guardarExamen(): void {
    const id = this.editandoExamen;
    const ruta = id
      ? `/historia-clinica/examenes/${id}`
      : `/historia-clinica/pacientes/${this.pacienteId}/examenes`;
    const peticion$ = id ? this.api.put<ExamenClinico>(ruta, this.formExamen) : this.api.post<ExamenClinico>(ruta, this.formExamen);
    peticion$.subscribe({
      next: () => {
        this.modalExamen = false;
        this.ok('Examen guardado');
        this.cargarExamenes();
        this.cargarResumen();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  // ------------------------------------------------------------------
  // Diagnósticos
  // ------------------------------------------------------------------

  nuevoDiagnostico(): void {
    this.editandoDiagnostico = null;
    this.formDiagnostico = { severidad: 'MODERADA', estado: 'CONFIRMADO', fecha: this.hoy() };
    this.modalDiagnostico = true;
  }

  editarDiagnostico(d: DiagnosticoClinico): void {
    this.editandoDiagnostico = d.id ?? null;
    this.formDiagnostico = { ...d };
    this.modalDiagnostico = true;
  }

  guardarDiagnostico(): void {
    const id = this.editandoDiagnostico;
    const ruta = id
      ? `/historia-clinica/diagnosticos/${id}`
      : `/historia-clinica/pacientes/${this.pacienteId}/diagnosticos`;
    const peticion$ = id
      ? this.api.put<DiagnosticoClinico>(ruta, this.formDiagnostico)
      : this.api.post<DiagnosticoClinico>(ruta, this.formDiagnostico);
    peticion$.subscribe({
      next: () => {
        this.modalDiagnostico = false;
        this.ok('Diagnóstico guardado');
        this.cargarDiagnosticos();
        this.cargarResumen();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  cambiarEstadoDiagnostico(d: DiagnosticoClinico): void {
    this.api.put<DiagnosticoClinico>(`/historia-clinica/diagnosticos/${d.id}`, d).subscribe({
      next: () => {
        this.ok('Diagnóstico actualizado');
        this.cargarResumen();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  // ------------------------------------------------------------------
  // Evoluciones
  // ------------------------------------------------------------------

  nuevaEvolucion(): void {
    this.editandoEvolucion = null;
    this.formEvolucion = { motivo: '' };
    this.modalEvolucion = true;
  }

  editarEvolucion(e: Evolucion): void {
    if (!editableEvolucion(e)) return;
    this.editandoEvolucion = e.id ?? null;
    this.formEvolucion = { ...e };
    this.modalEvolucion = true;
  }

  guardarEvolucion(): void {
    const id = this.editandoEvolucion;
    const ruta = id
      ? `/historia-clinica/evoluciones/${id}`
      : `/historia-clinica/pacientes/${this.pacienteId}/evoluciones`;
    const peticion$ = id ? this.api.put<Evolucion>(ruta, this.formEvolucion) : this.api.post<Evolucion>(ruta, this.formEvolucion);
    peticion$.subscribe({
      next: () => {
        this.modalEvolucion = false;
        this.ok('Evolución guardada');
        this.cargarEvoluciones();
        this.cargarResumen();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  async cerrarEvolucion(e: Evolucion): Promise<void> {
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Cerrar evolución',
        mensaje: 'Al cerrarla la nota queda inmutable: las correcciones posteriores se hacen por adenda.',
        confirmarTexto: 'Cerrar evolución',
        peligro: false,
      }))
    )
      return;
    this.api.post<Evolucion>(`/historia-clinica/evoluciones/${e.id}/cerrar`, {}).subscribe({
      next: () => {
        this.ok('Evolución cerrada');
        this.cargarEvoluciones();
      },
      error: (err) => {
        this.error = this.msg(err);
        this.cdr.markForCheck();
      },
    });
  }

  abrirAdenda(e: Evolucion): void {
    this.adendaEvolucionId = e.id ?? 0;
    this.adendaTexto = '';
    this.modalAdenda = true;
  }

  guardarAdenda(): void {
    this.api
      .post(`/historia-clinica/evoluciones/${this.adendaEvolucionId}/adendas`, { texto: this.adendaTexto })
      .subscribe({
        next: () => {
          this.modalAdenda = false;
          this.ok('Adenda registrada');
          this.cargarEvoluciones();
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

  esEditable(e: Evolucion): boolean {
    return editableEvolucion(e);
  }

  trackByTab(_: number, tab: { id: string }): string {
    return tab.id;
  }

  close(e: MouseEvent): void {
    if (e.target !== e.currentTarget) return;
    this.cerrarTodos();
  }

  stop(e: MouseEvent): void {
    e.stopPropagation();
  }

  cerrarTodos(): void {
    this.modalAntecedente = false;
    this.modalExamen = false;
    this.modalDiagnostico = false;
    this.modalEvolucion = false;
    this.modalAdenda = false;
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrarTodos();
  }

  /** Vuelve al listado de pacientes (cabecera). */
  volver(): void {
    void this.router.navigate(['/pacientes']);
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
