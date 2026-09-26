import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Indicador de carga reutilizable (spinner + texto). */
@Component({
  selector: 'ui-loading',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="loading-block" role="status" aria-live="polite">
      <span class="spinner" aria-hidden="true"></span>
      <span>{{ text() }}</span>
    </div>
  `,
})
export class UiLoadingComponent {
  readonly text = input('Cargando…');
}
