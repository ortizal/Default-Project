import { Component, ChangeDetectorRef } from '@angular/core';
import { Api } from '../core/api';
import { DashboardData } from '../core/models';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
  imports: [MatCardModule, MatIconModule, MatProgressSpinnerModule],
})
export class DashboardComponent {
  d: DashboardData | null = null;
  cargando = true;

  constructor(
    private readonly api: Api,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.api.get<DashboardData>('/reportes/dashboard').subscribe({
      next: (r) => {
        this.d = r;
        this.cargando = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.cargando = false;
        this.cdr.detectChanges();
      },
    });
  }
}