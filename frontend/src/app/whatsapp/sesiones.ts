import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Api } from '../core/api';
import { WhatsappSesion } from '../core/models';

@Component({
  selector: 'app-sesiones',
  templateUrl: './sesiones.html',
  imports: [
    CommonModule,
    FormsModule,
    MatToolbarModule,
    MatIconModule,
    MatTooltipModule,
    MatChipsModule,
    MatCardModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
})
export class SesionesComponent {
  private readonly api = inject(Api);
  cols = ['id', 'sesionId', 'nombre', 'estado', 'qr', 'acciones'];
  items: WhatsappSesion[] = [];
  buscar = '';
  cargando = false;
  error = '';
  page = 0;
  totalPages = 1;

  constructor() {
    this.cargar(0);
  }

  cargar(p: number): void {
    this.cargando = true;
    this.error = '';
    const params = new URLSearchParams({ page: String(p), busca: this.buscar || '' });
    this.api.get<WhatsappSesion[]>('/whatsapp/sesiones?' + params.toString()).subscribe({
      next: (r) => {
        this.items = r;
        this.cargando = false;
        this.page = Math.min(p, Math.max(0, this.totalPages - 1));
        this.totalPages = Math.max(Math.ceil(r.length / 10), 1);
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cargando = false;
      },
    });
  }

  probar(s: WhatsappSesion): void {
    this.api.post<WhatsappSesion>(`/whatsapp/sesiones/${s.id}/conectar`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  borrar(s: WhatsappSesion): void {
    this.api.del<void>(`/whatsapp/sesiones/${s.id}`).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  nueva(): void {
    const id = window.prompt('ID de sesión (OpenWA)') ?? '';
    if (!id) return;
    this.api.post<WhatsappSesion>('/whatsapp/sesiones', { sesionId: id, nombre: null }).subscribe({
      next: () => this.cargar(0),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a.status === 409 || a.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}
