import { Component, OnInit } from '@angular/core';
import { Api } from '../core/api';
import { EstadisticasData, ReporteSerie } from '../core/models';

@Component({
  selector: 'app-reportes',
  templateUrl: './reportes.html',
})
export class ReportesComponent implements OnInit {
  e: EstadisticasData | null = null;
  error = '';

  constructor(private readonly api: Api) {}

  ngOnInit(): void {
    this.api.get<EstadisticasData>('/reportes/estadisticas').subscribe({
      next: (r) => (this.e = r),
      error: (e) => (this.error = this.msg(e)),
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