import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Odontologo, Page, GoogleCalendario } from '../core/models';

@Component({
  selector: 'app-odontologos',
  templateUrl: './odontologos.html',
  imports: [CommonModule, FormsModule],
})
export class OdontologosComponent {
  items: Odontologo[] = [];
  activos: Odontologo[] = [];
  q = '';
  estado = 'ACTIVO';
  page = 0;
  size = 15;
  totalPages = 1;
  error = '';
  cargando = false;
  showForm = false;
  form: Partial<Odontologo> = {};
  calendarios: GoogleCalendario[] = [];

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {
    this.cargar(0);
    this.api.get<Odontologo[]>('/odontologos/activos').subscribe((r) => {
      this.activos = r;
      this.cdr.detectChanges();
    });
    this.api.get<GoogleCalendario[]>('/google/calendars').subscribe({
      next: (r) => {
        this.calendarios = r;
        this.cdr.detectChanges();
      },
      error: () => {
        this.calendarios = [];
        this.cdr.detectChanges();
      },
    });
  }

  cargar(p: number): void {
    this.cargando = true;
    this.error = '';
    const params = new URLSearchParams({ page: String(p), size: String(this.size) });
    if (this.q.trim()) params.set('q', this.q.trim());
    if (this.estado) params.set('estado', this.estado);
    this.api.get<Page<Odontologo>>(`/odontologos?${params.toString()}`).subscribe({
      next: (r) => {
        this.cargando = false;
        this.items = r.content;
        this.totalPages = Math.max(r.totalPages ?? 1, 1);
        this.page = Math.min(p, this.totalPages - 1);
        this.cdr.detectChanges();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }

  nuevo(): void {
    this.form = { estado: 'ACTIVO' };
    this.showForm = true;
  }

editar(o: Odontologo): void {
    this.form = { ...o };
    this.showForm = true;
  }
  guardar(): void {
    if (!this.form.nombres || !this.form.apellidos) {
      this.error = 'Completa nombres y apellidos (campos obligatorios)';
      return;
    }
    const body = { ...this.form, estado: this.form.estado || 'ACTIVO' };
    const req = this.form.id
      ? this.api.put<Odontologo>(`/odontologos/${this.form.id}`, body)
      : this.api.post<Odontologo>('/odontologos', body);
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cargar(this.page);
        this.api.get<Odontologo[]>('/odontologos/activos').subscribe((r) => (this.activos = r));
      },
      error: (e) => {
        this.cargando = false;
        this.error = this.msg(e);
      },
    });
  }

  desactivar(o: Odontologo): void {
    this.api.del<void>(`/odontologos/${o.id}`).subscribe({
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