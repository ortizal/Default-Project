import { Component, signal } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { MatToolbarModule } from '@angular/material/toolbar';
import { SidebarComponent } from './sidebar';
import { HeaderComponent } from './header';
import { FooterComponent } from './footer';
import { MENU } from './menu';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.html',
  styleUrl: './layout.css',
  imports: [
    RouterOutlet,
    SidebarComponent,
    HeaderComponent,
    FooterComponent,
    MatToolbarModule,
  ],
})
export class LayoutComponent {
  readonly colapsado = signal(false);

  constructor(
    protected readonly auth: AuthService,
    private readonly router: Router,
  ) {}

  get titulo(): string {
    const seg = this.router.url.split('/')[1];
    if (seg === 'citas') return 'Citas';
    if (seg === 'dashboard') return 'Inicio';
    if (seg === 'pacientes') return 'Pacientes';
    if (seg === 'odontologos') return 'Odontólogos';
    if (seg === 'servicios') return 'Servicios';
    if (seg === 'horarios') return 'Horarios';
    if (seg === 'agenda') return 'Agenda';
    if (seg === 'whatsapp') return 'WhatsApp';
    if (seg === 'google') return 'Google Calendar';
    if (seg === 'plantillas') return 'Plantillas';
    if (seg === 'automatizaciones') return 'Automatizaciones';
    if (seg === 'notificaciones') return 'Notificaciones';
    if (seg === 'agente') return 'Agente IA';
    if (seg === 'reportes') return 'Reportes';
    if (seg === 'auditoria') return 'Auditoría';
    if (seg === 'usuarios') return 'Usuarios';
    return (MENU.flatMap((s) => s.items).find((i) => i.path === '/' + seg)?.label) ?? 'Inicio';
  }

  get fecha(): string {
    return new Date().toLocaleDateString('es-EC', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }

  alternar(): void {
    this.colapsado.update((c) => !c);
  }

  salir(): void {
    this.auth.logout();
  }
}
