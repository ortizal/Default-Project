import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Api } from './api';
import { LoginResponse } from './models';

const KEY = 'dentalcrm.auth';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private state: LoginResponse | null = null;

  constructor(
    private api: Api,
    private router: Router,
  ) {
    try {
      const raw = localStorage.getItem(KEY);
      if (raw) this.state = JSON.parse(raw) as LoginResponse;
    } catch {
      this.state = null;
    }
  }

  get token(): string | null {
    return this.state?.token ?? null;
  }

  get username(): string {
    return this.state?.username ?? '';
  }

  get roles(): string[] {
    return this.state?.roles ?? [];
  }

  get logged(): boolean {
    return !!this.token;
  }

  hasRole(code: string): boolean {
    return this.roles.some((r) => r.toUpperCase() === code.toUpperCase());
  }

  login(username: string, password: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.api.post<LoginResponse>('/auth/login', { username, password }).subscribe({
        next: (l) => {
          this.state = l;
          localStorage.setItem(KEY, JSON.stringify(l));
          this.router.navigate(['/dashboard']);
          resolve();
        },
        error: (err) => reject(err),
      });
    });
  }

  logout(): void {
    this.state = null;
    localStorage.removeItem(KEY);
    this.router.navigate(['/login']);
  }
}