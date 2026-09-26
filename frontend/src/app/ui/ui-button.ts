import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export type UiButtonVariant = 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost';
export type UiButtonSize = 'sm' | 'md' | 'lg';

/** Botón único del sistema: centraliza variante, tamaño y estados. */
@Component({
  selector: 'ui-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      class="btn"
      [class.primary]="variant() === 'primary'"
      [class.secondary]="variant() === 'secondary'"
      [class.outline]="variant() === 'outline'"
      [class.danger]="variant() === 'danger'"
      [class.ghost]="variant() === 'ghost'"
      [class.sm]="size() === 'sm'"
      [class.small]="size() === 'sm'"
      [class.lg]="size() === 'lg'"
      [class.block]="block()"
      [class.loading]="loading()"
      [type]="type()"
      [disabled]="disabled() || loading()"
      [attr.aria-busy]="loading() || null"
      [attr.aria-label]="ariaLabel()"
      [attr.title]="title() || ariaLabel()"
      (click)="onClick.emit($event)"
    >
      @if (icon()) {
        <mat-icon>{{ icon() }}</mat-icon>
      }
      <ng-content />
    </button>
  `,
  imports: [MatIconModule],
})
export class UiButtonComponent {
  readonly variant = input<UiButtonVariant>('secondary');
  readonly size = input<UiButtonSize>('md');
  readonly type = input<'button' | 'submit' | 'reset'>('button');
  readonly icon = input<string | null>(null);
  readonly block = input(false);
  readonly loading = input(false);
  readonly disabled = input(false);
  readonly ariaLabel = input<string | null>(null);
  readonly title = input<string | null>(null);

  readonly onClick = output<MouseEvent>();
}
