import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Api } from '../core/api';
import { Bloqueo, Cita } from '../core/models';

type CitaGrid = Cita & { estadoNombre: string };

interface DiaDatos {
  iso: string;
  numero: number;
  enOtroMes?: boolean;
  citas: CitaGrid[];
  bloqueado: boolean;
  inactivo: boolean;
}

@Component({
  selector: 'app-calendario',
  templateUrl: './calendario.html',
  styleUrl: './calendario.css',
  imports: [CommonModule],
  standalone: true,
})
export class CalendarioComponent {
  @Input() modo: 'semana' | 'mes' = 'semana';
  @Input() set fecha(v: string) {
    this.ancla = v;
    this.cargar();
  }
  @Input() set odontologoId(v: number | undefined) {
    this.odontologo = v ?? 0;
    this.horariosDia = undefined;
    this.cargar();
  }
  @Output() irDia = new EventEmitter<string>();

  dias: DiaDatos[] = [];
  titulo = '';
  hoy = new Date().toISOString().slice(0, 10);
  error = '';
  private ancla = '';
  private odontologo = 0;
  private citas: Cita[] = [];
  private bloqueos: Bloqueo[] = [];
  private horariosDia?: Set<number>;

  constructor(private readonly api: Api) {}

  cargar(): void {
    if (!this.ancla || !this.odontologo) {
      this.dias = [];
      return;
    }
    if (!this.horariosDia) {
      this.api.get<{ id: number; diaSemana: number; estado?: string }[]>(`/horarios?odontologoId=${this.odontologo}`).subscribe({
        next: (r) => {
          this.horariosDia = new Set(r.filter((h) => h.estado === 'ACTIVO').map((h) => h.diaSemana));
          this.construir();
        },
        error: () => this.construir(),
      });
    }
    const rango = this.rango();
    this.api
      .get<{ content: Cita[] }>(
        `/citas?desde=${rango.desde}&hasta=${rango.hasta}&page=0&size=1000&doctorId=${this.odontologo}`,
      )
      .subscribe({
        next: (r) => {
          this.citas = r.content;
          this.construir();
        },
        error: (e) => (this.error = this.msg(e)),
      });
    this.api.get<Bloqueo[]>(`/agenda/bloqueos?odontologoId=${this.odontologo}&desde=${rango.desde}&hasta=${rango.hasta}`).subscribe({
      next: (r) => {
        this.bloqueos = r;
        this.construir();
      },
      error: () => undefined,
    });
  }

  private rango(): { desde: string; hasta: string } {
    const ancla = new Date(this.ancla + 'T12:00:00');
    if (this.modo === 'semana') {
      const dia = ancla.getDay();
      const diff = dia === 0 ? -6 : 1 - dia;
      const lunes = new Date(ancla);
      lunes.setDate(ancla.getDate() + diff);
      const domingo = new Date(lunes);
      domingo.setDate(lunes.getDate() + 6);
      return { desde: this.iso(lunes), hasta: this.iso(domingo) };
    }
    const primero = new Date(ancla.getFullYear(), ancla.getMonth(), 1);
    const ultimo = new Date(ancla.getFullYear(), ancla.getMonth() + 1, 0);
    return { desde: this.iso(primero), hasta: this.iso(ultimo) };
  }

  construir(): void {
    if (!this.ancla || !this.odontologo) return;
    const porFecha = new Map<string, CitaGrid[]>();
    for (const c of this.citas) {
      const lista = porFecha.get(c.fecha) ?? [];
      lista.push({ ...c, estadoNombre: this.nombreEstado(c.estado) });
      porFecha.set(c.fecha, lista);
    }
    const bloqueadas = new Set(this.bloqueos.map((b) => b.fecha));

    const celdas: DiaDatos[] = [];
    if (this.modo === 'semana') {
      const { desde } = this.rango();
      const lunes = new Date(desde + 'T12:00:00');
      for (let i = 0; i < 7; i++) {
        const fecha = new Date(lunes);
        fecha.setDate(lunes.getDate() + i);
        const isoDate = this.iso(fecha);
        celdas.push(this.celda(isoDate, fecha.getDate(), false, porFecha, bloqueadas));
      }
      const r = this.rango();
      this.titulo = `${this.formato(r.desde)} — ${this.formato(r.hasta)}`;
    } else {
      const ancla = new Date(this.ancla + 'T12:00:00');
      const offset = (new Date(ancla.getFullYear(), ancla.getMonth(), 1).getDay() + 6) % 7;
      const total = new Date(ancla.getFullYear(), ancla.getMonth() + 1, 0).getDate();
      let cursor = -offset + 1;
      while (celdas.length < total + offset) {
        const fecha = new Date(ancla.getFullYear(), ancla.getMonth(), cursor);
        const otroMes = cursor < 1 || cursor > total;
        celdas.push(this.celda(this.iso(fecha), fecha.getDate(), otroMes, porFecha, bloqueadas));
        cursor++;
      }
      while (celdas.length % 7 !== 0) {
        const fecha = new Date(ancla.getFullYear(), ancla.getMonth(), cursor);
        celdas.push(this.celda(this.iso(fecha), fecha.getDate(), true, porFecha, bloqueadas));
        cursor++;
      }
      const m = ancla.toLocaleDateString('es-EC', { month: 'long', year: 'numeric' });
      this.titulo = m.charAt(0).toUpperCase() + m.slice(1);
    }
    this.dias = celdas;
  }

  private celda(
    iso: string,
    numero: number,
    otroMes: boolean,
    porFecha: Map<string, CitaGrid[]>,
    bloqueadas: Set<string>,
  ): DiaDatos {
    const fecha = new Date(iso + 'T12:00:00');
    const dia = ((fecha.getDay() + 6) % 7) + 1;
    const inactivo = this.horariosDia ? !this.horariosDia.has(dia) && !otroMes : false;
    const citas = (porFecha.get(iso) ?? []).sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
    return { iso, numero, enOtroMes: otroMes, citas, bloqueado: bloqueadas.has(iso), inactivo };
  }

  navegar(dir: number): void {
    const ancla = new Date(this.ancla + 'T12:00:00');
    if (this.modo === 'semana') {
      ancla.setDate(ancla.getDate() + dir * 7);
    } else {
      ancla.setMonth(ancla.getMonth() + dir);
    }
    this.ancla = this.iso(ancla);
    this.cargar();
  }

  badge(estado: string): string {
    switch (estado) {
      case 'CONFIRMADA':
        return 'ok';
      case 'REALIZADA':
        return 'info';
      case 'CANCELADA':
        return 'bad';
      case 'NO_ASISTIO':
        return 'warn';
      default:
        return 'dim';
    }
  }

  private nombreEstado(estado: string): string {
    return (estado || '').toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
  }

  private iso(d: Date): string {
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${d.getFullYear()}-${m}-${dd}`;
  }

  private formato(iso: string): string {
    const d = new Date(iso + 'T12:00:00');
    return d.toLocaleDateString('es-EC', { day: 'numeric', month: 'short' });
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string }; status?: number };
    return a?.status === 400 || a?.status === 409 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
  }
}