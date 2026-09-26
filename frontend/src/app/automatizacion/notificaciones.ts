import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Notificacion } from '../core/models';
import { withLoading } from '../core/loading';
import { UiPageHeaderComponent, UiTableComponent } from '../ui';

@Component({
  selector: 'app-notificaciones',
  templateUrl: './notificaciones.html',
  imports: [CommonModule, FormsModule, UiPageHeaderComponent, UiTableComponent],
})
export class NotificacionesComponent implements OnInit {
  items: Notificacion[] = [];
  error = '';
  cargando = false;
  estadoF = '';
  eventoF = '';

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    const params = new URLSearchParams({ limite: '200' });
    if (this.estadoF) params.set('estado', this.estadoF);
    if (this.eventoF) params.set('evento', this.eventoF);
    withLoading(this, this.api.get<Notificacion[]>(`/automatizaciones/notificaciones?${params.toString()}`), undefined, this.cdr).subscribe({
      next: (r) => { this.items = r; this.cdr.markForCheck(); },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
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