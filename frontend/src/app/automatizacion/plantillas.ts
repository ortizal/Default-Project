import { Component, OnInit, ChangeDetectorRef, inject, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Plantilla } from '../core/models';
import { withLoading } from '../core/loading';
import { UiPageHeaderComponent, UiTableComponent, UiConfirmService } from '../ui';

@Component({
  selector: 'app-plantillas',
  templateUrl: './plantillas.html',
  imports: [CommonModule, FormsModule, UiPageHeaderComponent, UiTableComponent],
})
export class PlantillasComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);

  private readonly confirmacion = inject(UiConfirmService);

  items: Plantilla[] = [];
  error = '';
  exito = '';
  cargando = false;
  showForm = false;
  form: Partial<Plantilla> = {};

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    withLoading(this, this.api.get<Plantilla[]>('/plantillas'), undefined, this.cdr).subscribe({
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
    this.form = { nombre: '', contenido: '', activa: true };
    this.showForm = true;
  }

  editar(p: Plantilla): void {
    this.form = { ...p };
    this.showForm = true;
  }
  guardando = false;

  guardar(): void {
    if (this.guardando) return;
    if (!this.form.nombre || !this.form.contenido) {
      this.error = 'Completa el nombre y el contenido de la plantilla';
      this.cdr.markForCheck();
      return;
    }
    const body = {
      nombre: this.form.nombre,
      contenido: this.form.contenido,
      activa: this.form.activa ?? true,
    };
    const req = this.form.id
      ? this.api.put<Plantilla>(`/plantillas/${this.form.id}`, body)
      : this.api.post<Plantilla>('/plantillas', body);
    this.guardando = true;
    req.subscribe({
      next: () => {
        this.guardando = false;
        this.exito = 'Plantilla guardada correctamente';
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

  async eliminar(p: Plantilla): Promise<void> {
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Eliminar plantilla',
        mensaje: '¿Eliminar esta plantilla?',
        confirmarTexto: 'Eliminar',
      }))
    )
      return;
    this.api.del<void>(`/plantillas/${p.id}`).subscribe({
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
