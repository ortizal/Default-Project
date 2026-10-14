import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Api } from '../core/api';
import { Auditoria, Page, paginasDe } from '../core/models';
import { withLoading } from '../core/loading';
import { UiPageHeaderComponent, UiTableComponent, UiPaginationComponent } from '../ui';

const MODULOS = [
  'AGENDA',
  'AGENTE_IA',
  'AUTOMATIZACIONES',
  'CITAS',
  'FACTURACION',
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
  imports: [
    CommonModule,
    FormsModule,
    UiPageHeaderComponent,
    UiTableComponent,
    UiPaginationComponent,
  ],
})
export class AuditoriaComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);

  readonly modulos = MODULOS;
  rows: Auditoria[] = [];
  q = '';
  modulo = '';
  entidad = '';
  entidadId: number | null = null;
  pagina = 0;
  totalPag = 1;
  cargando = false;
  error = '';
  private abiertos = new Set<number>();

  ngOnInit(): void {
    // Enlace desde la bandeja de comprobantes (§3.7):
    // ?entidad=COMPROBANTE_ELECTRONICO&entidadId=1&modulo=FACTURACION
    const params = this.route.snapshot.queryParamMap;
    this.q = params.get('q') ?? '';
    this.modulo = params.get('modulo') ?? '';
    this.entidad = params.get('entidad') ?? '';
    const id = Number(params.get('entidadId'));
    this.entidadId = Number.isFinite(id) && id > 0 ? id : null;
    this.cargar();
  }

  /** Quita el enlace de una fila concreta y vuelve a la vista general. */
  quitarFiltroEntidad(): void {
    this.entidad = '';
    this.entidadId = null;
    this.pagina = 0;
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
    if (this.entidad) params.push(`entidad=${encodeURIComponent(this.entidad)}`);
    if (this.entidadId) params.push(`entidadId=${this.entidadId}`);
    params.push(`page=${this.pagina}`, 'size=20');
    withLoading(
      this,
      this.api.get<Page<Auditoria>>(`/auditoria?${params.join('&')}`),
      undefined,
      this.cdr,
    ).subscribe({
      next: (r) => {
        this.rows = r.content;
        this.totalPag = paginasDe(r);
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
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
