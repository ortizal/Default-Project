import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Api } from '../core/api';
import { DashboardData } from '../core/models';
import { withLoading } from '../core/loading';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
  imports: [MatCardModule, MatIconModule, MatProgressSpinnerModule],
})
export class DashboardComponent implements OnInit {
  d: DashboardData | null = null;
  cargando = true;

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    withLoading(this, this.api.get<DashboardData>('/reportes/dashboard'), undefined, this.cdr).subscribe({
      next: (r) => { this.d = r; this.cdr.markForCheck(); },
      error: () => this.cdr.markForCheck(),
    });
  }
}