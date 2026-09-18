import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Plantilla } from '../core/models';

@Component({
  selector: 'app-plantillas',
  templateUrl: './plantillas.html',
  imports: [CommonModule, FormsModule],
})
export class PlantillasComponent {
  items: Plantilla[] = [];
  error = '';
  showForm = false;
  form: Partial<Plantilla> = {};

  constructor(private readonly api: Api) {
    this.cargar();
  }

  cargar(): void {
    this.api.get<Plantilla[]>('/plantillas').subscribe({
      next: (r) => (this.items = r),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  nueva(): void {
    this.form = { nombre: '', contenido: '', activa: true };
    this.showForm = true;
  }

editar(p: Plantilla): void {
    this.form = { ...p };
    this.showForm = true;
  }
  guardar(): void {
    if (!this.form.nombre || !this.form.contenido) {
      this.error = 'Completa el nombre y el contenido de la plantilla';
      return;
    }
    const body = { nombre: this.form.nombre, contenido: this.form.contenido, activa: this.form.activa ?? true };
    const req = this.form.id
      ? this.api.put<Plantilla>(`/plantillas/${this.form.id}`, body)
      : this.api.post<Plantilla>('/plantillas', body);
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cargar();
      },
      error: (e) => (this.error = this.msg(e)),
    });
  }

  eliminar(p: Plantilla): void {
    if (!confirm('¿Eliminar esta plantilla?')) return;
    this.api.del<void>(`/plantillas/${p.id}`).subscribe({
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