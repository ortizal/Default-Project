import { Component, ChangeDetectorRef, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.html',
  imports: [FormsModule],
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  username = '';
  password = '';
  cargando = false;
  error = '';
  mostrarContrasena = false;

  get puedeEntrar(): boolean {
    return !!this.username.trim() && !!this.password && !this.cargando;
  }

  entrar(): void {
    if (!this.puedeEntrar) return;
    this.error = '';
    this.cargando = true;
    this.cdr.markForCheck();
    this.auth
      .login(this.username, this.password)
      .catch((err) => {
        this.error =
          err?.error?.message ??
          err?.error?.mensaje ??
          (err?.status === 401 ? 'Credenciales inválidas' : 'Error al iniciar sesión');
      })
      .finally(() => {
        this.cargando = false;
        this.cdr.markForCheck();
      });
  }
}
