import { Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
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
    <aside class="sidebar" [class.collapsed]="colapsado()">
      <div class="brand">
        <mat-icon>dental_care</mat-icon>
        @if (!colapsado()) {
          <span>CRM Dental</span>
        }
      </div>
      <nav class="lista">
        @for (sec of MENU; track sec.section) {
          <div class="section" [hidden]="colapsado()">{{ sec.section }}</div>
          @for (item of sec.items; track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="active"
              [attr.title]="colapsado() ? item.label : null"
            >
              <mat-icon>{{ ICONOS[item.path] ?? 'chevron_right' }}</mat-icon>
              @if (!colapsado()) {
                <span class="txt">{{ item.label }}</span>
              }
            </a>
          }
        }
      </nav>
      <footer>
        <div class="muted">Usuario: <strong>{{ usuario }}</strong></div>
        <button class="btn small" style="margin-top:6px" (click)="salir()">
          Cerrar sesión
        </button>
      </footer>
    </aside>
  `,
  styles: [
    `
      :host {
        display: block;
        height: 100%;
      }
      .sidebar {
        display: flex;
        flex-direction: column;
        height: 100%;
        width: 230px;
        background: var(--sidebar-bg);
        color: var(--sidebar-fg);
        overflow: hidden;
        transition: width 0.22s ease;
      }
      .sidebar.collapsed {
        width: 62px;
      }
      .brand {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 16px 14px;
        font-weight: 600;
        color: #fff;
        border-bottom: 1px solid #1e293b;
        white-space: nowrap;
      }
      .lista {
        flex: 1 1 auto;
        overflow-y: auto;
        min-height: 0;
      }
      .section {
        font-size: 11px;
        text-transform: uppercase;
        letter-spacing: 0.08em;
        color: var(--muted);
        padding: 10px 14px 4px;
      }
      .lista a {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 10px 14px;
        font-size: 13.5px;
        text-decoration: none;
        color: var(--sidebar-fg);
        border-left: 3px solid transparent;
      }
      .lista a:hover {
        background: #1e293b;
      }
      .lista a.active {
        background: var(--sidebar-active);
        color: #fff;
        border-left-color: #14b8a6;
      }
      .lista a mat-icon {
        font-size: 20px;
        width: 22px;
        height: 22px;
      }
      footer {
        padding: 12px 14px;
        font-size: 12px;
        border-top: 1px solid #1e293b;
      }
    `,
  ],
  imports: [RouterLink, RouterLinkActive, MatListModule, MatIconModule, MatTooltipModule],
})
export class SidebarComponent {
  readonly colapsado = input(false);
  readonly MENU = MENU;
  readonly ICONOS = ICONOS;

  usuario = '';

  salir(): void {
    /* el layout se encarga del logout */
  }
}
