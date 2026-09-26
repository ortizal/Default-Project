import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Estado vacío / sin resultados. */
@Component({
  selector: 'ui-empty-state',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty-state">
      <mat-icon class="empty-icon">{{ icon() }}</mat-icon>
      <div class="empty-title">{{ title() }}</div>
      @if (description()) {
        <div class="empty-desc">{{ description() }}</div>
      }
      <ng-content />
    </div>
  `,
  imports: [MatIconModule],
})
export class UiEmptyStateComponent {
  readonly icon = input('inbox');
  readonly title = input('Sin resultados');
  readonly description = input('');
}
