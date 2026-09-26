import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { DIAS, ExcepcionHorario, Horario, nombreEstado, Odontologo } from '../core/models';
import { withLoading } from '../core/loading';
import { UiPageHeaderComponent, UiTableComponent } from '../ui';

@Component({
  selector: 'app-horarios',
  templateUrl: './horarios.html',
  imports: [CommonModule, FormsModule, UiPageHeaderComponent, UiTableComponent],
})
export class HorariosComponent implements OnInit {
  readonly DIAS = DIAS;
  readonly nombreEstado = nombreEstado;
  odontologos: Odontologo[] = [];
  odontologoSeleccionado = '';
  items: Horario[] = [];
  excepciones: ExcepcionHorario[] = [];
  error = '';
  aviso = '';
  cargando = false;
  showForm = false;
  form: Partial<Horario> = {};
  showExcepcion = false;
  excForm: Partial<ExcepcionHorario> = {};

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.api.get<Odontologo[]>('/odontologos/activos').subscribe({
      next: (r) => {
        this.odontologos = r;
        if (r.length) {
          this.odontologoSeleccionado = String(r[0].id);
          this.cargar();
        }
        this.cdr.markForCheck();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  cargar(): void {
    if (!this.odontologoSeleccionado) return;
    withLoading(this, this.api.get<Horario[]>(`/horarios?odontologoId=${this.odontologoSeleccionado}`), undefined, this.cdr).subscribe({
      next: (r) => { this.items = r; this.cdr.markForCheck(); },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
    this.cargarExcepciones();
  }

  private cargarExcepciones(): void {
    if (!this.odontologoSeleccionado) return;
    const hoy = new Date().toISOString().slice(0, 10);
    const hasta = new Date(Date.now() + 180 * 86400000).toISOString().slice(0, 10);
    this.api.get<ExcepcionHorario[]>(
      `/agenda/excepciones?odontologoId=${this.odontologoSeleccionado}&desde=${hoy}&hasta=${hasta}`
    ).subscribe({
      next: (r) => { this.excepciones = r; this.cdr.markForCheck(); },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

nuevo(): void {
    this.form = { diaSemana: 1, horaInicio: '08:00', horaFin: '17:00', intervaloMinutos: 30, estado: 'ACTIVO' };
    this.showForm = true;
    this.cdr.markForCheck();
  }

  editar(h: Horario): void {
    this.form = { ...h };
    this.showForm = true;
    this.cdr.markForCheck();
  }
  guardar(): void {
    if (!this.form.diaSemana || !this.form.horaInicio || !this.form.horaFin) {
      this.error = 'Completa día, hora de inicio y hora de fin';
      this.cdr.markForCheck();
      return;
    }
    const body = { ...this.form, odontologoId: Number(this.odontologoSeleccionado), estado: this.form.estado || 'ACTIVO' };
    const req = this.form.id
      ? this.api.put<Horario>(`/horarios/${this.form.id}`, body)
      : this.api.post<Horario>('/horarios', body);
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cdr.markForCheck();
        this.cargar();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  eliminar(h: Horario): void {
    if (!confirm('¿Eliminar este horario?')) return;
    this.api.del<void>(`/horarios/${h.id}`).subscribe({
      next: () => this.cargar(),
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  // ------------------------------------------------------------------
  // Días de excepción de atención
  // ------------------------------------------------------------------

  nuevaExcepcion(): void {
    this.aviso = '';
    this.excForm = { fecha: '', tipo: 'CERRADO', horaInicio: '09:00', horaFin: '13:00', motivo: '' };
    this.showExcepcion = true;
    this.cdr.markForCheck();
  }

  esCerrada(): boolean {
    return this.excForm.tipo === 'CERRADO';
  }

  guardarExcepcion(): void {
    this.error = '';
    this.aviso = '';
    if (!this.excForm.fecha) {
      this.error = 'Selecciona la fecha de la excepción';
      this.cdr.markForCheck();
      return;
    }
    const cerrada = this.esCerrada();
    if (!cerrada && (!this.excForm.horaInicio || !this.excForm.horaFin)) {
      this.error = 'El horario especial necesita hora de inicio y de fin';
      this.cdr.markForCheck();
      return;
    }
    const body = {
      odontologoId: Number(this.odontologoSeleccionado),
      fecha: this.excForm.fecha,
      tipo: this.excForm.tipo,
      horaInicio: cerrada ? null : this.excForm.horaInicio,
      horaFin: cerrada ? null : this.excForm.horaFin,
      motivo: this.excForm.motivo || null,
    };
    this.api.post<ExcepcionHorario>('/agenda/excepciones', body).subscribe({
      next: (r) => {
        this.showExcepcion = false;
        this.aviso = r.citasCanceladas
          ? `Excepción guardada. Se cancelaron ${r.citasCanceladas} cita(s) y se avisó a sus pacientes.`
          : 'Excepción guardada.';
        this.cdr.markForCheck();
        this.cargarExcepciones();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  eliminarExcepcion(e: ExcepcionHorario): void {
    const detalle = e.tipo === 'CERRADO'
      ? `¿Eliminar la excepción del ${e.fecha} (día cerrado)?`
      : `¿Eliminar la excepción del ${e.fecha} (horario especial)?`;
    if (!confirm(detalle)) return;
    this.api.del<void>(`/agenda/excepciones/${e.id}`).subscribe({
      next: () => this.cargarExcepciones(),
      error: (err) => { this.error = this.msg(err); this.cdr.markForCheck(); },
    });
  }

  rangoExcepcion(e: ExcepcionHorario): string {
    return e.tipo === 'CERRADO' ? 'Todo el día' : `${e.horaInicio} – ${e.horaFin}`;
  }

  close(e: MouseEvent): void {
    if (e.target === e.currentTarget) { this.showForm = false; this.showExcepcion = false; }
  }

  stop(e: MouseEvent): void {
    e.stopPropagation();
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}
