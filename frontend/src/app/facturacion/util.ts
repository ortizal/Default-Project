import { UiBadgeTone } from '../ui';
import { EstadoCaja, EstadoCuenta, FormaPago, TipoMovimientoCuenta } from '../core/models';

/** Utilidades visuales compartidas del módulo de finanzas (Fase F). */

export function moneda(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) return '—';
  return '$ ' + Number(valor).toFixed(2);
}

export function etiquetaEstadoCuenta(estado: EstadoCuenta): string {
  switch (estado) {
    case 'ABIERTA':
      return 'Abierta';
    case 'PARCIAL':
      return 'Pago parcial';
    case 'PAGADA':
      return 'Pagada';
    case 'CERRADA':
      return 'Cerrada';
    default:
      return 'Anulada';
  }
}

export function toneEstadoCuenta(estado: EstadoCuenta): UiBadgeTone {
  switch (estado) {
    case 'PAGADA':
      return 'ok';
    case 'PARCIAL':
      return 'info';
    case 'CERRADA':
      return 'muted';
    case 'ANULADA':
      return 'danger';
    default:
      return 'warn';
  }
}

export function etiquetaMovimiento(tipo: TipoMovimientoCuenta): string {
  switch (tipo) {
    case 'CARGO':
      return 'Cargo';
    case 'DESCUENTO':
      return 'Descuento';
    case 'ABONO':
      return 'Abono';
    case 'ANTICIPO':
      return 'Anticipo';
    default:
      return 'Devolución';
  }
}

export function toneMovimiento(tipo: TipoMovimientoCuenta): UiBadgeTone {
  switch (tipo) {
    case 'CARGO':
      return 'info';
    case 'DESCUENTO':
      return 'warn';
    case 'DEVOLUCION':
      return 'danger';
    default:
      return 'ok';
  }
}

/** El signo que aporta cada movimiento al saldo. */
export function efectoMovimiento(tipo: TipoMovimientoCuenta, total: number): number {
  const signo =
    tipo === 'CARGO' || tipo === 'DEVOLUCION' ? 1 : -1;
  return signo * Number(total);
}

export function etiquetaFormaPago(forma: FormaPago | string | null): string {
  if (!forma) return '—';
  switch (forma) {
    case 'EFECTIVO':
      return 'Efectivo';
    case 'TRANSFERENCIA':
      return 'Transferencia';
    case 'TARJETA':
      return 'Tarjeta';
    default:
      return 'Otro';
  }
}

export function etiquetaCaja(estado: EstadoCaja): string {
  return estado === 'ABIERTA' ? 'Abierta' : 'Cerrada';
}

/** Estados del comprobante electrónico (plan §3.8). */

export function etiquetaEstadoTributario(estado: string | null | undefined): string {
  switch (estado) {
    case 'AUTORIZADO':
      return 'Autorizado';
    case 'PENDIENTE':
      return 'Pendiente';
    case 'DEVUELTO':
      return 'Devuelto';
    case 'NO_AUTORIZADO':
      return 'No autorizado';
    case 'ANULADO':
      return 'Anulado';
    default:
      return '—';
  }
}

export function toneEstadoTributario(estado: string | null | undefined): UiBadgeTone {
  switch (estado) {
    case 'AUTORIZADO':
      return 'ok';
    case 'PENDIENTE':
      return 'info';
    case 'DEVUELTO':
    case 'NO_AUTORIZADO':
      return 'danger';
    case 'ANULADO':
      return 'muted';
    default:
      return 'warn';
  }
}

export function etiquetaEstadoGeneracion(estado: string | null | undefined): string {
  switch (estado) {
    case 'BORRADOR':
      return 'Borrador';
    case 'GENERADO':
      return 'Generado';
    case 'FIRMADO':
      return 'Firmado';
    case 'ENVIADO':
      return 'Enviado';
    case 'ERROR':
      return 'Error';
    default:
      return '—';
  }
}

export function toneEstadoGeneracion(estado: string | null | undefined): UiBadgeTone {
  switch (estado) {
    case 'ERROR':
      return 'danger';
    case 'ENVIADO':
      return 'ok';
    case 'FIRMADO':
    case 'GENERADO':
      return 'info';
    default:
      return 'muted';
  }
}

export function etiquetaTipoComprobante(tipo: string | null | undefined): string {
  switch (tipo) {
    case '01':
      return 'Factura';
    case '04':
      return 'Nota de crédito';
    case '05':
      return 'Nota de débito';
    case '06':
      return 'Guía de remisión';
    case '07':
      return 'Retención';
    default:
      return '—';
  }
}
