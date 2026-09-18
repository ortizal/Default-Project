import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
export const authInterceptor = (req, next) => {
    const auth = inject(AuthService);
    if (auth.token) {
        req = req.clone({ setHeaders: { Authorization: `Bearer ${auth.token}` } });
    }
    return next(req).pipe(catchError((err) => {
        if (err.status === 401 && !req.url.includes('/auth/login')) {
            auth.logout();
        }
        return throwError(() => err);
    }));
};
