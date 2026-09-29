import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { AgenteConversacion, ConversacionInicio, MensajeRequest } from '../core/models';
import { withLoading } from '../core/loading';
import { UiPageHeaderComponent, UiTableComponent, UiConfirmService } from '../ui';

@Component({
  selector: 'app-agente',
  templateUrl: './agente.html',
  imports: [FormsModule, CommonModule, UiPageHeaderComponent, UiTableComponent],
})
export class AgenteComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);

  private readonly confirmacion = inject(UiConfirmService);

  items: AgenteConversacion[] = [];
  q = '';
  error = '';
  cargando = false;
  msgTelefono = '';
  msgTexto = '';
  inicioResp: ConversacionInicio | null = null;
  respuestaChat = '';

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    withLoading(
      this,
      this.api.get<AgenteConversacion[]>(`/agente/conversaciones?q=${encodeURIComponent(this.q)}`),
      undefined,
      this.cdr,
    ).subscribe({
      next: (r) => {
        this.items = r;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  iniciarConversacion(): void {
    this.api.post<ConversacionInicio>('/agente/conversacion/start', {}).subscribe({
      next: (r) => {
        this.inicioResp = r;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  guardando = false;

  enviarMensaje(): void {
    if (this.guardando) return;
    if (!this.msgTelefono.trim() || !this.msgTexto.trim()) {
      this.error = 'Completa teléfono y mensaje';
      this.cdr.markForCheck();
      return;
    }
    const req: MensajeRequest = { telefono: this.msgTelefono.trim(), texto: this.msgTexto.trim() };
    this.guardando = true;
    this.api.post<string>('/agente/conversacion/mensaje', req).subscribe({
      next: (r) => {
        this.guardando = false;
        this.respuestaChat = r;
        this.msgTexto = '';
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.guardando = false;
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  toggleAgente(c: AgenteConversacion): void {
    this.api
      .post<AgenteConversacion>(`/agente/conversaciones/${c.id}/agente`, {
        activo: !c.agenteActivo,
      })
      .subscribe({
        next: () => this.cargar(),
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  async transferir(c: AgenteConversacion): Promise<void> {
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Transferir a atención humana',
        mensaje: `¿Transferir la conversación de ${c.nombreContacto || c.telefono} a atención humana?`,
        confirmarTexto: 'Transferir',
      }))
    )
      return;
    this.api.post<AgenteConversacion>(`/agente/conversaciones/${c.id}/transferir`, {}).subscribe({
      next: () => this.cargar(),
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
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
    return a?.status === 409 || a?.status === 400
      ? (a.error?.message ?? 'Datos inválidos')
      : 'Error de conexión';
  }
}
