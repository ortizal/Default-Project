import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { EstadisticasData, ReporteSerie } from '../core/models';
import { withLoading } from '../core/loading';

@Component({
  selector: 'app-reportes',
  templateUrl: './reportes.html',
  imports: [CommonModule, FormsModule],
})
export class ReportesComponent implements OnInit {
  e: EstadisticasData | null = null;
  error = '';
  cargando = false;
  desde = new Date(Date.now() - 29 * 86400000).toISOString().slice(0, 10);
  hasta = new Date().toISOString().slice(0, 10);

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    const params = new URLSearchParams();
    if (this.desde) params.set('desde', this.desde);
    if (this.hasta) params.set('hasta', this.hasta);
    withLoading(this, this.api.get<EstadisticasData>(`/reportes/estadisticas?${params.toString()}`), undefined, this.cdr).subscribe({
      next: (r) => { this.e = r; this.cdr.markForCheck(); },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  get maxOd(): number {
    return Math.max(1, ...(this.e?.citasPorOdontologo?.map((x) => x.total) ?? [1]));
  }

  get maxDia(): number {
    return Math.max(1, ...(this.e?.citasPorDia?.map((x) => x.total) ?? [1]));
  }

  pct(v: number, max: number): number {
    return Math.round((Number(v) / max) * 100);
  }

  tot(serie?: ReporteSerie[]): number {
    return (serie ?? []).reduce((a, s) => a + (Number(s.total) || 0), 0);
  }

  bar(v: number, max: number): number {
    return Math.round((Number(v) / max) * 90) + 2;
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}