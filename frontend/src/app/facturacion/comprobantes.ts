import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import {
  ComprobanteElectronico,
  ComprobanteResumen,
  EstadoTributario,
  Page,
  paginasDe,
  totalDe,
} from '../core/models';
import { withLoading } from '../core/loading';
import {
  UiBadgeComponent,
  UiModalComponent,
  UiPageHeaderComponent,
  UiPaginationComponent,
  UiTableComponent,
} from '../ui';
import {
  etiquetaEstadoGeneracion,
  etiquetaEstadoTributario,
  etiquetaTipoComprobante,
  moneda,
  toneEstadoGeneracion,
  toneEstadoTributario,
} from './util';

 /** Todo comprobante (fila o detalle) expone estos datos para las acciones. */
interface Identificado {
  id: number;
  numeroComprobante: string;
  claveAcceso: string;
  tipo: string;
  estadoTributario: EstadoTributario;
  simulado: boolean;
}

/** Línea de la nota de crédito dentro del diálogo (plan §3.9). */
interface LineaDevolucion {
  linea: number;
  descripcion: string;
  facturada: number;
  /** Lo que se cobró por esa línea, IVA incluido. */
  importe: number;
  cantidad: number;
}

/** Estado del diálogo «Emitir nota de crédito». */
interface DialogoNota {
  comprobante: ComprobanteElectronico;
  motivo: string;
  parcial: boolean;
  lineas: LineaDevolucion[];
  error: string;
  enviando: boolean;
}

/** Estado del diálogo «Anular comprobante» (plan §3.10). */
interface DialogoAnulacion {
  comprobante: Identificado;
  motivo: string;
  error: string;
  enviando: boolean;
}

const TIPOS: string[] = ['', '01', '04', '05', '06', '07'];
const ESTADOS_TRIBUTARIOS: string[] = [
  '',
  'PENDIENTE',
  'AUTORIZADO',
  'DEVUELTO',
  'NO_AUTORIZADO',
  'ANULADO',
];
const ESTADOS_GENERACION: string[] = ['', 'BORRADOR', 'GENERADO', 'FIRMADO', 'ENVIADO', 'ERROR'];

/**
 * Bandeja de comprobantes electrónicos (Fase G, §3.7): filtros por
 * fecha, texto libre (número, clave, cédula o paciente), tipo y estados;
 * acciones de ver, descargar XML/RIDE, reenviar, consultar al SRI y
 * revisar la auditoría. Nunca se borra un comprobante emitido (§3.10).
 *
 * Fase H.2 (§3.9/§3.10): sobre una factura autorizada se emite una
 * **nota de crédito** —total o parcial, eligiendo líneas— y cualquier
 * comprobante autorizado se **anula** con motivo; ambas acciones piden
 * `FACTURACION_ANULAR` y se resuelven en diálogos dentro de esta página.
 */
@Component({
  selector: 'app-facturacion-comprobantes',
  templateUrl: './comprobantes.html',
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatMenuModule,
    UiBadgeComponent,
    UiModalComponent,
    UiPageHeaderComponent,
    UiPaginationComponent,
    UiTableComponent,
  ],
})
export class FacturacionComprobantesComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly moneda = moneda;
  readonly etiquetaEstadoTributario = etiquetaEstadoTributario;
  readonly toneEstadoTributario = toneEstadoTributario;
  readonly etiquetaEstadoGeneracion = etiquetaEstadoGeneracion;
  readonly toneEstadoGeneracion = toneEstadoGeneracion;
  readonly etiquetaTipoComprobante = etiquetaTipoComprobante;
  readonly tipos = TIPOS;
  readonly estadosTributarios = ESTADOS_TRIBUTARIOS;
  readonly estadosGeneracion = ESTADOS_GENERACION;

  puedeEmitir = false;
  puedeAnular = false;
  readonly anulacionSriDisponible = false;

  rows: ComprobanteResumen[] = [];
  q = '';
  desde = '';
  hasta = '';
  tipo = '';
  estado = '';
  generacion = '';
  pagina = 0;
  totalPaginas = 1;
  totalElementos = 0;

  cargando = false;
  error = '';
  mensaje = '';

  sel: ComprobanteElectronico | null = null;

  /** Abierto sólo cuando el usuario pidió una nota de crédito. */
  nota: DialogoNota | null = null;
  /** Abierto sólo cuando el usuario pidió anular. */
  anulacion: DialogoAnulacion | null = null;

  ngOnInit(): void {
    this.puedeEmitir = this.auth.puede('FACTURACION_EMITIR');
    this.puedeAnular = this.auth.puede('FACTURACION_ANULAR');
    this.cargar();
  }

  buscar(): void {
    this.pagina = 0;
    this.cargar();
  }

  limpiar(): void {
    this.q = '';
    this.desde = '';
    this.hasta = '';
    this.tipo = '';
    this.estado = '';
    this.generacion = '';
    this.pagina = 0;
    this.error = '';
    this.cargar();
  }

  cargar(): void {
    const params: string[] = [];
    if (this.q.trim()) params.push(`q=${encodeURIComponent(this.q.trim())}`);
    if (this.desde) params.push(`desde=${this.desde}`);
    if (this.hasta) params.push(`hasta=${this.hasta}`);
    if (this.tipo) params.push(`tipo=${this.tipo}`);
    if (this.estado) params.push(`estado=${this.estado}`);
    if (this.generacion) params.push(`generacion=${this.generacion}`);
    params.push(`page=${this.pagina}`, 'size=20');
    this.error = '';
    withLoading(
      this,
      this.api.get<Page<ComprobanteResumen>>(`/facturacion/comprobantes?${params.join('&')}`),
      undefined,
      this.cdr,
    ).subscribe({
      next: (r) => {
        this.rows = r.content ?? [];
        this.totalPaginas = paginasDe(r);
        this.totalElementos = totalDe(r);
        if (this.pagina >= this.totalPaginas) {
          this.pagina = this.totalPaginas - 1;
        }
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  ir(p: number): void {
    if (p < 0 || p >= this.totalPaginas) return;
    this.pagina = p;
    this.cargar();
  }

  abrir(id: number, mostrarError = true): void {
    this.mensaje = '';
    if (mostrarError) this.error = '';
    this.api.get<ComprobanteElectronico>(`/facturacion/comprobantes/${id}`).subscribe({
      next: (comprobante) => {
        this.sel = comprobante;
        this.cdr.markForCheck();
      },
      error: (e) => {
        if (mostrarError) this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  cerrarDetalle(): void {
    this.sel = null;
    this.cdr.markForCheck();
  }

  descargarXml(comprobante: Identificado): void {
    this.error = '';
    this.api.getBlob(`/facturacion/comprobantes/${comprobante.id}/xml`).subscribe({
      next: (blob) => this.guardar(blob, `comprobante-${comprobante.numeroComprobante}.xml`),
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  descargarRide(comprobante: Identificado): void {
    this.error = '';
    if (!this.puedeDescargarRide(comprobante)) {
      this.error = 'El RIDE solo está disponible después de una autorización real del SRI.';
      return;
    }
    this.api.getBlob(`/facturacion/comprobantes/${comprobante.id}/ride`).subscribe({
      next: (blob) => this.guardar(blob, `ride-${comprobante.numeroComprobante}.pdf`),
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  puedeDescargarRide(comprobante: Identificado): boolean {
    return !comprobante.simulado && comprobante.estadoTributario === 'AUTORIZADO';
  }

  reenviar(comprobante: Identificado): void {
    this.accion(comprobante, 'reenviar', 'Comprobante reenviado al SRI.');
  }

  consultar(comprobante: Identificado): void {
    this.accion(comprobante, 'consultar', 'Consulta enviada al SRI.');
  }

  /** Reenvío / consulta al SRI desde la fila o desde el detalle. */
  private accion(comprobante: Identificado, accion: string, exito: string): void {
    this.error = '';
    this.mensaje = '';
    this.api
      .post<ComprobanteElectronico>(`/facturacion/comprobantes/${comprobante.id}/${accion}`, {})
      .subscribe({
        next: (actualizado) => {
          if (this.sel?.id === actualizado.id) this.sel = actualizado;
          this.mensaje =
            accion === 'consultar' && actualizado.estadoTributario === 'AUTORIZADO'
              ? 'El SRI autorizó el comprobante.'
              : exito;
          this.cargar();
          this.cdr.markForCheck();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  auditoria(comprobante: Identificado): void {
    void this.router.navigate(['/auditoria'], {
      queryParams: {
        entidad: 'COMPROBANTE_ELECTRONICO',
        entidadId: comprobante.id,
        modulo: 'FACTURACION',
      },
    });
  }

  // ------------------------------------------------------------------
  // Fase H.2 — nota de crédito (§3.9) y anulación (§3.10)
  // ------------------------------------------------------------------

  /** Sólo una factura autorizada admite nota de crédito. */
  esFacturaAutorizada(c: Identificado): boolean {
    return c.tipo === '01' && c.estadoTributario === 'AUTORIZADO';
  }

  /** Facturas y notas de crédito autorizados admiten anulación. */
  esAnulable(c: Identificado): boolean {
    return (c.tipo === '01' || c.tipo === '04') && c.estadoTributario === 'AUTORIZADO';
  }

  /**
   * Abre la nota de crédito: trae el detalle con las líneas del XML para
   * poder elegir una devolución parcial.
   */
  abrirNotaCredito(comprobante: Identificado): void {
    this.error = '';
    this.mensaje = '';
    this.anulacion = null;
    this.nota = null;
    this.api.get<ComprobanteElectronico>(`/facturacion/comprobantes/${comprobante.id}`).subscribe({
      next: (detalle) => {
        this.sel = detalle;
        if (detalle.tipo !== '01') {
          this.error = 'La nota de crédito sólo puede emitirse sobre una factura.';
        } else if (detalle.estadoTributario !== 'AUTORIZADO') {
          this.error = 'Sólo se emite una nota de crédito sobre una factura autorizada.';
        } else {
          this.nota = {
            comprobante: detalle,
            motivo: '',
            parcial: false,
            lineas: (detalle.detalles ?? []).map((l) => ({
              linea: l.linea,
              descripcion: l.descripcion,
              facturada: l.cantidad,
              importe: l.total + (l.impuesto ?? 0),
              cantidad: 0,
            })),
            error: '',
            enviando: false,
          };
        }
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  /** Anulación: pide el motivo y no borra nunca la fila (§3.10). */
  abrirAnulacion(comprobante: Identificado): void {
    this.error = '';
    this.mensaje = '';
    this.nota = null;
    this.anulacion = { comprobante, motivo: '', error: '', enviando: false };
    this.cdr.markForCheck();
  }

  cerrarDialogo(): void {
    this.nota = null;
    this.anulacion = null;
    this.cdr.markForCheck();
  }

  /** Unidades marcadas en la devolución parcial. */
  lineasMarcadas(n: DialogoNota): number {
    return n.lineas.filter((l) => l.cantidad > 0).length;
  }

  /** Importe que devuelve la línea según lo que se marque. */
  importeLinea(l: LineaDevolucion): number {
    if (l.facturada <= 0) return 0;
    return Math.round(((l.importe * l.cantidad) / l.facturada) * 100) / 100;
  }

  /** Total que cubrirá la nota: todo el comprobante o lo marcado. */
  devuelto(n: DialogoNota): number {
    if (!n.parcial) return n.comprobante.total;
    const suma = n.lineas.reduce((acc, l) => acc + this.importeLinea(l), 0);
    return Math.round(suma * 100) / 100;
  }

  emitirNotaCredito(): void {
    const n = this.nota;
    if (!n || n.enviando) return;
    const motivo = n.motivo.trim();
    if (motivo.length < 5 || motivo.length > 300) {
      n.error = 'El motivo debe tener entre 5 y 300 caracteres.';
      return;
    }
    const marcadas = n.lineas.filter((l) => l.cantidad > 0);
    if (n.parcial && marcadas.length === 0) {
      n.error = 'Indica cuántas unidades devolver en al menos una línea.';
      return;
    }
    const exceso = marcadas.find((l) => l.cantidad > l.facturada);
    if (exceso) {
      n.error = `La línea ${exceso.linea} sólo tiene ${exceso.facturada} unidades.`;
      return;
    }
    n.error = '';
    n.enviando = true;
    this.api
      .post<ComprobanteElectronico>('/facturacion/notas-credito', {
        comprobanteId: n.comprobante.id,
        motivo,
        lineas: n.parcial ? marcadas.map((l) => ({ linea: l.linea, cantidad: l.cantidad })) : null,
      })
      .subscribe({
        next: (nota) => {
          this.nota = null;
          this.cargar();
          this.abrir(nota.id);
          this.mensaje = `Nota de crédito ${nota.numeroComprobante} emitida.`;
          this.cdr.markForCheck();
        },
        error: (e) => {
          n.enviando = false;
          n.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  confirmarAnulacion(): void {
    const a = this.anulacion;
    if (!a || a.enviando) return;
    const motivo = a.motivo.trim();
    if (motivo.length < 5 || motivo.length > 500) {
      a.error = 'El motivo debe tener entre 5 y 500 caracteres.';
      return;
    }
    a.error = '';
    a.enviando = true;
    this.api
      .post<ComprobanteElectronico>(`/facturacion/comprobantes/${a.comprobante.id}/anular`, { motivo })
      .subscribe({
        next: (anulado) => {
          const abierto = this.sel?.id === anulado.id;
          this.anulacion = null;
          this.cargar();
          if (abierto) this.abrir(anulado.id);
          this.mensaje = `Comprobante ${anulado.numeroComprobante} anulado.`;
          this.cdr.markForCheck();
        },
        error: (e) => {
          a.enviando = false;
          a.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  private guardar(blob: Blob, nombre: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    a.click();
    URL.revokeObjectURL(url);
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string; code?: string }; status?: number };
    if (a?.status === 403) return 'No tienes permiso para ver los comprobantes.';
    if (a?.status === 404) return 'El comprobante no existe.';
    if (a?.status === 400 || a?.status === 409 || a?.status === 501) {
      return a.error?.message ?? 'No se pudo completar la operación.';
    }
    return 'Error de conexión';
  }
}
