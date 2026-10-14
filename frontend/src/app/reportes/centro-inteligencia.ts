import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../core/auth.service';
import { UiPageHeaderComponent } from '../ui';

interface PanelInteligencia {
  nombre: string;
  descripcion: string;
  ruta: string;
  permiso: string;
  icono: string;
}

@Component({
  selector: 'app-centro-inteligencia',
  templateUrl: './centro-inteligencia.html',
  styleUrl: './centro-inteligencia.scss',
  imports: [RouterLink, MatIconModule, UiPageHeaderComponent],
})
export class CentroInteligenciaComponent {
  private readonly auth = inject(AuthService);

  readonly paneles: PanelInteligencia[] = [
    {
      nombre: 'Resumen operativo',
      descripcion: 'Agenda, citas, atención y actividad del consultorio.',
      ruta: '/dashboard/operacion',
      permiso: 'REPORTES',
      icono: 'event_available',
    },
    {
      nombre: 'Finanzas',
      descripcion: 'Facturación interna, cobros, saldos y tendencias disponibles para tu perfil.',
      ruta: '/facturacion',
      permiso: 'FACTURACION_READ',
      icono: 'payments',
    },
    {
      nombre: 'Actividad clínica',
      descripcion: 'Indicadores clínicos, diagnósticos y continuidad de atención.',
      ruta: '/reportes-clinicos',
      permiso: 'REPORTES_CLINICOS',
      icono: 'health_and_safety',
    },
  ];

  get disponibles(): PanelInteligencia[] {
    return this.paneles.filter((panel) => this.auth.puede(panel.permiso));
  }

  get puedeConsultarReportes(): boolean {
    return ['REPORTES', 'REPORTES_FINANCIEROS', 'REPORTES_CLINICOS']
      .some((permiso) => this.auth.puede(permiso));
  }
}