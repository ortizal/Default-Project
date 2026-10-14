import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Api } from '../core/api';
import { DashboardData, EstadisticasData, ReporteSerie } from '../core/models';
import { withLoading } from '../core/loading';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { UiModalComponent, UiPageHeaderComponent } from '../ui';

type DashboardDetalle =
  | 'citas'
  | 'tendencia'
  | 'cancelaciones'
  | 'ranking'
  | 'pacientes'
  | 'conversaciones'
  | 'mensajes'
  | 'notificaciones';

function fechaLocal(fecha: Date): string {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
}

function desplazarFecha(dias: number): string {
  const fecha = new Date();
  fecha.setDate(fecha.getDate() + dias);
  return fechaLocal(fecha);
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
  imports: [
    RouterLink,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    UiModalComponent,
    UiPageHeaderComponent,
  ],
})
export class DashboardComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);

  d: DashboardData | null = null;
  estadisticas: EstadisticasData | null = null;
  cargando = true;
  error = false;
  detalle: DashboardDetalle | null = null;

  readonly hoy = fechaLocal(new Date());
  readonly inicioPeriodoActual = desplazarFecha(-29);
  readonly inicioPeriodoAnterior = desplazarFecha(-59);
  readonly finPeriodoAnterior = desplazarFecha(-30);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    const params = new URLSearchParams({
      desde: this.inicioPeriodoAnterior,
      hasta: this.hoy,
    });
    this.cargando = true;
    this.error = false;
    withLoading(
      this,
      forkJoin({
        dashboard: this.api.get<DashboardData>('/reportes/dashboard'),
        estadisticas: this.api.get<EstadisticasData>(`/reportes/estadisticas?${params.toString()}`),
      }),
      undefined,
      this.cdr,
    ).subscribe({
      next: (r) => {
        this.d = r.dashboard;
        this.estadisticas = r.estadisticas;
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = true;
        this.cargando = false;
        this.cdr.markForCheck();
      },
    });
  }

  total(serie: ReporteSerie[] | undefined, desde: string, hasta: string): number {
    return (serie ?? []).reduce(
      (suma, dato) => suma + (dato.fecha >= desde && dato.fecha <= hasta ? Number(dato.total) || 0 : 0),
      0,
    );
  }

  get citasPeriodoActual(): number {
    return this.total(this.estadisticas?.citasPorDia, this.inicioPeriodoActual, this.hoy);
  }

  get citasPeriodoAnterior(): number {
    return this.total(this.estadisticas?.citasPorDia, this.inicioPeriodoAnterior, this.finPeriodoAnterior);
  }

  get variacionCitas(): number | null {
    if (this.citasPeriodoAnterior === 0) return null;
    return Math.round(((this.citasPeriodoActual - this.citasPeriodoAnterior) / this.citasPeriodoAnterior) * 100);
  }

  get cancelacionesPeriodo(): number {
    return this.total(this.estadisticas?.cancelaciones, this.inicioPeriodoActual, this.hoy);
  }

  get inasistenciasPeriodo(): number {
    return this.total(this.estadisticas?.noAsistencia, this.inicioPeriodoActual, this.hoy);
  }

  get tasaCancelacion(): number {
    return this.porcentaje(this.cancelacionesPeriodo, this.citasPeriodoActual);
  }

  get tasaInasistencia(): number {
    return this.porcentaje(this.inasistenciasPeriodo, this.citasPeriodoActual);
  }

  get tendencia14(): { fecha: string; etiqueta: string; total: number }[] {
    const porFecha = new Map(
      (this.estadisticas?.citasPorDia ?? []).map((dato) => [dato.fecha, Number(dato.total) || 0]),
    );
    return Array.from({ length: 14 }, (_, indice) => {
      const fecha = new Date();
      fecha.setDate(fecha.getDate() - (13 - indice));
      const clave = fechaLocal(fecha);
      return {
        fecha: clave,
        etiqueta: new Intl.DateTimeFormat('es-EC', { day: 'numeric', month: 'short' }).format(fecha),
        total: porFecha.get(clave) ?? 0,
      };
    });
  }

  get maxTendencia(): number {
    return Math.max(1, ...this.tendencia14.map((dia) => dia.total));
  }

  get odontologosTop(): { nombre: string; total: number }[] {
    return this.odontologosOrdenados.slice(0, 3);
  }

  get odontologosOrdenados(): { nombre: string; total: number }[] {
    return [...(this.estadisticas?.citasPorOdontologo ?? [])].sort((a, b) => b.total - a.total);
  }

  get serviciosTop(): { nombre: string; total: number }[] {
    return this.serviciosOrdenados.slice(0, 3);
  }

  get serviciosOrdenados(): { nombre: string; total: number }[] {
    return [...(this.estadisticas?.serviciosMasSolicitados ?? [])].sort((a, b) => b.total - a.total);
  }

  porcentaje(parte: number, total: number): number {
    return total > 0 ? Math.round((parte / total) * 100) : 0;
  }

  abrirDetalle(tipo: DashboardDetalle): void {
    this.detalle = tipo;
  }

  cerrarDetalle(): void {
    this.detalle = null;
  }

  get tituloDetalle(): string {
    const titulos: Record<DashboardDetalle, string> = {
      citas: 'Actividad de hoy',
      tendencia: 'Tendencia de citas',
      cancelaciones: 'Cancelaciones y asistencia',
      ranking: 'Actividad por equipo',
      pacientes: 'Nuevos pacientes',
      conversaciones: 'Conversaciones abiertas',
      mensajes: 'Rendimiento de mensajes',
      notificaciones: 'Notificaciones',
    };
    return this.detalle ? titulos[this.detalle] : '';
  }
}
