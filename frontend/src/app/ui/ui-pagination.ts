import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

/** Paginación compacta y consistente para todos los listados. */
@Component({
  selector: 'ui-pagination',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pagination-bar">
      <button
        class="btn sm outline"
        type="button"
        [disabled]="page() <= 1"
        (click)="prev.emit()"
      >
        <mat-icon>chevron_left</mat-icon>
        Anterior
      </button>

      <span class="muted">Página {{ page() }} de {{ pages() }}</span>

      <button
        class="btn sm outline"
        type="button"
        [disabled]="page() >= pages()"
        (click)="next.emit()"
      >
        Siguiente
        <mat-icon>chevron_right</mat-icon>
      </button>
    </div>
  `,
  imports: [MatIconModule],
})
export class UiPaginationComponent {
  readonly page = input(1);
  readonly pages = input(1);

  readonly prev = output<void>();
  readonly next = output<void>();
  readonly pageChange = output<number>();

  readonly canPrev = computed(() => this.page() > 1);
  readonly canNext = computed(() => this.page() < this.pages());
}
