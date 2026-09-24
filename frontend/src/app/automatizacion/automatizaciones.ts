import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Automatizacion, Plantilla } from '../core/models';

const EVENTOS = ['CITA_CREADA', 'CITA_PROXIMA', 'CITA_CONFIRMADA', 'CITA_CANCELADA', 'CITA_ATENDIDA', 'NO_ASISTIO'];

@Component({
  selector: 'app-automatizaciones',
  templateUrl: './automatizaciones.html',
  imports: [CommonModule, FormsModule],
})
export class AutomatizacionesComponent implements OnInit {
  items: Automatizacion[] = [];
  plantillas: Plantilla[] = [];
  readonly EVENTOS = EVENTOS;
  error = '';
  showForm = false;
  form: Partial<Automatizacion> = {};

  constructor(private readonly api: Api) {}

  ngOnInit(): void {
    this.cargar();
    this.api.get<Plantilla[]>('/plantillas').subscribe((r) => (this.plantillas = r));
  }

  cargar(): void {
    this.api.get<Automatizacion[]>('/automatizaciones').subscribe({
      next: (r) => (this.items = r),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  nueva(): void {
    this.form = { nombre: '', evento: EVENTOS[0], minutosAntes: 0, plantillaId: undefined, condicion: '', activa: true };
    this.showForm = true;
  }

  editar(a: Automatizacion): void {
    this.form = { ...a };
    this.showForm = true;
  }

  toggle(a: Automatizacion): void {
    const r = a.activa
      ? this.api.post<Automatizacion>(`/automatizaciones/${a.id}/desactivar`, {})
      : this.api.post<Automatizacion>(`/automatizaciones/${a.id}/activar`, {});
    r.subscribe({
      next: () => this.cargar(),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  guardar(): void {
    if (!this.form.nombre || !this.form.evento || !this.form.plantillaId) {
      this.error = 'Completa nombre, evento y plantilla';
      return;
    }
    const body = {
      nombre: this.form.nombre,
      evento: this.form.evento,
      minutosAntes: this.form.minutosAntes ?? 0,
      plantillaId: Number(this.form.plantillaId),
      condicion: this.form.condicion || null,
      activa: this.form.activa ?? true,
    };
    const req = this.form.id
      ? this.api.put<Automatizacion>(`/automatizaciones/${this.form.id}`, body)
      : this.api.post<Automatizacion>('/automatizaciones', body);
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cargar();
      },
      error: (e) => (this.error = this.msg(e)),
    });
  }

  eliminar(a: Automatizacion): void {
    if (!confirm('¿Eliminar esta automatización?')) return;
    this.api.del<void>(`/automatizaciones/${a.id}`).subscribe({
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