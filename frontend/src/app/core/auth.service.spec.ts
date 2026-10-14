import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import type { LoginResponse } from './models';

const CLAVE = 'dentalcrm.auth';

function guardarSesion(datos: Partial<LoginResponse>): void {
  const completa = {
    token: 'tok',
    tokenType: 'Bearer',
    expiresInMs: 3_600_000,
    username: 'admin',
    roles: ['SUPER_ADMIN'],
    ...datos,
  } as LoginResponse;
  localStorage.setItem(CLAVE, JSON.stringify(completa));
}

describe('AuthService — permisos para el menú dinámico', () => {
  beforeEach(async () => {
    localStorage.removeItem(CLAVE);
    await TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();
  });

  afterEach(() => localStorage.removeItem(CLAVE));

  it('la sesión antigua (sin permisos) no oculta nada', () => {
    guardarSesion({ permisos: undefined as unknown as string[] });
    const auth = TestBed.inject(AuthService);
    expect(auth.permisos).toEqual([]);
    expect(auth.puede('USUARIOS_READ')).toBe(true);
    expect(auth.puede()).toBe(true);
  });

  it('sólo concede lo que viene en la sesión', () => {
    guardarSesion({ permisos: ['PACIENTES_READ', 'CITAS_READ'] });
    const auth = TestBed.inject(AuthService);
    expect(auth.puede('PACIENTES_READ')).toBe(true);
    expect(auth.puede('USUARIOS_READ')).toBe(false);
    expect(auth.puede('FACTURACION_EMITIR')).toBe(false);
    expect(auth.puede()).toBe(true);
    expect(auth.puede(undefined)).toBe(true);
  });

  it('una sesión nueva sin permisos concedidos no ve nada restringido', () => {
    guardarSesion({ permisos: [], roles: ['INVITADO'] });
    const auth = TestBed.inject(AuthService);
    expect(auth.puede('PACIENTES_READ')).toBe(false);
    expect(auth.puede()).toBe(true);
  });

  it('hasRole sigue funcionando sobre la lista de roles', () => {
    guardarSesion({ roles: ['ADMIN'], permisos: [] });
    const auth = TestBed.inject(AuthService);
    expect(auth.hasRole('admin')).toBe(true);
    expect(auth.hasRole('ODONTOLOGO')).toBe(false);
  });
});
