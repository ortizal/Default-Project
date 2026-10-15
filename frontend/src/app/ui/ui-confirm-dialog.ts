import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';
import { UiModalComponent } from './ui-modal';
import { UiButtonComponent } from './ui-button';
import { ConfirmCampo } from './confirm.service';

/**
 * Confirmación obligatoria para acciones destructivas
 * (eliminar, cancelar cita, desactivar usuario…). Con `campo` opcional
 * pide además un texto (motivo, detalle…) que se devuelve al confirmar.
 */
@Component({
  selector: 'ui-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ui-modal
      [title]="title()"
      size="narrow"
      [cerrable]="true"
      (closed)="cancelled.emit()"
    >
      <div class="confirm-body">
        <span class="confirm-icon" aria-hidden="true">{{ icon() }}</span>
        <p class="confirm-text">{{ message() }}</p>
      </div>

      @if (campo(); as c) {
        <div class="confirm-field">
          <label class="label" for="confirm-campo">{{ c.etiqueta }}</label>
          <input
            id="confirm-campo"
            class="input sm"
            type="text"
            [attr.placeholder]="c.placeholder ?? null"
            [value]="campoValor()"
            [attr.aria-required]="c.requerido ? 'true' : null"
            (input)="campoValor.set($any($event.target).value)"
            (keyup.enter)="confirmarSiPuede()"
          >
          @if (c.requerido && !campoValor().trim()) {
            <p class="hint">Este campo es obligatorio.</p>
          }
        </div>
      }

      <ng-container actions>
        <ui-button variant="ghost" (clicked)="cancelled.emit()">
          {{ cancelText() }}
        </ui-button>
        <ui-button
          [variant]="danger() ? 'danger' : 'primary'"
          [disabled]="bloqueado()"
          (clicked)="confirm.emit()"
        >
          {{ confirmText() }}
        </ui-button>
      </ng-container>
    </ui-modal>
  `,
  imports: [UiModalComponent, UiButtonComponent],
})
export class UiConfirmDialogComponent {
  readonly title = input('Confirmar acción');
  readonly message = input('¿Estás seguro de continuar?');
  readonly confirmText = input('Confirmar');
  readonly cancelText = input('Cancelar');
  readonly danger = input(true);
  readonly icon = input('warning');
  /** Campo de texto opcional (null = diálogo clásico). */
  readonly campo = input<ConfirmCampo | null>(null);
  /** Valor del campo; se escribe con dos vías desde el host. */
  readonly campoValor = model('');

  /** El confirmar queda bloqueado si el campo obligatorio está vacío. */
  readonly bloqueado = computed(() => {
    const c = this.campo();
    return !!c?.requerido && !this.campoValor().trim();
  });

  confirmarSiPuede(): void {
    if (!this.bloqueado()) {
      this.confirm.emit();
    }
  }

  readonly confirm = output<void>();
  /** 'cancel' es evento nativo (HTMLDialogElement), por eso 'cancelled'. */
  readonly cancelled = output<void>();
}
