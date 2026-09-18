import { APP_INITIALIZER, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { Api } from './core/api';
import { authInterceptor } from './core/auth.interceptor';
export const appConfig = {
    providers: [
        provideBrowserGlobalErrorListeners(),
        provideAnimations(),
        provideHttpClient(withFetch(), withInterceptors([authInterceptor])),
        { provide: APP_INITIALIZER, useFactory: (api) => () => api.init(), deps: [Api], multi: true },
        provideRouter(routes),
    ],
};
