import { Component, computed, DestroyRef, inject, input, OnInit, output, signal } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

type Tema = 'dark' | 'light';

const CLAVE = 'ls-theme';

@Component({
  selector: 'app-header',
  templateUrl: './header.html',
  styleUrl: './header.css',
  imports: [MatToolbarModule, MatIconModule, MatButtonModule],
})
export class HeaderComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  private readonly mq = window.matchMedia('(prefers-color-scheme: dark)');

  readonly colapsado = input(false);
  readonly abierto = input(false);
  readonly titulo = input('');
  readonly fecha = input('');
  readonly alternarMenu = output<void>();
  readonly salir = output<void>();

  readonly cerrando = computed(() => this.colapsado() || this.abierto());

  /** Tema que se está viendo ahora (explícito o el del sistema). */
  readonly oscuro = signal(this.mq.matches);

  ngOnInit(): void {
    this.aplicar(this.leerPreferencia());
    this.mq.addEventListener('change', this.onSistema);
    this.destroyRef.onDestroy(() => this.mq.removeEventListener('change', this.onSistema));
  }

  alternarTema(): void {
    const siguiente: Tema = this.oscuro() ? 'light' : 'dark';
    this.aplicar(siguiente);
    try {
      localStorage.setItem(CLAVE, siguiente);
    } catch {
      /* sin almacenamiento: el tema se aplica sólo para esta sesión */
    }
  }

  /** Etiqueta accesible y tooltip: siempre describe la acción a realizar. */
  readonly etiquetaTema = computed(() =>
    this.oscuro() ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro',
  );

  private readonly onSistema = (): void => {
    if (!this.leerPreferencia()) this.aplicar(null);
  };

  private leerPreferencia(): Tema | null {
    try {
      const t = localStorage.getItem(CLAVE);
      return t === 'dark' || t === 'light' ? t : null;
    } catch {
      return null;
    }
  }

  /**
   * `<html>` lleva SIEMPRE una clase de tema (así lo garantiza también el
   * script de <head>): con preferencia guardada se impone sobre el sistema y
   * sin ella se refleja el sistema, que puede cambiar en caliente.
   */
  private aplicar(preferencia: Tema | null): void {
    const efectivo: Tema = preferencia ?? (this.mq.matches ? 'dark' : 'light');
    const html = document.documentElement;
    html.classList.toggle('theme-dark', efectivo === 'dark');
    html.classList.toggle('theme-light', efectivo === 'light');
    this.oscuro.set(efectivo === 'dark');
  }
}
