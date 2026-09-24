import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Bloqueo, Cita, Horario, Odontologo, Paciente, Page, Servicio, Slot } from '../core/models';
import { CalendarioComponent } from './calendario';
import { AppDateComponent } from '../core/app-date.component';
import { withLoading } from '../core/loading';

@Component({
  selector: 'app-agenda',
  templateUrl: './agenda.html',
  imports: [CommonModule, FormsModule, RouterLink, CalendarioComponent, AppDateComponent],
})
export class AgendaComponent implements OnInit {
  vista: 'dia' | 'semana' | 'mes' = 'dia';
  odontologos: Odontologo[] = [];
  odontologoSeleccionado = '';
  servicios: Servicio[] = [];
  servicioSeleccionado = '';
  pacientes: Paciente[] = [];
  citas: Cita[] = [];
  bloqueos: Bloqueo[] = [];
  horarios: Horario[] = [];
  slots: Slot[] = [];
  fecha = new Date().toISOString().slice(0, 10);
  error = '';
  cargando = false;
  showBloqueo = false;
  showCita = false;
  horaSugerida = '';
  bloqueo: Partial<Bloqueo> = {};
  cita: Partial<Cita> = {};

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.api.get<Odontologo[]>('/odontologos/activos').subscribe({
      next: (r) => {
        this.odontologos = r;
        if (r.length) {
          this.odontologoSeleccionado = String(r[0].id);
          this.cargarTodo();
        }
        this.cdr.markForCheck();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
    this.api.get<Servicio[]>('/servicios/activos').subscribe({
      next: (r) => { this.servicios = r; this.cdr.markForCheck(); },
      error: () => this.cdr.markForCheck(),
    });
    this.api.get<Page<Paciente>>('/pacientes?estado=ACTIVO&page=0&size=500').subscribe({
      next: (r) => { this.pacientes = r.content; this.cdr.markForCheck(); },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  get diario(): string {
    const d = new Date(this.fecha + 'T00:00:00');
    return d.toLocaleDateString('es-EC', { weekday: 'long' });
  }

  get odontologoNum(): number | undefined {
    const n = Number(this.odontologoSeleccionado);
    return Number.isFinite(n) && n > 0 ? n : undefined;
  }

  setVista(v: string): void {
    this.vista = v as 'dia' | 'semana' | 'mes';
  }

  visitarDia(d: string): void {
    this.fecha = d;
    this.setVista('dia');
    this.cargarTodo();
  }

  cargarTodo(): void {
    if (!this.odontologoSeleccionado) return;
    const o = this.odontologoSeleccionado;
    const f = this.fecha;
    withLoading(this, this.api.get<Cita[]>(`/agenda?odontologoId=${o}&fecha=${f}`), undefined, this.cdr).subscribe({
      next: (r) => {
        this.citas = [...r].sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
        this.cdr.markForCheck();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
    this.api.get<Bloqueo[]>(`/agenda/bloqueos?odontologoId=${o}&fecha=${f}`).subscribe({
      next: (r) => { this.bloqueos = r; this.cdr.markForCheck(); },
      error: () => this.cdr.markForCheck(),
    });
    this.api.get<Horario[]>(`/horarios?odontologoId=${o}`).subscribe({
      next: (r) => {
        const d = new Date(f + 'T00:00:00');
        const dia = ((d.getDay() + 6) % 7) + 1;
        this.horarios = r.filter((h) => h.diaSemana === dia && h.estado === 'ACTIVO');
        this.cdr.markForCheck();
      },
      error: () => this.cdr.markForCheck(),
    });
    this.cargarSlots();
  }

  cargarSlots(): void {
    if (!this.odontologoSeleccionado || !this.servicioSeleccionado) {
      this.slots = [];
      this.cdr.markForCheck();
      return;
    }
    const svc = this.servicios.find((s) => s.id === Number(this.servicioSeleccionado));
    this.api
      .get<Slot[]>(
        `/agenda/disponibilidad?odontologoId=${this.odontologoSeleccionado}&fecha=${this.fecha}&servicioId=${this.servicioSeleccionado}&duracion=${svc?.duracionMinutos ?? ''}`,
      )
      .subscribe({
        next: (r) => { this.slots = r; this.cdr.markForCheck(); },
        error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
      });
  }

  crearBloqueo(): void {
    this.bloqueo = { horaInicio: '08:00', horaFin: '12:00', motivo: '' };
    this.showBloqueo = true;
    this.cdr.markForCheck();
  }

  guardarBloqueo(): void {
    this.api
      .post<Bloqueo>('/agenda/bloqueos', {
        odontologoId: Number(this.odontologoSeleccionado),
        fecha: this.fecha,
        horaInicio: this.bloqueo.horaInicio || null,
        horaFin: this.bloqueo.horaFin || null,
        motivo: this.bloqueo.motivo ?? null,
      })
      .subscribe({
        next: () => {
          this.showBloqueo = false;
          this.cdr.markForCheck();
          this.cargarTodo();
        },
        error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
      });
  }

  quitarBloqueo(b: Bloqueo): void {
    if (!confirm('¿Quitar este bloqueo?')) return;
    this.api.del<void>(`/agenda/bloqueos/${b.id}`).subscribe({
      next: () => this.cargarTodo(),
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  crearCitaEn(s: Slot): void {
    if (this.pacientes.length === 0) {
      this.api.get<Page<Paciente>>('/pacientes?estado=ACTIVO&page=0&size=500').subscribe({
        next: (r) => { this.pacientes = r.content; this.cdr.markForCheck(); },
        error: () => this.cdr.markForCheck(),
      });
    }
    const svc = this.servicios.find((s2) => s2.id === Number(this.servicioSeleccionado));
    this.cita = {
      pacienteId: '' as unknown as number,
      servicioId: svc ? svc.id : undefined,
      horaInicio: s.horaInicio,
      observaciones: '',
    };
    this.horaSugerida = s.horaInicio;
    this.showCita = true;
    this.cdr.markForCheck();
  }

  onServicio(): void {
    if (this.cita.horaInicio) return;
    const svc = this.servicios.find((s) => s.id === Number(this.cita.servicioId));
    if (svc) {
      const now = new Date();
      now.setHours(8, 0, 0, 0);
      this.cita.horaInicio = now.toTimeString().slice(0, 5);
    }
  }

  guardarCita(): void {
    if (!this.cita.pacienteId || !this.cita.servicioId) {
      this.error = 'Selecciona el paciente y el servicio antes de agendar';
      this.cdr.markForCheck();
      return;
    }
    if (!this.cita.horaInicio) {
      this.error = 'Selecciona una hora para la cita';
      this.cdr.markForCheck();
      return;
    }
    this.api
      .post<Cita>('/citas', {
        pacienteId: Number(this.cita.pacienteId),
        doctorId: Number(this.odontologoSeleccionado),
        servicioId: Number(this.cita.servicioId),
        fecha: this.fecha,
        horaInicio: this.cita.horaInicio,
        observaciones: this.cita.observaciones ?? null,
      })
      .subscribe({
        next: () => {
          this.showCita = false;
          this.cdr.markForCheck();
          this.cargarTodo();
        },
        error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
      });
  }

  badge(estado: string): string {
    switch (estado) {
      case 'CONFIRMADA':
        return 'ok';
      case 'ATENDIDA':
        return 'info';
      case 'CANCELADA':
        return 'bad';
      case 'NO_ASISTIO':
        return 'warn';
      default:
        return 'dim';
    }
  }

  close(e: MouseEvent): void {
    if (e.target === e.currentTarget) {
      this.showBloqueo = false;
      this.showCita = false;
    }
  }

  stop(e: MouseEvent): void {
    e.stopPropagation();
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    if (a?.status === 409 || a?.status === 400) return a.error?.message ?? 'Datos inválidos';
    return 'Error de conexión';
  }
}