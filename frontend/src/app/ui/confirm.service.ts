import { Injectable, signal } from '@angular/core';

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
}

/**
 * Confirmación de acciones destructivas con el diálogo del sistema UI.
 * Una única instancia se monta en `app.html` (`ui-confirm-host`); los
 * módulos sólo hacen `await this.confirmacion.abrir({...})` y reciben true/false.
 */
@Injectable({ providedIn: 'root' })
export class UiConfirmService {
  readonly peticion = signal<Confirmacion | null>(null);
  private resolver: ((ok: boolean) => void) | null = null;

  abrir(opciones: Confirmacion): Promise<boolean> {
    // Si ya había un diálogo abierto se cancela, para no dejar su promesa colgada.
    this.cerrar(false);
    this.peticion.set(opciones);
    return new Promise<boolean>(res => {
      this.resolver = res;
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
    this.resolver = null;
    res?.(ok);
  }
}
