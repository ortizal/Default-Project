import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Api } from '../core/api';
import { Notificacion } from '../core/models';

@Component({
  selector: 'app-notificaciones',
  templateUrl: './notificaciones.html',
  imports: [CommonModule],
})
export class NotificacionesComponent {
  items: Notificacion[] = [];
  error = '';

  constructor(private readonly api: Api) {
    this.cargar();
  }

  cargar(): void {
    this.api.get<Notificacion[]>('/automatizaciones/notificaciones?limite=100').subscribe({
      next: (r) => (this.items = r),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  badge(estado?: string): string {
    switch (estado) {
      case 'PENDIENTE':
        return 'warn';
      case 'ENVIADA':
        return 'ok';
      case 'ERROR':
        return 'bad';
      case 'CANCELADA':
        return 'dim';
      default:
        return 'dim';
    }
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}