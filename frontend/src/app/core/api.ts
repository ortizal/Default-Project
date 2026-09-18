import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiConfig {
  apiUrl: string;
}

const DEFAULT: ApiConfig = { apiUrl: '/api/v1' };

@Injectable({ providedIn: 'root' })
export class Api {
  private cfg: ApiConfig = { ...DEFAULT };

  constructor(private http: HttpClient) {}

  async init(): Promise<void> {
    try {
      const r = await fetch('/assets/config.json', { cache: 'no-cache' });
      if (r.ok) {
        const json = (await r.json()) as Partial<ApiConfig>;
        if (json.apiUrl) this.cfg = { ...DEFAULT, ...json };
      }
    } catch {
      this.cfg = { ...DEFAULT };
    }
  }

  get apiUrl(): string {
    return this.cfg.apiUrl;
  }

  url(path: string): string {
    return this.cfg.apiUrl + path;
  }

  get<T>(path: string, params?: HttpParams): Observable<T> {
    return this.http.get<T>(this.url(path), { params });
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.http.post<T>(this.url(path), body);
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.http.put<T>(this.url(path), body);
  }

  patch<T>(path: string, body: unknown): Observable<T> {
    return this.http.patch<T>(this.url(path), body);
  }

  del<T>(path: string): Observable<T> {
    return this.http.delete<T>(this.url(path));
  }
}