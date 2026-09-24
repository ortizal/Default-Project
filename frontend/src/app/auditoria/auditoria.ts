import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Auditoria, Page } from '../core/models';
import { withLoading } from '../core/loading';

const MODULOS = [
  'AGENDA',
  'AGENTE_IA',
  'AUTOMATIZACIONES',
  'CITAS',
  'GOOGLE_CALENDAR',
  'HORARIOS',
  'ODONTOLOGOS',
  'PACIENTES',
  'SERVICIOS',
  'USUARIOS',
  'WHATSAPP',
];

@Component({
  selector: 'app-auditoria',
  templateUrl: './auditoria.html',
  imports: [CommonModule, FormsModule],
})
export class AuditoriaComponent implements OnInit {
  readonly modulos = MODULOS;
  rows: Auditoria[] = [];
  q = '';
  modulo = '';
  pagina = 0;
  totalPag = 1;
  cargando = false;
  error = '';
  private abiertos = new Set<number>();

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargar();
  }

  buscar(): void {
    this.pagina = 0;
    this.cargar();
  }

  cargar(): void {
    const params: string[] = [];
    if (this.q.trim()) params.push(`q=${encodeURIComponent(this.q.trim())}`);
    if (this.modulo) params.push(`modulo=${this.modulo}`);
    params.push(`page=${this.pagina}`, 'size=20');
    withLoading(this, this.api.get<Page<Auditoria>>(`/auditoria?${params.join('&')}`), undefined, this.cdr).subscribe({
      next: (r) => {
        this.rows = r.content;
        this.totalPag = Math.max(r.totalPages ?? 1, 1);
        this.cdr.markForCheck();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  ir(p: number): void {
    if (p < 0 || p >= this.totalPag) return;
    this.pagina = p;
    this.cargar();
  }

  toggle(id: number): void {
    if (this.abiertos.has(id)) {
      this.abiertos.delete(id);
    } else {
      this.abiertos.add(id);
    }
  }

  abierto(id: number): boolean {
    return this.abiertos.has(id);
  }

  pretty(v: unknown): string {
    if (v == null) return '—';
    try {
      const obj = typeof v === 'string' ? JSON.parse(v) : v;
      return JSON.stringify(obj, null, 2);
    } catch {
      return String(v);
    }
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a?.status === 403 ? 'No tienes permiso para ver la auditoría.' : 'Error de conexión';
  }
}