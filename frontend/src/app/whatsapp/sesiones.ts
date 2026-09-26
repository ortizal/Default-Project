import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Page, WhatsappSesion } from '../core/models';
import { withLoading } from '../core/loading';
import { UiPageHeaderComponent, UiPaginationComponent } from '../ui';

@Component({
  selector: 'app-sesiones',
  templateUrl: './sesiones.html',
  styleUrl: './sesiones.css',
  imports: [CommonModule, FormsModule, UiPageHeaderComponent, UiPaginationComponent],
})
export class SesionesComponent implements OnInit {
  items: WhatsappSesion[] = [];
  buscar = '';
  estadoF = '';
  cargando = false;
  error = '';
  page = 0;
  totalPages = 1;
  total = 0;
  size = 20;
  mostrarModal = false;
  nuevaSesion = { sesionId: '', nombre: '' };
  private reqSeq = 0;

  totalConectadas = 0;
  totalQR = 0;
  totalDesconectadas = 0;
  totalError = 0;

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargar(0);
  }

  cargar(p: number): void {
    this.error = '';
    const seq = ++this.reqSeq;
    const params = new URLSearchParams({ page: String(p), size: String(this.size), busca: this.buscar || '', estado: this.estadoF || '' });
    withLoading(this, this.api.get<Page<WhatsappSesion>>('/whatsapp/sesiones?' + params.toString()), () => seq === this.reqSeq, this.cdr).subscribe({
      next: (r) => {
        if (seq !== this.reqSeq) return;
        this.items = r.content ?? [];
        this.total = r.totalElements ?? this.items.length;
        this.totalPages = Math.max(r.totalPages ?? 1, 1);
        this.page = Math.min(Math.max(p, 0), this.totalPages - 1);
        this.calcularTotales();
        this.cdr.markForCheck();
      },
      error: (e) => {
        if (seq !== this.reqSeq) return;
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  calcularTotales(): void {
    this.totalConectadas = this.items.filter((s) => s.estado === 'CONECTADA').length;
    this.totalQR = this.items.filter((s) => s.estado === 'CONECTANDO').length;
    this.totalDesconectadas = this.items.filter((s) => s.estado === 'DESCONECTADA').length;
    this.totalError = this.items.filter((s) => s.estado === 'ERROR' || (s.lastError && s.lastError.length > 0)).length;
  }

  probar(s: WhatsappSesion): void {
    this.api.post<WhatsappSesion>(`/whatsapp/sesiones/${s.id}/conectar`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  conectar(s: WhatsappSesion): void {
    this.api.post<WhatsappSesion>(`/whatsapp/sesiones/${s.id}/conectar`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  desconectar(s: WhatsappSesion): void {
    if (!confirm('¿Desconectar la sesión?')) return;
    this.api.post<WhatsappSesion>(`/whatsapp/sesiones/${s.id}/desconectar`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  borrar(s: WhatsappSesion): void {
    if (!confirm(`¿Eliminar la sesión ${s.sesionId}?`)) return;
    this.api.del<void>(`/whatsapp/sesiones/${s.id}`).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
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
    this.cdr.markForCheck();
    this.api.post<WhatsappSesion>('/whatsapp/sesiones', {
      sesionId: this.nuevaSesion.sesionId.trim(),
      nombre: this.nuevaSesion.nombre.trim() || null,
    }).subscribe({
      next: () => {
        this.mostrarModal = false;
        this.cargar(0);
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cargando = false;
        this.cdr.markForCheck();
      },
    });
  }

  badge(estado: string): string {
    switch (estado) {
      case 'CONECTADA': return 'ok';
      case 'CONECTANDO': return 'warn';
      case 'DESCONECTADA': return 'bad';
      case 'ERROR': return 'bad';
      default: return 'dim';
    }
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a.status === 409 || a.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}
