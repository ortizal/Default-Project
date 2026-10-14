import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { ReporteClinicoData } from '../core/models';
import { withLoading } from '../core/loading';
import { UiBadgeComponent, UiBadgeTone, UiPageHeaderComponent } from '../ui';

export function etiquetaPrioridad(prioridad: string | null): string {
  if (!prioridad) return '—';
  return prioridad.charAt(0) + prioridad.slice(1).toLowerCase();
}

export function tonePrioridad(prioridad: string | null): UiBadgeTone {
  switch (prioridad) {
    case 'URGENTE':
      return 'danger';
    case 'ALTA':
      return 'warn';
    case 'MEDIA':
      return 'info';
    default:
      return 'muted';
  }
}

export function moneda(valor: number | null): string {
  if (valor === null || valor === undefined) return '—';
  return '$ ' + Number(valor).toFixed(2);
}

export function pieza(diente: number | null, superficie: string | null): string {
  if (diente === null || diente === undefined) return '—';
  return superficie ? `${diente} · ${superficie.toLowerCase()}` : String(diente);
}

/**
 * Reportes clínicos del consultorio (Fase E, §4.17): totales del período,
 * diagnósticos frecuentes, tratamientos pendientes, continuidad de la
 * atención y producción clínica.
 */
@Component({
  selector: 'app-reportes-clinicos',
  templateUrl: './reportes-clinicos.html',
  imports: [CommonModule, FormsModule, UiBadgeComponent, UiPageHeaderComponent],
})
export class ReportesClinicosComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly etiquetaPrioridad = etiquetaPrioridad;
  readonly tonePrioridad = tonePrioridad;
  readonly moneda = moneda;
  readonly pieza = pieza;
  readonly limites = [5, 10, 25, 50];

  e: ReporteClinicoData | null = null;
  cargando = false;
  error = '';
  desde = new Date(Date.now() - 365 * 86400000).toISOString().slice(0, 10);
  hasta = new Date().toISOString().slice(0, 10);
  limite = 10;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    const params = new URLSearchParams();
    if (this.desde) params.set('desde', this.desde);
    if (this.hasta) params.set('hasta', this.hasta);
    params.set('limite', String(this.limite));
    this.error = '';
    withLoading(
      this,
      this.api.get<ReporteClinicoData>(`/historia-clinica/reportes?${params.toString()}`),
      undefined,
      this.cdr,
    ).subscribe({
      next: (r) => {
        this.e = r;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = this.msg(err);
        this.cdr.markForCheck();
      },
    });
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    if (a?.status === 403) return 'No tienes permiso para ver los reportes clínicos.';
    return a?.status === 400
      ? (a.error?.message ?? 'Rango de fechas inválido')
      : 'Error de conexión';
  }
}
