import { Component, inject, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../core/auth.service';
import { MENU, type MenuSection } from './menu';

const ICONOS: Record<string, string> = {
  '/dashboard': 'dashboard',
  '/dashboard/operacion': 'query_stats',
  '/pacientes': 'groups',
  '/odontologos': 'medical_services',
  '/servicios': 'healing',
  '/horarios': 'schedule',
  '/agenda': 'calendar_month',
  '/citas': 'event_available',
  '/whatsapp/inbox': 'mark_chat_unread',
  '/whatsapp/campana': 'campaign',
  '/whatsapp/sesiones': 'smart_toy',
  '/redes-sociales': 'share',
  '/plantillas': 'description',
  '/notificaciones': 'notifications',
  '/automatizaciones': 'bolt',
  '/agente': 'support_agent',
  '/google': 'event',
  '/reportes': 'insert_chart',
  '/reportes-clinicos': 'health_and_safety',
  '/auditoria': 'verified_user',
  '/usuarios': 'account_circle',
  '/facturacion': 'payments',
  '/facturacion/cuentas': 'account_balance_wallet',
  '/facturacion/comprobantes': 'receipt_long',
  '/facturacion/caja': 'point_of_sale',
  '/facturacion/reportes': 'query_stats',
  '/configuracion/consultorio': 'location_on',
  '/configuracion/integraciones': 'settings',
};

@Component({
  selector: 'app-sidebar',
  template: `
    <aside class="sidebar" [class.collapsed]="colapsado()" [class.open]="abierto()">
      <div
        class="brand"
        role="img"
        aria-label="CRM Dental AT, de Alantek"
        [title]="colapsado() ? 'CRM Dental AT · Alantek' : null"
      >
        <span class="brand-mark"><mat-icon>medical_services</mat-icon></span>
        <span class="brand-text">
          <span class="brand-name">CRM Dental AT</span>
          <span class="alantek"><span class="alantek-badge" aria-hidden="true">A</span>Alantek</span>
        </span>
      </div>

      <nav class="sidebar-nav">
        @for (sec of secciones; track sec.section) {
          <div class="section">{{ sec.section }}</div>
          @for (item of sec.items; track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: item.path === '/dashboard' || item.path === '/facturacion' || item.path === '/reportes' }"
              [title]="colapsado() ? item.label : null"
              (click)="navegar.emit()"
            >
              <mat-icon>{{ ICONOS[item.path] ?? 'apps' }}</mat-icon>
              <span class="txt">{{ item.label }}</span>
            </a>
          }
        }
      </nav>

      <div class="sidebar-footer">
        <div>Usuario: <strong>{{ usuario }}</strong></div>
        <button type="button" class="btn block" (click)="salir.emit()">
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

  readonly ICONOS = ICONOS;

  private readonly auth = inject(AuthService);

  /** Secciones del menú filtradas por los permisos de la sesión actual. */
  get secciones(): MenuSection[] {
    return MENU.map((sec) => ({
      section: sec.section,
      items: sec.items.filter((item) =>
        item.permisosAlternativos
          ? item.permisosAlternativos.some((permiso) => this.auth.puede(permiso))
          : this.auth.puede(item.permiso),
      ),
    })).filter((sec) => sec.items.length > 0);
  }

  get usuario(): string {
    return this.auth.username;
  }
}
