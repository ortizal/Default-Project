import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Conversacion, ConversacionDetalle, Mensaje } from '../core/models';

@Component({
  selector: 'app-inbox',
  templateUrl: './inbox.html',
  styleUrl: './inbox.css',
  imports: [CommonModule, FormsModule],
})
export class InboxComponent implements OnInit, OnDestroy {
  conversaciones: Conversacion[] = [];
  seleccion: Conversacion | null = null;
  detalle: ConversacionDetalle | null = null;
  q = '';
  texto = '';
  nuevoEstado = '';
  enviando = false;
  cargando = false;
  error = '';
  private timer: ReturnType<typeof setInterval> | null = null;
  private reqSeq = 0;

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
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
    const seq = ++this.reqSeq;
    this.api.get<Conversacion[]>(`/whatsapp/conversaciones?q=${encodeURIComponent(this.q)}`).subscribe({
      next: (r) => {
        if (seq !== this.reqSeq) return;
        this.conversaciones = r;
        if (this.seleccion) {
          const a = r.find((c) => c.id === this.seleccion!.id);
          if (a) this.seleccion = a;
          this.abrir(this.seleccion);
        }
        this.cdr.markForCheck();
      },
      error: (e) => {
        if (seq !== this.reqSeq) return;
        if (!silencioso) this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  abrir(c: Conversacion): void {
    this.seleccion = c;
    this.nuevoEstado = '';
    this.api.get<ConversacionDetalle>(`/whatsapp/conversaciones/${c.id}`).subscribe({
      next: (d) => { this.detalle = d; this.cdr.markForCheck(); },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  enviar(): void {
    if (!this.detalle || !this.texto.trim()) return;
    this.enviando = true;
    this.cdr.markForCheck();
    this.api
      .post<Mensaje>(`/whatsapp/conversaciones/${this.detalle.conversacion.id}/mensajes`, { texto: this.texto.trim() })
      .subscribe({
        next: (m) => {
          this.texto = '';
          this.detalle!.mensajes = [...this.detalle!.mensajes, m];
          this.enviando = false;
          this.cdr.markForCheck();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.enviando = false;
          this.cdr.markForCheck();
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
          this.cdr.markForCheck();
        },
        error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
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

  iniciales(c: Conversacion): string {
    const nombre = c.nombreContacto || c.pacienteNombres || c.telefono;
    return nombre
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((parte) => parte[0])
      .join('')
      .toUpperCase();
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}