import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { UiConfirmDialogComponent } from './ui-confirm-dialog';
import { UiConfirmService } from './confirm.service';

/**
 * Monta el diálogo de confirmación que corresponda (si hay alguno pendiente).
 * Se instancia una sola vez en `app.html`, así los módulos no repiten markup.
 */
@Component({
  selector: 'ui-confirm-host',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (confirmacion.peticion(); as p) {
      <ui-confirm-dialog
        [title]="p.titulo"
        [message]="p.mensaje"
        [confirmText]="p.confirmarTexto ?? 'Confirmar'"
        [cancelText]="p.cancelarTexto ?? 'Cancelar'"
        [icon]="p.icono ?? 'warning'"
        [danger]="p.peligro ?? true"
        (confirm)="confirmacion.confirmado()"
        (cancelled)="confirmacion.cancelado()"
      />
    }
  `,
  imports: [UiConfirmDialogComponent],
})
export class UiConfirmHostComponent {
  readonly confirmacion = inject(UiConfirmService);
}
