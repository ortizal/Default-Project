import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatPaginatorModule } from '@angular/material/paginator';
import { Api } from '../core/api';
import { Cita, Odontologo, Page } from '../core/models';
import { AppDateComponent } from '../core/app-date.component';

@Component({
  selector: 'app-citas',
  templateUrl: './citas.html',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatDatepickerModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatChipsModule,
    MatTooltipModule,
    MatToolbarModule,
    MatPaginatorModule,
    AppDateComponent,
  ],
})
export class CitasComponent {
  items: Cita[] = [];
  odontologos: Odontologo[] = [];
  cargando = false;
  desde = new Date().toISOString().slice(0, 10);
  hasta = '';
  estado = '';
  doctorId = '';
  page = 0;
  size = 15;
  totalPages = 1;
  total = 0;
  private reqSeq = 0;
  readonly cols = ['fecha', 'hora', 'paciente', 'servicio', 'odontologo', 'estado', 'acciones'];
  error = '';

  constructor(private readonly api: Api) {
    this.cargar(0);
    this.api.get<Odontologo[]>('/odontologos/activos').subscribe((r) => (this.odontologos = r));
  }

  cargar(p: number): void {
    this.cargando = true;
    this.error = '';
    const seq = ++this.reqSeq;
    const params = new URLSearchParams({ page: String(p), size: String(this.size), desde: this.desde || '', hasta: this.hasta || '' });
    if (this.estado) params.set('estado', this.estado);
    if (this.doctorId) params.set('doctorId', this.doctorId);
    this.api.get<Page<Cita>>(`/citas?${params.toString()}`).subscribe({
      next: (r) => {
        if (seq !== this.reqSeq) return;
        this.cargando = false;
        this.items = r.content;
        this.totalPages = Math.max(r.totalPages ?? 1, 1);
        this.total = r.totalElements ?? this.items.length;
        this.page = Math.min(p, this.totalPages - 1);
      },
      error: (e) => {
        if (seq !== this.reqSeq) return;
        this.error = this.msg(e);
        this.cargando = false;
      },
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

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    if (a?.status === 409 || a?.status === 400) return a.error?.message ?? 'Datos inválidos';
    if (a?.status === 403) return 'Sin permisos para esta acción';
    return 'Error de conexión';
  }
}