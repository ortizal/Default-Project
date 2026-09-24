import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Page, Servicio } from '../core/models';
import { withLoading } from '../core/loading';

@Component({
  selector: 'app-servicios',
  templateUrl: './servicios.html',
  imports: [CommonModule, FormsModule],
})
export class ServiciosComponent implements OnInit {
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

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargar(0);
  }

  cargar(p: number): void {
    this.error = '';
    const params = new URLSearchParams({ page: String(p), size: String(this.size) });
    if (this.q.trim()) params.set('q', this.q.trim());
    if (this.estado) params.set('estado', this.estado);
    withLoading(this, this.api.get<Page<Servicio>>(`/servicios?${params.toString()}`), undefined, this.cdr).subscribe({
      next: (r) => {
        this.items = r.content;
        this.totalPages = Math.max(r.totalPages ?? 1, 1);
        this.page = Math.min(p, this.totalPages - 1);
       this.cdr.markForCheck(); },
error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  nuevo(): void {
    this.form = { duracionMinutos: 30, precio: 0, estado: 'ACTIVO' };
    this.showForm = true;
    this.cdr.markForCheck();
  }

  editar(s: Servicio): void {
    this.form = { ...s };
    this.showForm = true;
    this.cdr.markForCheck();
  }
  guardar(): void {
    if (!this.form.nombre || !this.form.duracionMinutos || this.form.precio == null) {
      this.error = 'Completa nombre, duración y precio (campos obligatorios)';
      this.cdr.markForCheck();
      return;
    }
    const body = { ...this.form, estado: this.form.estado || 'ACTIVO' };
    const req = this.form.id
      ? this.api.put<Servicio>(`/servicios/${this.form.id}`, body)
      : this.api.post<Servicio>('/servicios', body);
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cdr.markForCheck();
        this.cargar(this.page);
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  desactivar(s: Servicio): void {
    this.api.del<void>(`/servicios/${s.id}`).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
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