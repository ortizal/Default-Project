import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Cabecera de página: título, descripción opcional y acciones.
 * Garantiza que todos los módulos arrancan en la misma línea visual.
 */
@Component({
  selector: 'ui-page-header',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-header">
      <div class="page-header-body">
        <h1 class="page-title">{{ title() }}</h1>
        @if (subtitle()) {
          <p class="page-subtitle">{{ subtitle() }}</p>
        }
      </div>
      <div class="page-header-actions">
        <ng-content />
      </div>
    </div>
  `,
})
export class UiPageHeaderComponent {
  readonly title = input('');
  readonly subtitle = input('');
}
