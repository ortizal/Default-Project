import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Conversacion, ConversacionDetalle, Mensaje } from '../core/models';

@Component({
  selector: 'app-inbox',
  templateUrl: './inbox.html',
  imports: [CommonModule, FormsModule],
})
export class InboxComponent implements OnDestroy {
  conversaciones: Conversacion[] = [];
  seleccion: Conversacion | null = null;
  detalle: ConversacionDetalle | null = null;
  q = '';
  texto = '';
  nuevoEstado = '';
  enviando = false;
  error = '';
  private timer: ReturnType<typeof setInterval> | null = null;

  constructor(private readonly api: Api) {
    this.cargarLista();
    this.timer = setInterval(() => this.poll(), 6000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  private poll(): void {
    if (!document.hidden) this.cargarLista(true);
  }

  cargarLista(silencioso = false): void {
    this.api.get<Conversacion[]>(`/whatsapp/conversaciones?q=${encodeURIComponent(this.q)}`).subscribe({
      next: (r) => {
        this.conversaciones = r;
        if (this.seleccion) {
          const a = r.find((c) => c.id === this.seleccion!.id);
          if (a) this.seleccion = a;
          this.abrir(this.seleccion);
        }
      },
      error: (e) => {
        if (!silencioso) this.error = this.msg(e);
      },
    });
  }

  abrir(c: Conversacion): void {
    this.seleccion = c;
    this.nuevoEstado = '';
    this.api.get<ConversacionDetalle>(`/whatsapp/conversaciones/${c.id}`).subscribe({
      next: (d) => (this.detalle = d),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  enviar(): void {
    if (!this.detalle || !this.texto.trim()) return;
    this.enviando = true;
    this.api
      .post<Mensaje>(`/whatsapp/conversaciones/${this.detalle.conversacion.id}/mensajes`, { texto: this.texto.trim() })
      .subscribe({
        next: (m) => {
          this.texto = '';
          this.detalle!.mensajes = [...this.detalle!.mensajes, m];
          this.enviando = false;
        },
        error: (e) => {
          this.error = this.msg(e);
          this.enviando = false;
        },
      });
  }

  cambiarEstado(): void {
    if (!this.detalle || !this.nuevoEstado) return;
    this.api
      .post<Conversacion>(`/whatsapp/conversaciones/${this.detalle.conversacion.id}/estado`, { estado: this.nuevoEstado })
      .subscribe({
        next: (c) => {
          this.seleccion = c;
          this.cargarLista();
          this.nuevoEstado = '';
        },
        error: (e) => (this.error = this.msg(e)),
      });
  }

  badge(estado: string): string {
    switch (estado) {
      case 'BOT':
        return 'info';
      case 'ATENCION_HUMANA':
        return 'warn';
      case 'ATENDIDA':
        return 'ok';
      case 'CERRADA':
        return 'dim';
      default:
        return 'dim';
    }
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}