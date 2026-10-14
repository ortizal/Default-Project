import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const isLoginRequest = req.url.includes('/auth/login');
  const esPublica = req.url.includes('/public/');
  if (auth.token && !isLoginRequest) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${auth.token}` } });
  }
  return next(req).pipe(
    catchError((err) => {
      // Los endpoints públicos (agenda con enlace temporal) nunca cierran sesión.
      if (err.status === 401 && !isLoginRequest && !esPublica) {
        auth.logout();
      }
      return throwError(() => err);
    }),
  );
};