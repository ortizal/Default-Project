import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import {
  ComprobanteElectronico,
  CuentaPaciente,
  CuentaResumen,
  NuevoMovimientoCuenta,
} from '../core/models';
import { withLoading } from '../core/loading';
import { UiBadgeComponent, UiPageHeaderComponent } from '../ui';
import {
  etiquetaEstadoCuenta,
  etiquetaEstadoTributario,
  etiquetaFormaPago,
  etiquetaMovimiento,
  moneda,
  toneEstadoCuenta,
  toneEstadoTributario,
  toneMovimiento,
} from './util';

const ESTADOS: string[] = ['', 'ABIERTA', 'PARCIAL', 'PAGADA', 'CERRADA', 'ANULADA'];
const TIPOS: string[] = ['CARGO', 'DESCUENTO', 'ABONO', 'ANTICIPO', 'DEVOLUCION'];
const FORMAS: string[] = ['EFECTIVO', 'TRANSFERENCIA', 'TARJETA', 'OTRO'];

/**
 * Cuentas de los pacientes (Fase F, §3.3): listado con saldo, detalle
 * con el libro de movimientos y alta de cargos, descuentos, anticipos,
 * abonos y devoluciones. Cerrar/reabrir la cuenta bloquea los
 * movimientos manuales.
 */
import { MatMenuModule } from '@angular/material/menu';

import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-facturacion-cuentas',
  templateUrl: './cuentas.html',
  imports: [
    MatIconModule,
    MatMenuModule,CommonModule, FormsModule, RouterLink, UiBadgeComponent, UiPageHeaderComponent],
})
export class FacturacionCuentasComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);

  readonly moneda = moneda;
  readonly etiquetaEstadoCuenta = etiquetaEstadoCuenta;
  readonly toneEstadoCuenta = toneEstadoCuenta;
  readonly etiquetaMovimiento = etiquetaMovimiento;
  readonly toneMovimiento = toneMovimiento;
  readonly etiquetaFormaPago = etiquetaFormaPago;
  readonly etiquetaEstadoTributario = etiquetaEstadoTributario;
  readonly toneEstadoTributario = toneEstadoTributario;
  readonly estados = ESTADOS;
  readonly tipos = TIPOS;
  readonly formas = FORMAS;

  puedeEscribir = false;
  puedeEmitir = false;
  comprobante: ComprobanteElectronico | null = null;

  cuentas: CuentaResumen[] = [];
  buscar = '';
  estado = '';
  cargando = false;
  error = '';

  sel: CuentaPaciente | null = null;
  form: NuevoMovimientoCuenta = { tipo: 'CARGO', concepto: '', cantidad: 1 };
  mensaje = '';

  ngOnInit(): void {
    this.puedeEscribir = this.auth.puede('FACTURACION_WRITE');
    this.puedeEmitir = this.auth.puede('FACTURACION_EMITIR');
    this.cargar();
  }

  cargar(): void {
    const params = new URLSearchParams();
    if (this.estado) params.set('estado', this.estado);
    if (this.buscar) params.set('buscar', this.buscar);
    this.error = '';
    withLoading(
      this,
      this.api.get<CuentaResumen[]>(`/facturacion/cuentas?${params.toString()}`),
      undefined,
      this.cdr,
    ).subscribe({
      next: (lista) => {
        this.cuentas = lista;
        if (this.sel) this.abrir(this.sel.pacienteId, false);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = this.msg(err);
        this.cdr.markForCheck();
      },
    });
  }

  abrir(pacienteId: number, mostrarError = true): void {
    this.mensaje = '';
    if (mostrarError) this.error = '';
    this.api.get<CuentaPaciente>(`/facturacion/cuentas/${pacienteId}`).subscribe({
      next: (cuenta) => {
        this.sel = cuenta;
        this.cdr.markForCheck();
      },
      error: (err) => {
        if (mostrarError) this.error = this.msg(err);
        this.cdr.markForCheck();
      },
    });
  }

  cerrarDetalle(): void {
    this.sel = null;
    this.mensaje = '';
    this.cdr.markForCheck();
  }

  agregar(): void {
    if (!this.sel) return;
    this.error = '';
    this.mensaje = '';
    const cuerpo: NuevoMovimientoCuenta = { ...this.form };
    if (cuerpo.tipo === 'CARGO') {
      delete cuerpo.total;
      delete cuerpo.formaPago;
    } else {
      delete cuerpo.precioUnitario;
      delete cuerpo.descuentoPct;
      delete cuerpo.cantidad;
      delete cuerpo.diente;
      delete cuerpo.superficie;
    }
    if (!this.form.formaPago) delete cuerpo.formaPago;
    this.api
      .post<CuentaPaciente>(`/facturacion/cuentas/${this.sel.pacienteId}/movimientos`, cuerpo)
      .subscribe({
        next: (cuenta) => {
          this.sel = cuenta;
          this.form = { tipo: 'CARGO', concepto: '', cantidad: 1 };
          this.mensaje = 'Movimiento registrado.';
          this.cargar();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = this.msg(err);
          this.cdr.markForCheck();
        },
      });
  }

  anular(movimientoId: number): void {
    if (!this.sel) return;
    this.error = '';
    this.api
      .post<CuentaPaciente>(
        `/facturacion/cuentas/${this.sel.pacienteId}/movimientos/${movimientoId}/anular`,
        { motivo: 'Anulado desde el módulo de cuentas' },
      )
      .subscribe({
        next: (cuenta) => {
          this.sel = cuenta;
          this.mensaje = 'Movimiento anulado.';
          this.cargar();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = this.msg(err);
          this.cdr.markForCheck();
        },
      });
  }

  /** Emite la factura electrónica de la cuenta abierta (plan §3.5). */
  emitir(): void {
    if (!this.sel) return;
    this.error = '';
    this.mensaje = '';
    this.api
      .post<ComprobanteElectronico>('/facturacion/comprobantes', { cuentaId: this.sel.id })
      .subscribe({
        next: (comprobante) => {
          this.comprobante = comprobante;
          this.mensaje = comprobante.simulado
            ? `Comprobante de prueba ${comprobante.numeroComprobante} generado localmente. No fue enviado ni autorizado por el SRI.`
            : comprobante.estadoTributario === 'AUTORIZADO'
              ? `Factura ${comprobante.numeroComprobante} autorizada por el SRI.`
              : comprobante.estadoGeneracion === 'ENVIADO'
                ? `Comprobante ${comprobante.numeroComprobante} recibido por el SRI; autorización pendiente.`
                : `Comprobante ${comprobante.numeroComprobante} generado. Estado SRI: ${comprobante.estadoTributario}.`;
          this.cargar();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = this.msg(err);
          this.cdr.markForCheck();
        },
      });
  }

  cambiarEstado(destino: 'CERRADA' | 'ABIERTA' | 'ANULADA'): void {
    if (!this.sel) return;
    this.error = '';
    this.api
      .put<CuentaPaciente>(`/facturacion/cuentas/${this.sel.pacienteId}/estado`, {
        estado: destino,
        motivo: destino === 'ANULADA' ? 'Anulada desde el módulo de cuentas' : null,
      })
      .subscribe({
        next: (cuenta) => {
          this.sel = cuenta;
          this.mensaje = `Cuenta ${destino.toLowerCase()}.`;
          this.cargar();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.error = this.msg(err);
          this.cdr.markForCheck();
        },
      });
  }

  get esCargo(): boolean {
    return this.form.tipo === 'CARGO';
  }

  get requiereFormaPago(): boolean {
    return this.form.tipo === 'ABONO' || this.form.tipo === 'ANTICIPO' || this.form.tipo === 'DEVOLUCION';
  }

  get bloqueada(): boolean {
    return this.sel?.estado === 'CERRADA' || this.sel?.estado === 'ANULADA';
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string; code?: string }; status?: number };
    if (a?.status === 403) return 'No tienes permiso para operar las cuentas.';
    if (a?.status === 400 || a?.status === 409) {
      return a.error?.message ?? 'Revisa los datos del movimiento';
    }
    return 'Error de conexión';
  }
}
