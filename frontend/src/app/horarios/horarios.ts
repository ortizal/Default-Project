import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { DIAS, Horario, nombreEstado, Odontologo } from '../core/models';

@Component({
  selector: 'app-horarios',
  templateUrl: './horarios.html',
  imports: [CommonModule, FormsModule],
})
export class HorariosComponent implements OnInit {
  readonly DIAS = DIAS;
  readonly nombreEstado = nombreEstado;
  odontologos: Odontologo[] = [];
  odontologoSeleccionado = '';
  items: Horario[] = [];
  error = '';
  showForm = false;
  form: Partial<Horario> = {};

  constructor(private readonly api: Api) {}

  ngOnInit(): void {
    this.api.get<Odontologo[]>('/odontologos/activos').subscribe({
      next: (r) => {
        this.odontologos = r;
        if (r.length) {
          this.odontologoSeleccionado = String(r[0].id);
          this.cargar();
        }
      },
      error: (e) => (this.error = this.msg(e)),
    });
  }

  cargar(): void {
    if (!this.odontologoSeleccionado) return;
    this.api.get<Horario[]>(`/horarios?odontologoId=${this.odontologoSeleccionado}`).subscribe({
      next: (r) => (this.items = r),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  nuevo(): void {
    this.form = { diaSemana: 1, horaInicio: '08:00', horaFin: '17:00', intervaloMinutos: 30, estado: 'ACTIVO' };
    this.showForm = true;
  }

editar(h: Horario): void {
    this.form = { ...h };
    this.showForm = true;
  }
  guardar(): void {
    if (!this.form.diaSemana || !this.form.horaInicio || !this.form.horaFin) {
      this.error = 'Completa día, hora de inicio y hora de fin';
      return;
    }
    const body = { ...this.form, odontologoId: Number(this.odontologoSeleccionado), estado: this.form.estado || 'ACTIVO' };
    const req = this.form.id
      ? this.api.put<Horario>(`/horarios/${this.form.id}`, body)
      : this.api.post<Horario>('/horarios', body);
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cargar();
      },
      error: (e) => (this.error = this.msg(e)),
    });
  }

  eliminar(h: Horario): void {
    if (!confirm('¿Eliminar este horario?')) return;
    this.api.del<void>(`/horarios/${h.id}`).subscribe({
      next: () => this.cargar(),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  close(e: MouseEvent): void {
    if (e.target === e.currentTarget) this.showForm = false;
  }

  stop(e: MouseEvent): void {
    e.stopPropagation();
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}