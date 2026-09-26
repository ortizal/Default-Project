import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type UiBadgeTone =
  | 'ok'
  | 'bad'
  | 'danger'
  | 'warn'
  | 'info'
  | 'muted'
  | 'primary'
  | 'accent';

/** Badge de estado: siempre color + texto (nunca color solo). */
@Component({
  selector: 'ui-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="badge" [class]="tone()">
      <ng-content />
    </span>
  `,
})
export class UiBadgeComponent {
  readonly tone = input<UiBadgeTone>('muted');
}
