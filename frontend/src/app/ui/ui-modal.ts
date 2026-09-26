import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Modal estándar: backdrop, foco en la superposición y acciones en el
 * pie. Se cierra con Escape o clic fuera (cerrable = true).
 */
@Component({
  selector: 'ui-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:keydown.escape)': 'onEscape()',
  },
  template: `
    <div class="modal-backdrop" (click)="cerrable() && close.emit()" role="presentation">
      <div
        class="modal"
        [class.narrow]="size() === 'narrow'"
        [class.wide]="size() === 'wide'"
        role="dialog"
        aria-modal="true"
        [attr.aria-label]="title() || null"
        (click)="$event.stopPropagation()"
      >
        @if (title()) {
          <h2>{{ title() }}</h2>
        }
        @if (subtitle()) {
          <p class="sub">{{ subtitle() }}</p>
        }
        <ng-content />
        <div class="modal-actions">
          <ng-content select="[actions]" />
        </div>
      </div>
    </div>
  `,
})
export class UiModalComponent {
  readonly title = input('');
  readonly subtitle = input('');
  readonly size = input<'narrow' | 'default' | 'wide'>('default');
  readonly cerrable = input(true);

  readonly close = output<void>();

  onEscape(): void {
    if (this.cerrable()) this.close.emit();
  }
}
