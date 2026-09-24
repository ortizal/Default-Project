import { APP_INITIALIZER, ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { Api } from './core/api';
import { authInterceptor } from './core/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection(),
    provideAnimations(),
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: APP_INITIALIZER, useFactory: (api: Api) => () => api.init(), deps: [Api], multi: true },
    provideRouter(routes),
  ],
};