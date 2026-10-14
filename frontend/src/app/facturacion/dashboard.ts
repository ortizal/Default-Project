import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { DashboardFinanciero, ReporteVentas } from '../core/models';
import { withLoading } from '../core/loading';
import { UiPageHeaderComponent } from '../ui';
import { etiquetaFormaPago, moneda } from './util';

/**
 * Dashboard financiero (Fase F, §3.12): facturado y cobrado del rango,
 * de hoy y del mes, cuentas por cobrar, anticipos, ticket promedio y
 * desgloses por forma de pago, odontólogo y servicio. Sin datos SRI
 * (Fase G).
 */
@Component({
  selector: 'app-facturacion-dashboard',
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
  imports: [CommonModule, FormsModule, UiPageHeaderComponent],
})
export class FacturacionDashboardComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly moneda = moneda;
  readonly etiquetaFormaPago = etiquetaFormaPago;

  e: DashboardFinanciero | null = null;
  ventas: ReporteVentas | null = null;
  ventasAnterior: ReporteVentas | null = null;
  cargando = false;
  error = '';
  readonly rangos = [
    { dias: 30, etiqueta: '30 días' },
    { dias: 90, etiqueta: '90 días' },
    { dias: 365, etiqueta: '12 meses' },
  ];
  escenario: 'conservador' | 'base' | 'optimista' = 'base';
  porcentajeRiesgo: number | null = null;
  desde = new Date(Date.now() - 365 * 86400000).toISOString().slice(0, 10);
  hasta = new Date().toISOString().slice(0, 10);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    const params = new URLSearchParams();
    if (this.desde) params.set('desde', this.desde);
    if (this.hasta) params.set('hasta', this.hasta);
    const dias = this.diasRango;
    const ancla = this.desde || this.hasta || new Date().toISOString().slice(0, 10);
    const hastaAnterior = this.desplazarFecha(ancla, -1);
    const desdeAnterior = this.desplazarFecha(ancla, -dias);
    const paramsAnterior = new URLSearchParams({ desde: desdeAnterior, hasta: hastaAnterior });
    this.error = '';
    const detalle$ = this.puedeVerReportes
      ? forkJoin({
          ventas: this.api.get<ReporteVentas>(`/facturacion/reportes/ventas?${params.toString()}`),
          anterior: this.api.get<ReporteVentas>(`/facturacion/reportes/ventas?${paramsAnterior.toString()}`),
        })
      : of({ ventas: null, anterior: null });
    withLoading(this, forkJoin({
      dashboard: this.api.get<DashboardFinanciero>(`/facturacion/dashboard?${params.toString()}`),
      detalle: detalle$,
    }), undefined, this.cdr).subscribe({
      next: ({ dashboard, detalle }) => {
        this.e = dashboard;
        this.ventas = detalle.ventas;
        this.ventasAnterior = detalle.anterior;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = this.msg(err);
        this.cdr.markForCheck();
      },
    });
  }

  seleccionarRango(dias: number): void {
    this.hasta = new Date().toISOString().slice(0, 10);
    this.desde = this.desplazarFecha(this.hasta, -(dias - 1));
    this.cargar();
  }

  seleccionarEscenario(escenario: 'conservador' | 'base' | 'optimista'): void {
    this.escenario = escenario;
  }

  get diasRango(): number {
    const inicio = Date.parse(`${this.desde || this.hasta}T00:00:00Z`);
    const fin = Date.parse(`${this.hasta || this.desde}T00:00:00Z`);
    return Number.isFinite(inicio) && Number.isFinite(fin) && fin >= inicio
      ? Math.floor((fin - inicio) / 86400000) + 1
      : 1;
  }

  get puedeVerReportes(): boolean {
    return this.auth.puede('REPORTES_FINANCIEROS');
  }

  get crecimiento(): number | null {
    const anterior = this.ventasAnterior?.facturado ?? 0;
    return anterior > 0 && this.ventas
      ? ((this.ventas.facturado - anterior) / anterior) * 100
      : null;
  }

  get proyeccion(): number {
    const factor = this.escenario === 'conservador' ? 0.9 : this.escenario === 'optimista' ? 1.1 : 1;
    return ((this.ventas?.facturado ?? 0) / this.diasRango) * 30 * factor;
  }

  get riesgoEstimado(): number | null {
    return this.porcentajeRiesgo === null
      ? null
      : ((this.e?.cuentasPorCobrar ?? 0) * this.porcentajeRiesgo) / 100;
  }

  get tendencia(): { etiqueta: string; facturado: number; cobrado: number }[] {
    const filas = this.ventas?.porDia ?? [];
    const grupos = new Map<string, { etiqueta: string; facturado: number; cobrado: number }>();
    for (const fila of filas) {
      const fecha = new Date(`${fila.fecha}T00:00:00Z`);
      const indiceDia = Math.floor((fecha.getTime() - Date.parse(`${this.desde}T00:00:00Z`)) / 86400000);
      const clave = this.diasRango <= 45
        ? fila.fecha
        : this.diasRango <= 180
          ? `semana-${Math.floor(indiceDia / 7)}`
          : fila.fecha.slice(0, 7);
      const etiqueta = this.diasRango <= 45
        ? fecha.toLocaleDateString('es', { day: '2-digit', month: 'short', timeZone: 'UTC' })
        : this.diasRango <= 180
          ? `Sem. ${Math.floor(indiceDia / 7) + 1}`
          : fecha.toLocaleDateString('es', { month: 'short', timeZone: 'UTC' });
      const grupo = grupos.get(clave) ?? { etiqueta, facturado: 0, cobrado: 0 };
      grupo.facturado += fila.facturado;
      grupo.cobrado += fila.cobrado;
      grupos.set(clave, grupo);
    }
    return [...grupos.values()];
  }

  get maxTendencia(): number {
    return Math.max(1, ...this.tendencia.map((p) => Math.max(p.facturado, p.cobrado)));
  }

  alturaBarra(valor: number): number {
    return Math.max(2, (valor / this.maxTendencia) * 100);
  }

  private desplazarFecha(fecha: string, dias: number): string {
    const valor = new Date(`${fecha}T00:00:00Z`);
    valor.setUTCDate(valor.getUTCDate() + dias);
    return valor.toISOString().slice(0, 10);
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    if (a?.status === 403) return 'No tienes permiso para ver el dashboard financiero.';
    return a?.status === 400
      ? (a.error?.message ?? 'Rango de fechas inválido')
      : 'Error de conexión';
  }
}
