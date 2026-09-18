import { Component } from '@angular/core';
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

  constructor(private readonly api: Api) {
    this.api.get<DashboardData>('/reportes/dashboard').subscribe({
      next: (r) => {
        this.d = r;
        this.cargando = false;
      },
      error: () => {
        this.cargando = false;
      },
    });
  }
}