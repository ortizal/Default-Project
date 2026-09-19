import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { WhatsappSesion } from '../core/models';

@Component({
  selector: 'app-sesiones',
  templateUrl: './sesiones.html',
  styleUrl: './sesiones.css',
  imports: [CommonModule, FormsModule],
})
export class SesionesComponent {
  items: WhatsappSesion[] = [];
  buscar = '';
  estadoF = '';
  cargando = false;
  error = '';
  page = 0;
  totalPages = 1;
  total = 0;
  mostrarModal = false;
  nuevaSesion = { sesionId: '', nombre: '' };
  private reqSeq = 0;

  totalConectadas = 0;
  totalQR = 0;
  totalDesconectadas = 0;
  totalError = 0;

  constructor(private readonly api: Api) {
    this.cargar(0);
  }

  cargar(p: number): void {
    this.cargando = true;
    this.error = '';
    const seq = ++this.reqSeq;
    const params = new URLSearchParams({ page: String(p), size: '20', busca: this.buscar || '', estado: this.estadoF || '' });
    this.api.get<WhatsappSesion[]>('/whatsapp/sesiones?' + params.toString()).subscribe({
      next: (r) => {
        if (seq !== this.reqSeq) return;
        this.items = r;
        this.cargando = false;
        this.total = r.length;
        this.page = Math.min(p, this.totalPages - 1);
        this.totalPages = Math.max(Math.ceil(r.length / 20), 1);
        this.calcularTotales();
      },
      error: (e) => {
        if (seq !== this.reqSeq) return;
        this.error = this.msg(e);
        this.cargando = false;
      },
    });
  }

  calcularTotales(): void {
    this.totalConectadas = this.items.filter((s) => s.estado === 'CONECTADO').length;
    this.totalQR = this.items.filter((s) => s.estado === 'ESPERANDO_QR').length;
    this.totalDesconectadas = this.items.filter((s) => s.estado === 'DESCONECTADO').length;
    this.totalError = this.items.filter((s) => s.lastError && s.lastError.length > 0).length;
  }

  probar(s: WhatsappSesion): void {
    this.api.post<WhatsappSesion>(`/whatsapp/sesiones/${s.id}/conectar`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  conectar(s: WhatsappSesion): void {
    this.api.post<WhatsappSesion>(`/whatsapp/sesiones/${s.id}/conectar`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  desconectar(s: WhatsappSesion): void {
    this.api.post<WhatsappSesion>(`/whatsapp/sesiones/${s.id}/desconectar`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  borrar(s: WhatsappSesion): void {
    if (!confirm(`¿Eliminar la sesión ${s.sesionId}?`)) return;
    this.api.del<void>(`/whatsapp/sesiones/${s.id}`).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
    });
  }

  nueva(): void {
    this.nuevaSesion = { sesionId: '', nombre: '' };
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
  }

  crearSesion(): void {
    if (!this.nuevaSesion.sesionId.trim()) {
      this.error = 'El ID de sesión es obligatorio';
      return;
    }
    this.cargando = true;
    this.api.post<WhatsappSesion>('/whatsapp/sesiones', {
      sesionId: this.nuevaSesion.sesionId.trim(),
      nombre: this.nuevaSesion.nombre.trim() || null,
    }).subscribe({
      next: () => {
        this.mostrarModal = false;
        this.cargar(0);
      },
      error: (e) => { this.error = this.msg(e); this.cargando = false; },
    });
  }

  badge(estado: string): string {
    switch (estado) {
      case 'CONECTADO': return 'ok';
      case 'ESPERANDO_QR': return 'warn';
      case 'DESCONECTADO': return 'bad';
      default: return 'dim';
    }
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a.status === 409 || a.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}
