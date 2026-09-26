import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Card estándar: fondo, borde, radio y padding del sistema. */
@Component({
  selector: 'ui-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (title() || subtitle() || actions()) {
      <div class="card-header">
        <div>
          @if (title()) {
            <h2 class="card-title">{{ title() }}</h2>
          }
          @if (subtitle()) {
            <p class="page-subtitle">{{ subtitle() }}</p>
          }
        </div>
        <div class="page-header-actions">
          <ng-content select="[actions]" />
        </div>
      </div>
    }
    <ng-content />
  `,
})
export class UiCardComponent {
  readonly title = input('');
  readonly subtitle = input('');
  /** true cuando se proyecta un bloque [actions] sin título. */
  readonly actions = input(false);
}
