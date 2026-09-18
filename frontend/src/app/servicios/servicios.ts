import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Page, Servicio } from '../core/models';

@Component({
  selector: 'app-servicios',
  templateUrl: './servicios.html',
  imports: [CommonModule, FormsModule],
})
export class ServiciosComponent {
  items: Servicio[] = [];
  q = '';
  estado = 'ACTIVO';
  page = 0;
  size = 15;
  totalPages = 1;
  error = '';
  cargando = false;
  showForm = false;
  form: Partial<Servicio> = {};

  constructor(private readonly api: Api) {
    this.cargar(0);
  }

  cargar(p: number): void {
    this.error = '';
    this.cargando = true;
    const params = new URLSearchParams({ page: String(p), size: String(this.size) });
    if (this.q.trim()) params.set('q', this.q.trim());
    if (this.estado) params.set('estado', this.estado);
    this.api.get<Page<Servicio>>(`/servicios?${params.toString()}`).subscribe({
      next: (r) => {
        this.cargando = false;
        this.items = r.content;
        this.totalPages = Math.max(r.totalPages ?? 1, 1);
        this.page = Math.min(p, this.totalPages - 1);
      },
      error: (e) => {
        this.cargando = false;
        this.error = this.msg(e);
      },
    });
  }

  nuevo(): void {
    this.form = { duracionMinutos: 30, precio: 0, estado: 'ACTIVO' };
    this.showForm = true;
  }

editar(s: Servicio): void {
    this.form = { ...s };
    this.showForm = true;
  }
  guardar(): void {
    if (!this.form.nombre || !this.form.duracionMinutos || this.form.precio == null) {
      this.error = 'Completa nombre, duración y precio (campos obligatorios)';
      return;
    }
    const body = { ...this.form, estado: this.form.estado || 'ACTIVO' };
    const req = this.form.id
      ? this.api.put<Servicio>(`/servicios/${this.form.id}`, body)
      : this.api.post<Servicio>('/servicios', body);
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cargar(this.page);
      },
      error: (e) => (this.error = this.msg(e)),
    });
  }

  desactivar(s: Servicio): void {
    this.api.del<void>(`/servicios/${s.id}`).subscribe({
      next: () => this.cargar(this.page),
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