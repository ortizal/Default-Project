import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { AgenteConversacion, ConversacionInicio, MensajeRequest } from '../core/models';

@Component({
  selector: 'app-agente',
  templateUrl: './agente.html',
  imports: [FormsModule, CommonModule],
})
export class AgenteComponent {
  items: AgenteConversacion[] = [];
  q = '';
  error = '';
  msgTelefono = '';
  msgTexto = '';
  inicioResp: ConversacionInicio | null = null;
  respuestaChat = '';

  constructor(private readonly api: Api) {
    this.cargar();
  }

  cargar(): void {
    this.api.get<AgenteConversacion[]>(`/agente/conversaciones?q=${encodeURIComponent(this.q)}`).subscribe({
      next: (r) => (this.items = r),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  iniciarConversacion(): void {
    this.api.post<ConversacionInicio>('/agente/conversacion/start', {}).subscribe({
      next: (r) => (this.inicioResp = r),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  enviarMensaje(): void {
    if (!this.msgTelefono.trim() || !this.msgTexto.trim()) {
      this.error = 'Completa teléfono y mensaje';
      return;
    }
    const req: MensajeRequest = { telefono: this.msgTelefono.trim(), texto: this.msgTexto.trim() };
    this.api.post<string>('/agente/conversacion/mensaje', req).subscribe({
      next: (r) => {
        this.respuestaChat = r;
        this.msgTexto = '';
      },
      error: (e) => (this.error = this.msg(e)),
    });
  }

  toggleAgente(c: AgenteConversacion): void {
    this.api.post<AgenteConversacion>(`/agente/conversaciones/${c.id}/agente`, { activo: !c.agenteActivo }).subscribe({
      next: () => this.cargar(),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  transferir(c: AgenteConversacion): void {
    if (!confirm(`¿Transferir la conversación de ${c.nombreContacto || c.telefono} a atención humana?`)) return;
    this.api.post<AgenteConversacion>(`/agente/conversaciones/${c.id}/transferir`, {}).subscribe({
      next: () => this.cargar(),
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
    if (a?.status === 403) return 'Sin permisos para ver el agente';
    return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}