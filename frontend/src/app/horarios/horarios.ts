import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { DIAS, Horario, nombreEstado, Odontologo } from '../core/models';
import { withLoading } from '../core/loading';

@Component({
  selector: 'app-horarios',
  templateUrl: './horarios.html',
  imports: [CommonModule, FormsModule],
})
export class HorariosComponent implements OnInit {
  readonly DIAS = DIAS;
  readonly nombreEstado = nombreEstado;
  odontologos: Odontologo[] = [];
  odontologoSeleccionado = '';
  items: Horario[] = [];
  error = '';
  cargando = false;
  showForm = false;
  form: Partial<Horario> = {};

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.api.get<Odontologo[]>('/odontologos/activos').subscribe({
      next: (r) => {
        this.odontologos = r;
        if (r.length) {
          this.odontologoSeleccionado = String(r[0].id);
          this.cargar();
        }
        this.cdr.markForCheck();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  cargar(): void {
    if (!this.odontologoSeleccionado) return;
    withLoading(this, this.api.get<Horario[]>(`/horarios?odontologoId=${this.odontologoSeleccionado}`), undefined, this.cdr).subscribe({
      next: (r) => { this.items = r; this.cdr.markForCheck(); },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

nuevo(): void {
    this.form = { diaSemana: 1, horaInicio: '08:00', horaFin: '17:00', intervaloMinutos: 30, estado: 'ACTIVO' };
    this.showForm = true;
    this.cdr.markForCheck();
  }

  editar(h: Horario): void {
    this.form = { ...h };
    this.showForm = true;
    this.cdr.markForCheck();
  }
  guardar(): void {
    if (!this.form.diaSemana || !this.form.horaInicio || !this.form.horaFin) {
      this.error = 'Completa día, hora de inicio y hora de fin';
      this.cdr.markForCheck();
      return;
    }
    const body = { ...this.form, odontologoId: Number(this.odontologoSeleccionado), estado: this.form.estado || 'ACTIVO' };
    const req = this.form.id
      ? this.api.put<Horario>(`/horarios/${this.form.id}`, body)
      : this.api.post<Horario>('/horarios', body);
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cdr.markForCheck();
        this.cargar();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  eliminar(h: Horario): void {
    if (!confirm('¿Eliminar este horario?')) return;
    this.api.del<void>(`/horarios/${h.id}`).subscribe({
      next: () => this.cargar(),
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
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
    return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}