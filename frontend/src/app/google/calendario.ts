import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { GoogleCalendario, GoogleConnect, GoogleCredential, GoogleStatus, GoogleSyncResult } from '../core/models';
import { withLoading } from '../core/loading';

@Component({
  selector: 'app-calendario',
  templateUrl: './calendario.html',
  imports: [CommonModule, FormsModule],
})
export class CalendarioComponent implements OnInit {
  status?: GoogleStatus;
  syncResult?: GoogleSyncResult;
  error = '';
  cargando = false;
  credenciales?: GoogleCredential;
  credGuardando = false;
  credGuardado = false;

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    withLoading(this, this.api.get<GoogleStatus>('/google/status'), undefined, this.cdr).subscribe({
      next: (r) => {
        this.status = r;
        this.syncResult = undefined;
        this.cdr.markForCheck();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
    this.api.get<GoogleCredential>('/google/credentials').subscribe({
      next: (c) => { this.credenciales = c; this.cdr.markForCheck(); },
      error: () => this.cdr.markForCheck(),
    });
  }

  connect(): void {
    this.api.get<GoogleConnect>('/google/connect').subscribe({
      next: (r) => {
        if (r.authUrl) {
          window.open(r.authUrl, '_blank');
        }
        this.cdr.markForCheck();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  seleccionar(c: GoogleCalendario): void {
    this.api.post<void>(`/google/calendars/${c.id}/select`, {}).subscribe({
      next: () => this.cargar(),
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  sync(): void {
    this.api.post<GoogleSyncResult>('/google/sync', {}).subscribe({
      next: (r) => {
        this.syncResult = r;
        this.cdr.markForCheck();
        this.cargar();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  disconnect(): void {
    if (!confirm('¿Desconectar la cuenta de Google y borrar sus tokens?')) return;
    this.api.del<void>('/google/disconnect').subscribe({
      next: () => this.cargar(),
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  guardarCredenciales(): void {
    if (!this.credenciales) return;
    this.credGuardando = true;
    this.credGuardado = false;
    this.cdr.markForCheck();
    this.api.put<GoogleCredential>('/google/credentials', this.credenciales).subscribe({
      next: (r) => {
        this.credenciales = r;
        this.credGuardando = false;
        this.credGuardado = true;
        setTimeout(() => { this.credGuardado = false; this.cdr.markForCheck(); }, 3000);
        this.cargar();
      },
      error: (e) => { this.error = this.msg(e); this.credGuardando = false; this.cdr.markForCheck(); },
    });
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return (a?.status === 400 || a?.status === 409) && a.error?.message ? a.error.message : 'Error de conexión';
  }
}
