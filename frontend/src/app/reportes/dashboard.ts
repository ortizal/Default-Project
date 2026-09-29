import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { Api } from '../core/api';
import { DashboardData } from '../core/models';
import { withLoading } from '../core/loading';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UiPageHeaderComponent } from '../ui';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
  imports: [MatCardModule, MatIconModule, MatProgressSpinnerModule, UiPageHeaderComponent],
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);

  d: DashboardData | null = null;
  cargando = true;

  ngOnInit(): void {
    withLoading(
      this,
      this.api.get<DashboardData>('/reportes/dashboard'),
      undefined,
      this.cdr,
    ).subscribe({
      next: (r) => {
        this.d = r;
        this.cdr.markForCheck();
      },
      error: () => this.cdr.markForCheck(),
    });
  }
}
