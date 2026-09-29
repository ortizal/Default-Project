import { Component, OnInit, ChangeDetectorRef, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Automatizacion, Plantilla } from '../core/models';
import { withLoading } from '../core/loading';
import { UiPageHeaderComponent, UiTableComponent, UiConfirmService } from '../ui';

const EVENTOS = [
  'CITA_CREADA',
  'CITA_PROXIMA',
  'CITA_CONFIRMADA',
  'CITA_CANCELADA',
  'CITA_ATENDIDA',
  'NO_ASISTIO',
];

@Component({
  selector: 'app-automatizaciones',
  templateUrl: './automatizaciones.html',
  imports: [CommonModule, FormsModule, UiPageHeaderComponent, UiTableComponent],
})
export class AutomatizacionesComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);

  private readonly confirmacion = inject(UiConfirmService);

  items: Automatizacion[] = [];
  plantillas: Plantilla[] = [];
  readonly EVENTOS = EVENTOS;
  error = '';
  exito = '';
  cargando = false;
  showForm = false;
  form: Partial<Automatizacion> = {};

  ngOnInit(): void {
    this.cargar();
    this.api.get<Plantilla[]>('/plantillas').subscribe({
      next: (r) => {
        this.plantillas = r;
        this.cdr.markForCheck();
      },
      error: () => this.cdr.markForCheck(),
    });
  }

  cargar(): void {
    withLoading(
      this,
      this.api.get<Automatizacion[]>('/automatizaciones'),
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

  nueva(): void {
    this.form = {
      nombre: '',
      evento: EVENTOS[0],
      minutosAntes: 0,
      plantillaId: undefined,
      condicion: '',
      activa: true,
    };
    this.showForm = true;
  }

  editar(a: Automatizacion): void {
    this.form = { ...a };
    this.showForm = true;
  }

  toggle(a: Automatizacion): void {
    const r = a.activa
      ? this.api.post<Automatizacion>(`/automatizaciones/${a.id}/desactivar`, {})
      : this.api.post<Automatizacion>(`/automatizaciones/${a.id}/activar`, {});
    r.subscribe({
      next: () => this.cargar(),
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  guardando = false;

  guardar(): void {
    if (this.guardando) return;
    if (!this.form.nombre || !this.form.evento || !this.form.plantillaId) {
      this.error = 'Completa nombre, evento y plantilla';
      this.cdr.markForCheck();
      return;
    }
    const body = {
      nombre: this.form.nombre,
      evento: this.form.evento,
      minutosAntes: this.form.minutosAntes ?? 0,
      plantillaId: Number(this.form.plantillaId),
      condicion: this.form.condicion || null,
      activa: this.form.activa ?? true,
    };
    const req = this.form.id
      ? this.api.put<Automatizacion>(`/automatizaciones/${this.form.id}`, body)
      : this.api.post<Automatizacion>('/automatizaciones', body);
    this.guardando = true;
    req.subscribe({
      next: () => {
        this.guardando = false;
        this.exito = 'Automatización guardada correctamente';
        this.error = '';
        setTimeout(() => {
          this.exito = '';
          this.cdr.markForCheck();
        }, 3000);
        this.showForm = false;
        this.cdr.markForCheck();
        this.cargar();
      },
      error: (e) => {
        this.guardando = false;
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  async eliminar(a: Automatizacion): Promise<void> {
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Eliminar automatización',
        mensaje: '¿Eliminar esta automatización?',
        confirmarTexto: 'Eliminar',
      }))
    )
      return;
    this.api.del<void>(`/automatizaciones/${a.id}`).subscribe({
      next: () => this.cargar(),
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  close(e: MouseEvent): void {
    if (e.target === e.currentTarget) this.showForm = false;
  }

  stop(e: MouseEvent): void {
    e.stopPropagation();
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a?.status === 409 || a?.status === 400
      ? (a.error?.message ?? 'Datos inválidos')
      : 'Error de conexión';
  }

  /** Escape cierra el modal/drawer mientras esté abierto. */
  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    if (this.showForm) this.showForm = false;
  }
}
