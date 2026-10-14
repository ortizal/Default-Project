import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { Caja, NuevoMovimientoCaja } from '../core/models';
import { UiPageHeaderComponent } from '../ui';
import {
  etiquetaCaja,
  etiquetaFormaPago,
  etiquetaMovimiento,
  moneda,
} from './util';

const TIPOS_CAJA: string[] = ['INGRESO', 'EGRESO'];
const FORMAS: string[] = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'OTRO'];

/**
 * Caja del consultorio (Fase F, §3.4): apertura con fondo inicial,
 * ingresos y egresos por forma de pago, cierre con efectivo declarado y
 * conciliación por diferencia, más el historial de sesiones.
 */
import { MatMenuModule } from '@angular/material/menu';

import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-facturacion-caja',
  templateUrl: './caja.html',
  imports: [
    MatIconModule,
    MatMenuModule,CommonModule, FormsModule, UiPageHeaderComponent],
})
export class FacturacionCajaComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);

  readonly moneda = moneda;
  readonly etiquetaCaja = etiquetaCaja;
  readonly etiquetaFormaPago = etiquetaFormaPago;
  readonly etiquetaMovimiento = etiquetaMovimiento;
  readonly tipos = TIPOS_CAJA;
  readonly formas = FORMAS;

  puedeEscribir = false;
  puedeCerrar = false;

  caja: Caja | null = null;
  historial: Caja[] = [];
  cargando = false;
  error = '';
  mensaje = '';

  apertura = { nombre: 'Caja', montoApertura: 0, observaciones: '' };
  mov: NuevoMovimientoCaja = { tipo: 'INGRESO', concepto: '', formaPago: 'EFECTIVO', monto: 0 };
  cierre = { montoDeclarado: 0, observaciones: '' };

  ngOnInit(): void {
    this.puedeEscribir = this.auth.puede('CAJA_WRITE');
    this.puedeCerrar = this.auth.puede('CAJA_CERRAR');
    this.cargar();
  }

  cargar(): void {
    this.error = '';
    this.mensaje = '';
    this.cargando = true;
    this.api.get<Caja>('/facturacion/cajas/actual').subscribe({
      next: (caja) => {
        this.caja = caja;
        this.cargando = false;
        this.cdr.markForCheck();
        this.cargarHistorial();
      },
      error: (err) => {
        const status = (err as { status?: number })?.status;
        if (status === 404) {
          this.caja = null;
          this.cargando = false;
          this.cdr.markForCheck();
          this.cargarHistorial();
          return;
        }
        this.error = this.msg(err);
        this.cargando = false;
        this.cdr.markForCheck();
      },
    });
  }

  private cargarHistorial(): void {
    this.api.get<Caja[]>('/facturacion/cajas').subscribe({
      next: (lista) => {
        this.historial = lista;
        this.cdr.markForCheck();
      },
      error: () => {
        /* el historial es secundario */
      },
    });
  }

  abrir(): void {
    this.error = '';
    this.api.post<Caja>('/facturacion/cajas', this.apertura).subscribe({
      next: (caja) => {
        this.caja = caja;
        this.mensaje = 'Caja abierta.';
        this.cargarHistorial();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = this.msg(err);
        this.cdr.markForCheck();
      },
    });
  }

  cerrar(): void {
    if (!this.caja) return;
    this.error = '';
    this.api
      .put<Caja>('/facturacion/cajas/actual/cerrar', {
        montoDeclarado: this.cierre.montoDeclarado,
        observaciones: this.cierre.observaciones || null,
      })
      .subscribe({
        next: (caja) => {
          this.caja = caja;
          this.mensaje = `Caja cerrada con diferencia ${this.moneda(caja.diferencia)}.`;
          this.cargarHistorial();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = this.msg(err);
          this.cdr.markForCheck();
        },
      });
  }

  agregar(): void {
    if (!this.caja) return;
    this.error = '';
    this.api
      .post<Caja>(`/facturacion/cajas/${this.caja.id}/movimientos`, this.mov)
      .subscribe({
        next: (caja) => {
          this.caja = caja;
          this.mov = { tipo: 'INGRESO', concepto: '', formaPago: 'EFECTIVO', monto: 0 };
          this.mensaje = 'Movimiento registrado.';
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = this.msg(err);
          this.cdr.markForCheck();
        },
      });
  }

  anular(movimientoId: number): void {
    if (!this.caja) return;
    this.error = '';
    this.api
      .post<Caja>(
        `/facturacion/cajas/${this.caja.id}/movimientos/${movimientoId}/anular?motivo=` +
          encodeURIComponent('Anulado desde el módulo de caja'),
        {},
      )
      .subscribe({
        next: (caja) => {
          this.caja = caja;
          this.mensaje = 'Movimiento anulado.';
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = this.msg(err);
          this.cdr.markForCheck();
        },
      });
  }

  get abierta(): boolean {
    return this.caja?.estado === 'ABIERTA';
  }

  get diferenciaEsperada(): number {
    if (!this.caja) return 0;
    return Number(this.cierre.montoDeclarado || 0) - Number(this.caja.totales.esperadoEfectivo);
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    if (a?.status === 403) return 'No tienes permiso para operar la caja.';
    if (a?.status === 400 || a?.status === 409) return a.error?.message ?? 'Revisa los datos';
    return 'Error de conexión';
  }
}
