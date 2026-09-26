import { Component, computed, input, output } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-header',
  templateUrl: './header.html',
  styleUrl: './header.css',
  imports: [MatToolbarModule, MatIconModule, MatButtonModule],
})
export class HeaderComponent {
  readonly colapsado = input(false);
  readonly abierto = input(false);
  readonly titulo = input('');
  readonly fecha = input('');
  readonly toggle = output<void>();
  readonly salir = output<void>();

  readonly cerrando = computed(() => this.colapsado() || this.abierto());
}
