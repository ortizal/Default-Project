import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { UiModalComponent } from './ui-modal';
import { UiButtonComponent } from './ui-button';

/**
 * Confirmación obligatoria para acciones destructivas
 * (eliminar, cancelar cita, desactivar usuario…).
 */
@Component({
  selector: 'ui-confirm-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ui-modal
      [title]="title()"
      size="narrow"
      [cerrable]="true"
      (close)="cancel.emit()"
    >
      <div class="confirm-body">
        <span class="confirm-icon" aria-hidden="true">{{ icon() }}</span>
        <p class="confirm-text">{{ message() }}</p>
      </div>

      <ng-container actions>
        <ui-button variant="ghost" (onClick)="cancel.emit()">
          {{ cancelText() }}
        </ui-button>
        <ui-button
          [variant]="danger() ? 'danger' : 'primary'"
          (onClick)="confirm.emit()"
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

  readonly confirm = output<void>();
  readonly cancel = output<void>();
}
