import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Cita, Odontologo, Servicio, Paciente, Page } from '../core/models';

@Component({
  selector: 'app-citas',
  templateUrl: './citas.html',
  styleUrl: './citas.css',
  imports: [CommonModule, FormsModule],
})
export class CitasComponent {
  items: Cita[] = [];
  odontologos: Odontologo[] = [];
  servicios: Servicio[] = [];
  pacientes: Paciente[] = [];
  cargando = false;
  desde = new Date().toISOString().slice(0, 10);
  hasta = '';
  estado = '';
  doctorId = '';
  pacienteNombre = '';
  page = 0;
  size = 15;
  totalPages = 1;
  total = 0;
  error = '';
  mostrarModal = false;
  private reqSeq = 0;
  nuevaCita = { pacienteId: 0, doctorId: 0, servicioId: 0, fecha: '', horaInicio: '', observaciones: '' };

  totalPendientes = 0;
  totalConfirmadas = 0;
  totalAtendidas = 0;
  totalCanceladas = 0;

  constructor(private readonly api: Api) {
    this.cargar(0);
    this.api.get<Odontologo[]>('/odontologos/activos').subscribe((r) => (this.odontologos = r));
    this.api.get<Servicio[]>('/servicios/activos').subscribe((r) => (this.servicios = r));
    this.api.get<Page<Paciente>>('/pacientes?page=0&size=100').subscribe((r) => (this.pacientes = r.content));
  }

  cargar(p: number): void {
    this.cargando = true;
    this.error = '';
    const seq = ++this.reqSeq;
    const params = new URLSearchParams({ page: String(p), size: String(this.size), desde: this.desde || '', hasta: this.hasta || '' });
    if (this.estado) params.set('estado', this.estado);
    if (this.doctorId) params.set('doctorId', this.doctorId);
    if (this.pacienteNombre) params.set('paciente', this.pacienteNombre);
    this.api.get<Page<Cita>>(`/citas?${params.toString()}`).subscribe({
      next: (r) => {
        if (seq !== this.reqSeq) return;
        this.cargando = false;
        this.items = r.content;
        this.totalPages = Math.max(r.totalPages ?? 1, 1);
        this.total = r.totalElements ?? this.items.length;
        this.page = Math.min(p, this.totalPages - 1);
        this.calcularTotales();
      },
      error: (e) => {
        if (seq !== this.reqSeq) return;
        this.error = this.msg(e);
        this.cargando = false;
      },
    });
  }

  calcularTotales(): void {
    this.totalPendientes = this.items.filter((c) => c.estado === 'PENDIENTE').length;
    this.totalConfirmadas = this.items.filter((c) => c.estado === 'CONFIRMADA').length;
    this.totalAtendidas = this.items.filter((c) => c.estado === 'ATENDIDA').length;
    this.totalCanceladas = this.items.filter((c) => c.estado === 'CANCELADA').length;
  }

  abrirNueva(): void {
    this.nuevaCita = { pacienteId: 0, doctorId: this.odontologos[0]?.id ?? 0, servicioId: this.servicios[0]?.id ?? 0, fecha: this.desde, horaInicio: '', observaciones: '' };
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
  }

  crearCita(): void {
    if (!this.nuevaCita.pacienteId || !this.nuevaCita.doctorId || !this.nuevaCita.servicioId || !this.nuevaCita.fecha) {
      this.error = 'Completa todos los campos obligatorios';
      return;
    }
    this.cargando = true;
    this.api.post<Cita>('/citas', {
      pacienteId: this.nuevaCita.pacienteId,
      doctorId: this.nuevaCita.doctorId,
      servicioId: this.nuevaCita.servicioId,
      fecha: this.nuevaCita.fecha,
      horaInicio: this.nuevaCita.horaInicio,
      observaciones: this.nuevaCita.observaciones || '',
    }).subscribe({
      next: () => {
        this.mostrarModal = false;
        this.cargar(0);
      },
      error: (e) => { this.error = this.msg(e); this.cargando = false; },
    });
  }

  confirmar(c: Cita): void {
    this.api.post<Cita>(`/citas/${c.id}/confirmar`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  atender(c: Cita): void {
    this.api.post<Cita>(`/citas/${c.id}/atender`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  cancelar(c: Cita): void {
    const motivo = prompt('Motivo de cancelación');
    if (motivo === null) return;
    this.api.post<Cita>(`/citas/${c.id}/cancelar`, { motivo }).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  noAsistio(c: Cita): void {
    if (!confirm('¿Registrar como no asistió?')) return;
    this.api.post<Cita>(`/citas/${c.id}/no-asistio`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  badge(estado: string): string {
    switch (estado) {
      case 'CONFIRMADA': return 'ok';
      case 'ATENDIDA': return 'info';
      case 'CANCELADA': return 'bad';
      case 'NO_ASISTIO': return 'warn';
      default: return 'dim';
    }
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    if (a?.status === 409 || a?.status === 400) return a.error?.message ?? 'Datos inválidos';
    if (a?.status === 403) return 'Sin permisos para esta acción';
    return 'Error de conexión';
  }
}
