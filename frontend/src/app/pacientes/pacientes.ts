import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Paciente, Page, Tutor, nombreEstado } from '../core/models';
import { AppDateComponent } from '../core/app-date.component';
import { withLoading } from '../core/loading';
import { UiPageHeaderComponent, UiButtonComponent, UiTableComponent, UiPaginationComponent, UiBadgeComponent } from '../ui';

@Component({
  selector: 'app-pacientes',
  templateUrl: './pacientes.html',
  imports: [CommonModule, FormsModule, AppDateComponent, UiPageHeaderComponent, UiButtonComponent, UiTableComponent, UiPaginationComponent, UiBadgeComponent],
})
export class PacientesComponent implements OnInit {
  items: Paciente[] = [];
  q = '';
  estado = 'ACTIVO';
  page = 0;
  size = 15;
  totalPages = 1;
  error = '';
  cargando = false;
  showForm = false;
  form: Partial<Paciente> = {};
  tutorPadre: Tutor = { parentesco: 'PADRE', nombres: '' };
  tutorMadre: Tutor = { parentesco: 'MADRE', nombres: '' };
  private reqSeq = 0;

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargar(0);
  }

  cargar(p: number): void {
    this.error = '';
    const seq = ++this.reqSeq;
    const params = new URLSearchParams({ page: String(p), size: String(this.size) });
    if (this.q.trim()) params.set('q', this.q.trim());
    if (this.estado) params.set('estado', this.estado);
    withLoading(this, this.api.get<Page<Paciente>>(`/pacientes?${params.toString()}`), () => seq === this.reqSeq, this.cdr).subscribe({
      next: (r) => {
        if (seq !== this.reqSeq) return;
        this.items = r.content;
        this.totalPages = Math.max(r.totalPages ?? 1, 1);
        this.page = Math.min(p, this.totalPages - 1);
       this.cdr.markForCheck(); },
      error: (e) => {
        if (seq !== this.reqSeq) return;
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  nuevo(): void {
    this.form = { fechaNacimiento: '', estado: 'ACTIVO' };
    this.tutorPadre = { parentesco: 'PADRE', nombres: '' };
    this.tutorMadre = { parentesco: 'MADRE', nombres: '' };
    this.showForm = true;
    this.cdr.markForCheck();
  }

  editar(p: Paciente): void {
    this.form = { ...p, fechaNacimiento: p.fechaNacimiento || '' };
    const padre = (p.tutores || []).find((t) => t.parentesco === 'PADRE');
    const madre = (p.tutores || []).find((t) => t.parentesco === 'MADRE');
    this.tutorPadre = padre ? { ...padre } : { parentesco: 'PADRE', nombres: '' };
    this.tutorMadre = madre ? { ...madre } : { parentesco: 'MADRE', nombres: '' };
    this.showForm = true;
    this.cdr.markForCheck();
  }

  /** Teléfono en formato legible: +593991112233 → +593 99 111 2233 */
  formatoTelefono(telefono?: string): string {
    if (!telefono) return '-';
    const m = /^\+593(\d{9})$/.exec(telefono.trim());
    if (!m) return telefono;
    const n = m[1];
    return `+593 ${n.slice(0, 2)} ${n.slice(2, 5)} ${n.slice(5)}`;
  }

  get esMenor(): boolean {
    const fn = this.form.fechaNacimiento;
    if (!fn) return false;
    const nac = new Date(fn + 'T00:00:00');
    const hoy = new Date();
    let edad = hoy.getFullYear() - nac.getFullYear();
    const m = hoy.getMonth() - nac.getMonth();
    if (m < 0 || (m === 0 && hoy.getDate() < nac.getDate())) edad--;
    return edad < 18;
  }

  guardar(): void {
    if (!this.form.nombres || !this.form.apellidos) {
      this.error = 'Completa nombres y apellidos (campos obligatorios)';
      this.cdr.markForCheck();
      return;
    }
    if (this.esMenor) {
      const hayPadre = this.tutorPadre.nombres?.trim();
      const hayMadre = this.tutorMadre.nombres?.trim();
      if (!hayPadre && !hayMadre) {
        this.error = 'El paciente es menor de edad: debe registrar al padre y/o madre como tutor';
        this.cdr.markForCheck();
        return;
      }
    }
    const tutores = this.tutoresValidos();
    const body: Record<string, unknown> = {
      cedula: this.form.cedula || null,
      nombres: this.form.nombres,
      apellidos: this.form.apellidos,
      telefono: this.form.telefono || null,
      email: this.form.email || null,
      fechaNacimiento: this.form.fechaNacimiento || null,
      direccion: this.form.direccion || null,
      ciudad: this.form.ciudad || null,
      observaciones: this.form.observaciones || null,
      estado: this.form.estado || 'ACTIVO',
      tutores,
    };
    const req = this.form.id
      ? this.api.put<Paciente>(`/pacientes/${this.form.id}`, body)
      : this.api.post<Paciente>('/pacientes', body);
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cdr.markForCheck();
        this.cargar(this.page);
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  private tutoresValidos(): Tutor[] {
    const tutores: Tutor[] = [];
    if (this.tutorPadre.nombres?.trim()) tutores.push(this.tutorPadre);
    if (this.tutorMadre.nombres?.trim()) tutores.push(this.tutorMadre);
    return tutores;
  }

  desactivar(p: Paciente): void {
    if (!confirm('¿Desactivar este paciente?')) return;
    this.api.del<void>(`/pacientes/${p.id}`).subscribe({
      next: () => this.cargar(this.page),
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
    const a = e as { error?: { message?: string; mensaje?: string }; status?: number };
    if (a?.status === 409 || a?.status === 400) return a.error?.message ?? a.error?.mensaje ?? 'Datos inválidos';
    return 'Error de conexión';
  }
}