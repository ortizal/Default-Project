import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Api } from '../core/api';

export interface AgendaContexto {
  odontologoId: number;
  odontologo: string;
  especialidad: string | null;
  paciente: string;
  servicioId: number | null;
  expiraEnMinutos: number;
}

export interface AgendaDia {
  fecha: string;
  estado: 'DISPONIBLE' | 'POCA' | 'SIN_DISPONIBILIDAD' | 'NO_LABORABLE' | 'PASADO';
  cupos: number;
}

export interface AgendaSlot {
  horaInicio: string;
  horaFin: string;
  odontologoId: number;
  odontologo: string;
  /** Cupo firmado por el backend (§15): se devuelve al confirmar en vez de fecha/hora "a ojo". */
  slotToken?: string | null;
}

export interface AgendaReserva {
  estado: 'CREADA' | 'REPETIDA' | 'OCUPADA' | 'ERROR';
  cita: {
    servicioNombre: string;
    doctorNombre: string;
    fecha: string;
    horaInicio: string;
  } | null;
  alternativas: AgendaSlot[];
  mensaje: string;
}

export const ESTADOS_DIA: Record<AgendaDia['estado'], string> = {
  DISPONIBLE: 'ok',
  POCA: 'poca',
  SIN_DISPONIBILIDAD: 'vacio',
  NO_LABORABLE: 'cerrado',
  PASADO: 'pasado',
};

export const ETIQUETA_DIA: Record<AgendaDia['estado'], string> = {
  DISPONIBLE: 'Disponible',
  POCA: 'Pocos cupos',
  SIN_DISPONIBILIDAD: 'Sin disponibilidad',
  NO_LABORABLE: 'No atiende',
  PASADO: 'Día pasado',
};

/** Nombre corto del mes en español para el encabezado del calendario. */
export function nombreMes(anio: number, mes: number): string {
  const nombres = [
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
  ];
  return `${nombres[mes - 1]} ${anio}`;
}

/**
 * Calendario público de reserva con enlace temporal (§10–§13 del plan):
 * móvil primero, sólo el mes actual y el siguiente, cupos en vivo y
 * confirmación con revalidación en el backend.
 */
@Component({
  selector: 'app-agenda-publica',
  templateUrl: './agenda-publica.html',
  styleUrl: './agenda-publica.css',
  imports: [CommonModule, FormsModule],
})
export class AgendaPublicaComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly route = inject(ActivatedRoute);
  private readonly cdr = inject(ChangeDetectorRef);

  token = '';
  contexto: AgendaContexto | null = null;
  error = '';
  cargando = false;

  anio = 0;
  mes = 0;
  dias: AgendaDia[] = [];
  diaSeleccionado: string | null = null;
  slots: AgendaSlot[] = [];
  slotSeleccionado: AgendaSlot | null = null;

  reservando = false;
  reserva: AgendaReserva | null = null;
  /** Clave idempotente del intento de reserva (§15): se reutiliza al reintentar. */
  idempotencyKey = '';

  private readonly hoy = new Date();

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('t') ?? '';
    const ahora = new Date();
    this.anio = ahora.getFullYear();
    this.mes = ahora.getMonth() + 1;
    if (!this.token) {
      this.error = 'Este enlace no es válido. Pide uno nuevo por WhatsApp.';
      return;
    }
    this.cargarContexto();
  }

  get mesActual(): boolean {
    return this.anio === this.hoy.getFullYear() && this.mes === this.hoy.getMonth() + 1;
  }

  get mesSiguientePermitido(): boolean {
    const siguiente = new Date(this.hoy.getFullYear(), this.hoy.getMonth() + 1, 1);
    return this.anio < siguiente.getFullYear()
      || (this.anio === siguiente.getFullYear() && this.mes < siguiente.getMonth() + 1);
  }

  get etiquetaMes(): string {
    return nombreMes(this.anio, this.mes);
  }

  cambiarMes(paso: number): void {
    const siguiente = this.mes + paso;
    if (siguiente < 1) {
      this.mes = 12;
      this.anio -= 1;
    } else if (siguiente > 12) {
      this.mes = 1;
      this.anio += 1;
    } else {
      this.mes = siguiente;
    }
    this.diaSeleccionado = null;
    this.slots = [];
    this.slotSeleccionado = null;
    this.reserva = null;
    this.idempotencyKey = '';
    this.cargarDias();
  }

  estadoDe(dia: AgendaDia): string {
    return ESTADOS_DIA[dia.estado];
  }

  seleccionarDia(dia: AgendaDia): void {
    if (dia.estado === 'PASADO' || dia.estado === 'SIN_DISPONIBILIDAD'
      || dia.estado === 'NO_LABORABLE') {
      return;
    }
    this.diaSeleccionado = dia.fecha;
    this.slotSeleccionado = null;
    this.reserva = null;
    this.idempotencyKey = '';
    this.cargarSlots(dia.fecha);
  }

  seleccionarSlot(slot: AgendaSlot): void {
    this.slotSeleccionado = slot;
    this.reserva = null;
    this.idempotencyKey = this.nuevaClaveIdempotencia();
    this.cdr.markForCheck();
  }

  confirmar(): void {
    if (!this.slotSeleccionado || !this.diaSeleccionado || this.reservando) {
      return;
    }
    this.reservando = true;
    this.error = '';
    if (!this.idempotencyKey) {
      this.idempotencyKey = this.nuevaClaveIdempotencia();
    }
    const body = {
      token: this.token,
      fecha: this.diaSeleccionado,
      horaInicio: this.slotSeleccionado.horaInicio,
      odontologoId: this.slotSeleccionado.odontologoId,
      servicioId: this.contexto?.servicioId ?? null,
      idempotencyKey: this.idempotencyKey,
      slotToken: this.slotSeleccionado.slotToken ?? null,
    };
    this.api.post<AgendaReserva>('/public/agenda/reservar', body).subscribe({
      next: (r) => {
        this.reservando = false;
        this.reserva = r;
        if (r.estado === 'OCUPADA') {
          this.slots = r.alternativas ?? [];
          this.slotSeleccionado = null;
        }
        if (r.estado === 'CREADA' || r.estado === 'REPETIDA') {
          this.cargarDias();
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.reservando = false;
        this.error = 'No pudimos confirmar tu cita. Inténtalo de nuevo en unos minutos.';
        this.cdr.markForCheck();
      },
    });
  }

  reiniciar(): void {
    this.reserva = null;
    this.slotSeleccionado = null;
    this.diaSeleccionado = null;
    this.slots = [];
    this.idempotencyKey = '';
  }

  /** Reintenta la última carga fallida sin perder el día elegido (§13). */
  reintentar(): void {
    this.error = '';
    if (!this.contexto) {
      this.cargarContexto();
    } else if (this.diaSeleccionado) {
      this.cargarSlots(this.diaSeleccionado);
    } else {
      this.cargarDias();
    }
    this.cdr.markForCheck();
  }

  private nuevaClaveIdempotencia(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }

  private cargarContexto(): void {
    this.cargando = true;
    this.api.get<AgendaContexto>(`/public/agenda/context?t=${encodeURIComponent(this.token)}`).subscribe({
      next: (c) => {
        this.contexto = c;
        this.cargando = false;
        this.cargarDias();
      },
      error: () => {
        this.cargando = false;
        this.error = 'El enlace caducó o no es válido. Pide uno nuevo por WhatsApp.';
        this.cdr.markForCheck();
      },
    });
  }

  private cargarDias(): void {
    this.api
      .get<AgendaDia[]>(`/public/agenda/dias?t=${encodeURIComponent(this.token)}&year=${this.anio}&month=${this.mes}`)
      .subscribe({
        next: (dias) => {
          this.dias = dias;
          this.cdr.markForCheck();
        },
        error: () => {
          this.error = 'No pudimos cargar la disponibilidad. Inténtalo de nuevo.';
          this.cdr.markForCheck();
        },
      });
  }

  private cargarSlots(fecha: string): void {
    this.cargando = true;
    this.api
      .get<AgendaSlot[]>(`/public/agenda/slots?t=${encodeURIComponent(this.token)}&fecha=${fecha}`)
      .subscribe({
        next: (slots) => {
          this.slots = slots;
          this.cargando = false;
          this.cdr.markForCheck();
        },
        error: () => {
          this.cargando = false;
          this.error = 'No pudimos cargar los cupos de ese día.';
          this.cdr.markForCheck();
        },
      });
  }
}
