import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Page, Usuario } from '../core/models';

@Component({
  selector: 'app-usuarios',
  templateUrl: './usuarios.html',
  imports: [CommonModule, FormsModule],
})
export class UsuariosComponent {
  readonly ROLES = ['SUPER_ADMIN', 'ADMIN', 'RECEPCION', 'ODONTOLOGO'];
  items: Usuario[] = [];
  q = '';
  page = 0;
  size = 15;
  totalPages = 1;
  error = '';
  showForm = false;
  form: Partial<Usuario> & { password?: string } = {};
  rolesSel: string[] = [];

  constructor(private readonly api: Api) {
    this.cargar(0);
  }

  cargar(p: number): void {
    this.error = '';
    const params = new URLSearchParams({ page: String(p), size: String(this.size) });
    if (this.q.trim()) params.set('q', this.q.trim());
    this.api.get<Page<Usuario>>(`/usuarios?${params.toString()}`).subscribe({
      next: (r) => {
        this.items = r.content;
        this.totalPages = Math.max(r.totalPages ?? 1, 1);
        this.page = Math.min(p, this.totalPages - 1);
      },
      error: (e) => (this.error = this.msg(e)),
    });
  }

  nuevo(): void {
    this.form = { username: '', email: '', nombres: '', apellidos: '', telefono: '', password: '' };
    this.rolesSel = [];
    this.showForm = true;
  }

  editar(u: Usuario): void {
    this.form = { ...u };
    this.rolesSel = Array.isArray(u.roles) ? [...u.roles] : [];
    this.showForm = true;
  }

  rolMarcado(r: string): boolean {
    return this.rolesSel.includes(r);
  }

  toggleRol(r: string): void {
    this.rolesSel = this.rolesSel.includes(r) ? this.rolesSel.filter((x) => x !== r) : [...this.rolesSel, r];
  }

  guardar(): void {
    if (!this.form.username || !this.form.email || !this.form.nombres || !this.form.apellidos || this.rolesSel.length === 0) {
      this.error = 'Completa usuario, email, nombres, apellidos y al menos un rol';
      return;
    }
    if (!this.form.id && !this.form.password) {
      this.error = 'Debes indicar una contraseña para el nuevo usuario';
      return;
    }
    const base = {
      email: this.form.email,
      telefono: this.form.telefono || null,
      nombres: this.form.nombres,
      apellidos: this.form.apellidos,
      roles: this.rolesSel,
    };
    const req = this.form.id
      ? this.api.put<Usuario>(`/usuarios/${this.form.id}`, base)
      : this.api.post<Usuario>('/usuarios', {
          username: this.form.username,
          password: this.form.password,
          ...base,
        });
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cargar(this.page);
      },
      error: (e) => (this.error = this.msg(e)),
    });
  }

  cambiarEstado(u: Usuario): void {
    this.api.post<Usuario>(`/usuarios/${u.id}/estado`, {}).subscribe({
      next: () => this.cargar(this.page),
      error: (e) => (this.error = this.msg(e)),
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
    if (a?.status === 403) return 'Sin permisos para gestionar usuarios';
    return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}