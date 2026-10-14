import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

const CLAVE = 'dentalcrm.auth';

describe('authInterceptor', () => {
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.setItem(
      CLAVE,
      JSON.stringify({ token: 'token-expirado', username: 'admin', roles: [], permisos: [] }),
    );
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.removeItem(CLAVE);
  });

  it('no envía el token guardado en la petición de login', () => {
    TestBed.inject(HttpClient).post('/api/v1/auth/login', {}).subscribe();

    const request = httpTesting.expectOne('/api/v1/auth/login');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('conserva el token en peticiones protegidas', () => {
    TestBed.inject(HttpClient).get('/api/v1/pacientes').subscribe();

    const request = httpTesting.expectOne('/api/v1/pacientes');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-expirado');
    request.flush([]);
  });
});