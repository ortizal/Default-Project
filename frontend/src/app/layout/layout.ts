import { Component, inject, signal, OnDestroy } from '@angular/core';
import { Router, RouterOutlet, NavigationEnd } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { SidebarComponent } from './sidebar';
import { HeaderComponent } from './header';
import { FooterComponent } from './footer';
import { MENU } from './menu';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.html',
  styleUrl: './layout.css',
  imports: [RouterOutlet, SidebarComponent, HeaderComponent, FooterComponent],
})
export class LayoutComponent implements OnDestroy {
  readonly colapsado = signal(false);
  readonly abierto = signal(false);

  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly sub: Subscription;

  constructor() {
    // Al navegar en móvil siempre se cierra el drawer.
    this.sub = this.router.events.subscribe((e) => {
      if (e instanceof NavigationEnd) this.abierto.set(false);
    });
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

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

  /** Un mismo botón: en desktop colapsa el sidebar, en móvil abre el drawer. */
  alternar(): void {
    if (this.esMovil()) {
      this.abierto.update((v) => !v);
    } else {
      this.colapsado.update((c) => !c);
    }
  }

  cerrarMenu(): void {
    this.abierto.set(false);
  }

  salir(): void {
    this.auth.logout();
  }

  private esMovil(): boolean {
    return typeof window !== 'undefined' && window.matchMedia('(max-width: 991px)').matches;
  }
}
