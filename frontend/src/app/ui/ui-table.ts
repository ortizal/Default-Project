import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Envoltorio único de tablas: scroll controlado, densidad y estados
 * loading/error. Las filas se proyectan en <thead>/<tbody>.
 */
@Component({
  selector: 'ui-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading()) {
      <div #wrap class="tbl-wrap" tabindex="0">
        <div class="tbl-state loading">
          <span class="spinner"></span>
          {{ loadingText() }}
        </div>
      </div>
    } @else if (error()) {
      <div #wrap class="tbl-wrap" tabindex="0">
        <div class="tbl-state error">{{ error }}</div>
      </div>
    } @else {
      <div #wrap class="tbl-wrap" tabindex="0">
        <table class="tbl" [class.dense]="dense()">
          <ng-content select="thead" />
          <ng-content select="tbody" />
        </table>
        <ng-content />
      </div>
    }
  `,
})
export class UiTableComponent {
  readonly loading = input(false);
  readonly error = input('');
  readonly dense = input(false);
  readonly scrollable = input(true);
  readonly loadingText = input('Cargando…');
}
