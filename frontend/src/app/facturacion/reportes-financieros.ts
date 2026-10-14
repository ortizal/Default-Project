import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import {
  ReporteCaja,
  ReporteProduccion,
  ReporteSaldos,
  ReporteVentas,
} from '../core/models';
import { withLoading } from '../core/loading';
import { UiBadgeComponent, UiPageHeaderComponent } from '../ui';
import {
  etiquetaEstadoCuenta,
  etiquetaFormaPago,
  moneda,
  toneEstadoCuenta,
} from './util';

type Pestana = 'ventas' | 'produccion' | 'saldos' | 'caja';

/**
 * Reportes financieros (Fase F, §3.13): ventas por día y forma de pago,
 * producción por odontólogo, saldos por cobrar y conciliación de cajas.
 */
@Component({
  selector: 'app-reportes-financieros',
  templateUrl: './reportes-financieros.html',
  imports: [CommonModule, FormsModule, UiBadgeComponent, UiPageHeaderComponent],
})
export class ReportesFinancierosComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly moneda = moneda;
  readonly etiquetaFormaPago = etiquetaFormaPago;
  readonly etiquetaEstadoCuenta = etiquetaEstadoCuenta;
  readonly toneEstadoCuenta = toneEstadoCuenta;
  readonly pestanas: Pestana[] = ['ventas', 'produccion', 'saldos', 'caja'];

  etiquetaPestana(p: Pestana): string {
    switch (p) {
      case 'ventas':
        return 'Ventas';
      case 'produccion':
        return 'Producción';
      case 'saldos':
        return 'Saldos';
      default:
        return 'Caja';
    }
  }

  pestana: Pestana = 'ventas';
  desde = new Date(Date.now() - 90 * 86400000).toISOString().slice(0, 10);
  hasta = new Date().toISOString().slice(0, 10);

  ventas: ReporteVentas | null = null;
  produccion: ReporteProduccion | null = null;
  saldos: ReporteSaldos | null = null;
  caja: ReporteCaja | null = null;

  cargando = false;
  error = '';

  ngOnInit(): void {
    this.cargar();
  }

  seleccionar(p: Pestana): void {
    this.pestana = p;
    this.cargar();
  }

  cargar(): void {
    const params = new URLSearchParams();
    if (this.desde) params.set('desde', this.desde);
    if (this.hasta) params.set('hasta', this.hasta);
    const qs = params.toString();

    switch (this.pestana) {
      case 'ventas':
        this.pedir<ReporteVentas>(`/facturacion/reportes/ventas?${qs}`, (r) => (this.ventas = r));
        break;
      case 'produccion':
        this.pedir<ReporteProduccion>(
          `/facturacion/reportes/produccion?${qs}`,
          (r) => (this.produccion = r),
        );
        break;
      case 'saldos':
        this.pedir<ReporteSaldos>('/facturacion/reportes/saldos', (r) => (this.saldos = r));
        break;
      default:
        this.pedir<ReporteCaja>(`/facturacion/reportes/caja?${qs}`, (r) => (this.caja = r));
    }
  }

  private pedir<T>(ruta: string, asignar: (r: T) => void): void {
    this.error = '';
    withLoading(this, this.api.get<T>(ruta), undefined, this.cdr).subscribe({
      next: (r) => {
        asignar(r);
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
    if (a?.status === 403) return 'No tienes permiso para ver los reportes financieros.';
    if (a?.status === 400) return a.error?.message ?? 'Rango de fechas inválido';
    return 'Error de conexión';
  }
}
