import { ApplicationRef, Injectable, NgZone } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom, Observable } from 'rxjs';

export interface ApiConfig {
  apiUrl: string;
}

const DEFAULT: ApiConfig = { apiUrl: '/api/v1' };

@Injectable({ providedIn: 'root' })
export class Api {
  private cfg: ApiConfig = { ...DEFAULT };

  constructor(
    private http: HttpClient,
    private zone: NgZone,
    private appRef: ApplicationRef,
  ) {}

  async init(): Promise<void> {
    try {
      const r = await firstValueFrom(this.http.get<Partial<ApiConfig>>('/assets/config.json'));
      if (r?.apiUrl) this.cfg = { ...DEFAULT, ...r };
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
    return this.inZone(this.http.get<T>(this.url(path), { params }));
  }

  post<T>(path: string, body: unknown): Observable<T> {
    return this.inZone(this.http.post<T>(this.url(path), body));
  }

  put<T>(path: string, body: unknown): Observable<T> {
    return this.inZone(this.http.put<T>(this.url(path), body));
  }

  patch<T>(path: string, body: unknown): Observable<T> {
    return this.inZone(this.http.patch<T>(this.url(path), body));
  }

  del<T>(path: string): Observable<T> {
    return this.inZone(this.http.delete<T>(this.url(path)));
  }

  private inZone<T>(source: Observable<T>): Observable<T> {
    return new Observable<T>((subscriber) => {
      const sub = source.subscribe({
        next: (value) => this.zone.run(() => {
          subscriber.next(value);
          this.scheduleTick();
        }),
        error: (err) => this.zone.run(() => {
          subscriber.error(err);
          this.scheduleTick();
        }),
        complete: () => this.zone.run(() => {
          subscriber.complete();
          this.scheduleTick();
        }),
      });
      return () => sub.unsubscribe();
    });
  }

  private scheduleTick(): void {
    this.zone.run(() => {
      queueMicrotask(() => {
        try {
          const app = this.appRef as unknown as { dirtyFlags?: number };
          if (typeof app.dirtyFlags === 'number') {
            app.dirtyFlags |= 1;
          }
          this.appRef.tick();
        } catch {
          // Ignore re-entrant tick during an ongoing CD cycle.
        }
      });
    });
  }
}
