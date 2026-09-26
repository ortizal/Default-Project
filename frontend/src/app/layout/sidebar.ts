import { Component, inject, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../core/auth.service';
import { MENU } from './menu';

const ICONOS: Record<string, string> = {
  '/dashboard': 'home',
  '/pacientes': 'people',
  '/odontologos': 'medical_services',
  '/servicios': 'build',
  '/horarios': 'schedule',
  '/agenda': 'calendar_month',
  '/citas': 'event_available',
  '/whatsapp/inbox': 'chat',
  '/whatsapp/campana': 'campaign',
  '/whatsapp/sesiones': 'smart_toy',
  '/plantillas': 'description',
  '/notificaciones': 'notifications',
  '/automatizaciones': 'bolt',
  '/agente': 'support_agent',
  '/google': 'event',
  '/reportes': 'insert_chart',
  '/auditoria': 'verified_user',
  '/usuarios': 'account_circle',
};

@Component({
  selector: 'app-sidebar',
  template: `
    <aside class="sidebar" [class.collapsed]="colapsado()" [class.open]="abierto()">
      <div class="brand">
        <span class="brand-mark"><mat-icon>medical_services</mat-icon></span>
        <span class="brand-name">Little Smile CRM</span>
      </div>

      <nav class="sidebar-nav">
        @for (sec of MENU; track sec.section) {
          <div class="section">{{ sec.section }}</div>
          @for (item of sec.items; track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="active"
              [title]="colapsado() ? item.label : null"
              (click)="navegar.emit()"
            >
              <mat-icon>{{ ICONOS[item.path] ?? 'chevron_right' }}</mat-icon>
              <span class="txt">{{ item.label }}</span>
            </a>
          }
        }
      </nav>

      <div class="sidebar-footer">
        <div>Usuario: <strong>{{ usuario }}</strong></div>
        <button class="btn sm block mt-2" (click)="salir.emit()">
          Cerrar sesión
        </button>
      </div>
    </aside>
  `,
  imports: [RouterLink, RouterLinkActive, MatIconModule],
})
export class SidebarComponent {
  readonly colapsado = input(false);
  readonly abierto = input(false);
  readonly salir = output<void>();
  readonly navegar = output<void>();

  readonly MENU = MENU;
  readonly ICONOS = ICONOS;

  private readonly auth = inject(AuthService);

  get usuario(): string {
    return this.auth.username;
  }
}
