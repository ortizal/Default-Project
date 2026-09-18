import { Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';

@Component({
  selector: 'app-footer',
  templateUrl: './footer.html',
  styleUrl: './footer.css',
  imports: [MatToolbarModule],
})
export class FooterComponent {
  readonly anio = new Date().getFullYear();
}
