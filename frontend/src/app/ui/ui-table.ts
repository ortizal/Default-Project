import { ChangeDetectionStrategy, Component, ElementRef, OnDestroy, effect, input, signal, viewChild } from '@angular/core';

/**
 * Envoltorio único de tablas: scroll controlado, densidad y estados
 * loading/error. Las filas se proyectan en <thead>/<tbody>.
 */
@Component({
  selector: 'ui-table',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (loading()) {
      <div #wrap class="tbl-wrap">
        <div class="tbl-state loading">
          <span class="spinner"></span>
          {{ loadingText() }}
        </div>
      </div>
    } @else if (error()) {
      <div #wrap class="tbl-wrap">
        <div class="tbl-state error">{{ error() }}</div>
      </div>
    } @else {
      <div
        #wrap
        class="tbl-wrap"
        [class.scrollable]="scrollable()"
        [attr.tabindex]="desplazable() ? 0 : null"
      >
        <table class="tbl" [class.dense]="dense()">
          <ng-content select="thead" />
          <ng-content select="tbody" />
        </table>
        <ng-content />
      </div>
    }
  `,
})
export class UiTableComponent implements OnDestroy {
  readonly loading = input(false);
  readonly error = input('');
  readonly dense = input(false);
  readonly scrollable = input(true);
  readonly loadingText = input('Cargando…');

  /**
   * Un contenedor con scroll horizontal debe poder recorrerse con el teclado
   * (WCAG 2.1.1). Sólo se vuelve focoteable si de verdad se desborda, para no
   * añadir paradas de tabulación de más cuando la tabla cabe.
   */
  readonly desplazable = signal(false);

  private readonly wrap = viewChild<ElementRef<HTMLElement>>('wrap');
  private readonly observer: ResizeObserver | null =
    typeof ResizeObserver === 'undefined'
      ? null
      : new ResizeObserver(() => this.medir());
  private readonly watcher = effect(() => {
    const el = this.wrap()?.nativeElement;
    this.observer?.disconnect();
    this.desplazable.set(false);
    if (!el || !this.observer) return;
    this.observer.observe(el);
    const tabla = el.querySelector('table');
    if (tabla) this.observer.observe(tabla);
    this.medir(el);
  });

  private medir(el?: HTMLElement): void {
    const nodo = el ?? this.wrap()?.nativeElement;
    if (!nodo) return;
    this.desplazable.set(nodo.scrollWidth > nodo.clientWidth + 1);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    this.watcher.destroy();
  }
}
