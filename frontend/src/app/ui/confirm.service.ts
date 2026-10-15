import { Injectable, signal } from '@angular/core';

/** Campo de texto opcional del diálogo (p. ej. el motivo de una anulación). */
export interface ConfirmCampo {
  /** Etiqueta visible sobre el input. */
  etiqueta: string;
  /** Texto de ayuda dentro del input. */
  placeholder?: string;
  /** Si es true el botón de confirmar queda deshabilitado mientras esté vacío. */
  requerido?: boolean;
  /** Valor inicial. */
  valor?: string;
}

export interface Confirmacion {
  /** Título del diálogo (encabezado del modal). */
  titulo: string;
  /** Texto de la pregunta. */
  mensaje: string;
  /** Etiqueta del botón de confirmación. */
  confirmarTexto?: string;
  /** Etiqueta del botón de cancelar. */
  cancelarTexto?: string;
  /** Icono de Material usado en el cuerpo (verbo de aviso). */
  icono?: string;
  /** false pinta el botón principal en verde en vez de rojo. */
  peligro?: boolean;
  /** Campo de texto opcional (motivo, detalle…). */
  campo?: ConfirmCampo;
}

/**
 * Confirmación de acciones destructivas con el diálogo del sistema UI.
 * Una única instancia se monta en `app.html` (`ui-confirm-host`); los
 * módulos sólo hacen `await this.confirmacion.abrir({...})` y reciben true/false.
 * Cuando el diálogo lleva campo de texto se usa `abrirConCampo({...})` y la
 * promesa resuelve el texto escrito (null si cancela o si sale vacío).
 */
@Injectable({ providedIn: 'root' })
export class UiConfirmService {
  readonly peticion = signal<Confirmacion | null>(null);
  /** Valor vivo del campo de texto (lo lee la promesa al confirmar). */
  readonly texto = signal('');
  private resolver: ((ok: boolean) => void) | null = null;
  private resolverTexto: ((valor: string | null) => void) | null = null;

  abrir(opciones: Confirmacion): Promise<boolean> {
    // Si ya había un diálogo abierto se cancela, para no dejar su promesa colgada.
    this.cerrar(false);
    this.resolverTexto = null;
    this.peticion.set(opciones);
    return new Promise<boolean>(res => {
      this.resolver = res;
    });
  }

  /** Igual que {@link #abrir} pero resuelve el texto del campo (null al cancelar). */
  abrirConCampo(opciones: Confirmacion): Promise<string | null> {
    this.cerrar(false);
    this.resolver = null;
    this.texto.set(opciones.campo?.valor ?? '');
    this.peticion.set(opciones);
    return new Promise<string | null>(res => {
      this.resolverTexto = res;
    });
  }

  confirmado(): void {
    this.cerrar(true);
  }

  cancelado(): void {
    this.cerrar(false);
  }

  private cerrar(ok: boolean): void {
    this.peticion.set(null);
    const res = this.resolver;
    const resTexto = this.resolverTexto;
    this.resolver = null;
    this.resolverTexto = null;
    res?.(ok);
    resTexto?.(ok ? this.texto().trim() || null : null);
  }
}
