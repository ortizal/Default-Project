import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  imports: [FormsModule],
})
export class LoginComponent {
  username = '';
  password = '';
  cargando = false;
  error = '';
  mostrarContrasena = false;

  constructor(private readonly auth: AuthService) {}

  entrar(): void {
    this.error = '';
    this.cargando = true;
    this.auth
      .login(this.username, this.password)
      .catch((err) => {
        this.error =
          err?.error?.message ?? err?.error?.mensaje ?? (err?.status === 401 ? 'Credenciales inválidas' : 'Error al iniciar sesión');
      })
      .finally(() => {
        this.cargando = false;
      });
  }
}