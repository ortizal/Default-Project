import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UiConfirmHostComponent } from './ui/ui-confirm-host';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, UiConfirmHostComponent],
  templateUrl: './app.html',
  styles: ``,
})
export class App {}
