import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { Page, Usuario } from '../core/models';
import { withLoading } from '../core/loading';

@Component({
  selector: 'app-usuarios',
  templateUrl: './usuarios.html',
  imports: [CommonModule, FormsModule],
})
export class UsuariosComponent implements OnInit {
  readonly ROLES = ['SUPER_ADMIN', 'ADMIN', 'RECEPCION', 'ODONTOLOGO'];
  items: Usuario[] = [];
  q = '';
  page = 0;
  size = 15;
  totalPages = 1;
  cargando = false;
  error = '';
  showForm = false;
  form: Partial<Usuario> & { password?: string; nuevaPassword?: string } = {};
  rolesSel: string[] = [];

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.cargar(0);
  }

  cargar(p: number): void {
    this.error = '';
    const seq = this.page = Math.max(p, 0);
    const params = new URLSearchParams({ page: String(seq), size: String(this.size) });
    if (this.q.trim()) params.set('q', this.q.trim());
    withLoading(this, this.api.get<Page<Usuario>>(`/usuarios?${params.toString()}`), undefined, this.cdr).subscribe({
      next: (r) => {
        this.items = r.content;
        this.totalPages = Math.max(r.totalPages ?? 1, 1);
        this.page = Math.min(seq, this.totalPages - 1);
        this.cdr.markForCheck();
      },
      error: (e) => { this.error = this.msg(e); this.cdr.markForCheck(); },
    });
  }

  nuevo(): void {
    this.form = { username: '', email: '', nombres: '', apellidos: '', telefono: '', password: '' };
    this.rolesSel = [];
    this.showForm = true;
    this.cdr.markForCheck();
  }

  editar(u: Usuario): void {
    this.form = { ...u };
    this.rolesSel = Array.isArray(u.roles) ? [...u.roles] : [];
    this.showForm = true;
    this.cdr.markForCheck();
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
      this.cdr.markForCheck();
      return;
    }
    if (!this.form.id && !this.form.password) {
      this.error = 'Debes indicar una contraseña para el nuevo usuario';
      this.cdr.markForCheck();
      return;
    }
    const base: Record<string, unknown> = {
      email: this.form.email,
      telefono: this.form.telefono || null,
      nombres: this.form.nombres,
      apellidos: this.form.apellidos,
      roles: this.rolesSel,
    };
    if (this.form.id && this.form.nuevaPassword && this.form.nuevaPassword.trim()) {
      base['nuevaPassword'] = this.form.nuevaPassword.trim();
    }
    const req = this.form.id
      ? this.api.put<Usuario>(`/usuarios/${this.form.id}`, base)
      : this.api.post<Usuario>('/usuarios', {
          username: this.form.username,
          password: this.form.password,
          ...base,
        });
    this.cargando = true;
    this.cdr.markForCheck();
    req.subscribe({
      next: () => {
        this.showForm = false;
        this.cdr.markForCheck();
        this.cargar(this.page);
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cargando = false;
        this.cdr.markForCheck();
      },
    });
  }

  cambiarEstado(u: Usuario): void {
    this.api.post<Usuario>(`/usuarios/${u.id}/estado`, {}).subscribe({
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
    const a = e as { error?: { message?: string }; status?: number };
    if (a?.status === 403) return 'Sin permisos para gestionar usuarios';
    return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}