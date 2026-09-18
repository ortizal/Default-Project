export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  username: string;
  roles: string[];
}

export interface Page<T> {
  content: T[];
  pageable?: { pageNumber?: number; pageSize?: number };
  totalPages?: number;
  totalElements?: number;
  last?: boolean;
  first?: boolean;
}

export interface Tutor {
  id?: number;
  parentesco: 'PADRE' | 'MADRE';
  nombres: string;
  apellidos?: string;
  telefono?: string;
}

export interface Paciente {
  id: number;
  cedula?: string;
  nombres: string;
  apellidos: string;
  telefono?: string;
  email?: string;
  fechaNacimiento?: string;
  direccion?: string;
  observaciones?: string;
  estado?: string;
  tutores?: Tutor[];
  createdAt?: string;
  updatedAt?: string;
}

export interface Odontologo {
  id: number;
  nombres: string;
  apellidos: string;
  especialidad?: string;
  telefono?: string;
  email?: string;
  etiquetas?: string;
  googleCalendarId?: string;
  estado?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Servicio {
  id: number;
  nombre: string;
  descripcion?: string;
  duracionMinutos: number;
  precio: number;
  estado?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Horario {
  id: number;
  odontologoId: number;
  odontologoNombre: string;
  diaSemana: number;
  horaInicio: string;
  horaFin: string;
  intervaloMinutos: number;
  estado?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Cita {
  id: number;
  pacienteId: number;
  pacienteNombre: string;
  pacienteTelefono?: string;
  doctorId: number;
  doctorNombre: string;
  servicioId: number;
  servicioNombre: string;
  duracionMinutos: number;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  estado: string;
  confirmada?: boolean;
  confirmadaAt?: string;
  confirmationSource?: string;
  observaciones?: string;
  canceladaAt?: string;
  canceladaMotivo?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface Slot {
  horaInicio: string;
  horaFin: string;
}

export interface Bloqueo {
  id: number;
  odontologoId: number;
  odontologoNombre: string;
  fecha: string;
  horaInicio?: string;
  horaFin?: string;
  motivo?: string;
  createdAt?: string;
}

export interface DashboardData {
  citasDeHoy: number;
  citasPendientesHoy: number;
  citasConfirmadasHoy: number;
  citasCanceladasHoy: number;
  noAsistieronHoy: number;
  pacientesNuevosMes: number;
  conversacionesAbiertas: number;
  mensajesEnviados: number;
  mensajesConError: number;
  notificacionesEnviadas: number;
  notificacionesConError: number;
}

export interface EstadisticasData {
  citasPorDia: ReporteSerie[];
  confirmaciones: ReporteSerie[];
  cancelaciones: ReporteSerie[];
  noAsistencia: ReporteSerie[];
  citasPorOdontologo: ReporteDetalle[];
  serviciosMasSolicitados: ReporteDetalle[];
  porEstado: ReporteDetalle[];
  confirmacionesPorFuente: ReporteDetalle[];
}

export interface ReporteDetalle {
  nombre: string;
  total: number;
}

export interface ReporteSerie {
  fecha: string;
  total: number;
}

export interface WhatsappSesion {
  id: number;
  sesionId: string;
  nombre?: string;
  estado: string;
  estadoDetalle?: string;
  qr?: string;
  lastError?: string;
  updatedAt?: string;
  proveedor?: string;
}

export interface Conversacion {
  id: number;
  sesionId: string;
  telefono: string;
  nombreContacto?: string;
  estado: string;
  pacienteId?: number;
  pacienteNombres?: string;
  ultimoMensaje?: string;
  ultimoMensajeAt?: string;
}

export interface Mensaje {
  id: number;
  direccion: string;
  texto: string;
  estado?: string;
  remitente?: string;
  error?: string;
  receivedAt: string;
}

export interface ConversacionDetalle {
  conversacion: Conversacion;
  mensajes: Mensaje[];
}

export interface Plantilla {
  id: number;
  nombre: string;
  contenido: string;
  activa?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface Automatizacion {
  id: number;
  nombre: string;
  evento: string;
  minutosAntes: number;
  plantillaId: number;
  plantillaNombre: string;
  condicion?: string;
  activa?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface Notificacion {
  id: number;
  citaId: number;
  pacienteId: number;
  telefono: string;
  evento: string;
  plantilla: string;
  mensaje: string;
  programadaAt?: string;
  enviadaAt?: string;
  estado?: string;
  intentos?: number;
  error?: string;
  createdAt?: string;
}

export interface AgenteConversacion {
  id: number;
  telefono: string;
  nombreContacto?: string;
  estado: string;
  pacienteId?: number;
  pacienteNombre?: string;
  intencion?: string;
  agenteActivo?: boolean;
  contextoAgente?: string;
  ultimoMensaje?: string;
  ultimoMensajeAt?: string;
}

export interface Usuario {
  id: number;
  username: string;
  email: string;
  nombres: string;
  apellidos: string;
  telefono?: string;
  estado?: string;
  roles: string[];
  ultimoLogin?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface BloqueoRequest {
  odontologoId: number;
  fecha: string;
  horaInicio?: string;
  horaFin?: string;
  motivo?: string;
}

export interface GoogleCalendario {
  id: number;
  calendarId: string;
  summary: string;
  timeZone?: string | null;
  seleccionado?: boolean;
}

export interface GoogleStatus {
  configurada: boolean;
  conectada: boolean;
  email?: string;
  calendarios: GoogleCalendario[];
  calendarioSeleccionado?: GoogleCalendario;
  citasSincronizadas: number;
  citasPendientes: number;
  citasConError: number;
}

export interface GoogleCredential {
  id?: number;
  clientId: string;
  clientSecret: string;
  redirectUri: string;
  authBaseUrl: string;
  oauthBaseUrl: string;
  apiBaseUrl: string;
  configurada: boolean;
}

export interface GoogleConnect {
  authUrl: string;
}

export interface GoogleSyncResult {
  creados: number;
  actualizados: number;
  cancelados: number;
  errores: number;
  sincronizadas: number;
}

export interface Auditoria {
  id: number;
  usuarioId?: number;
  accion: string;
  modulo: string;
  entidad?: string;
  entidadId?: number;
  datosAnteriores?: Record<string, unknown> | string | null;
  datosNuevos?: Record<string, unknown> | string | null;
  ip?: string;
  createdAt?: string;
}

export const DIAS: string[] = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

export function nombreEstado(estado: string): string {
  return (estado || '').toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
}