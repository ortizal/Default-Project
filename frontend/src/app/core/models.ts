export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  username: string;
  roles: string[];
  permisos: string[];
}

export interface Page<T> {
  content: T[];
  pageable?: { pageNumber?: number; pageSize?: number };
  totalPages?: number;
  totalElements?: number;
  last?: boolean;
  first?: boolean;
  /** Forma real de la API Spring: { content, page: { ... } }. */
  page?: { size: number; number: number; totalElements: number; totalPages: number };
}

/** Cuántas páginas y filas trajo el backend (acepta ambas formas). */
export function paginasDe<T>(r: Page<T>): number {
  return Math.max(r.page?.totalPages ?? r.totalPages ?? 1, 1);
}

export function totalDe<T>(r: Page<T>): number {
  return r.page?.totalElements ?? r.totalElements ?? r.content?.length ?? 0;
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
  ciudad?: string;
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

/** Día de excepción de atención: la clínica cierra o atiende en otra ventana. */
export interface ExcepcionHorario {
  id: number;
  odontologoId: number;
  odontologoNombre: string;
  fecha: string;
  tipo: 'CERRADO' | 'HORARIO_ESPECIAL';
  tipoNombre: string;
  horaInicio: string | null;
  horaFin: string | null;
  motivo: string | null;
  citasCanceladas?: number | null;
  createdAt?: string;
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

// ------------------------------------------------------------------
// Reportes clínicos (Fase E, §4.17)
// ------------------------------------------------------------------

export interface ReporteClinicoTotales {
  evoluciones: number;
  examenes: number;
  diagnosticos: number;
  odontogramas: number;
  periodontos: number;
  recetas: number;
  consentimientos: number;
  archivos: number;
  planes: number;
  citasAtendidas: number;
  pacientesAtendidos: number;
}

export interface ReporteDiagnosticoFrecuente {
  nombre: string;
  total: number;
  pacientes: number;
}

export interface ReporteTratamientoPendiente {
  pacienteId: number;
  paciente: string;
  planId: number;
  plan: string;
  lineaId: number;
  procedimiento: string;
  diente: number | null;
  superficie: string | null;
  prioridad: string | null;
  estado: string | null;
  precio: number | null;
}

export interface ReporteSinActividad {
  pacienteId: number;
  paciente: string;
}

export interface ReporteContinuidad {
  citasAtendidas: number;
  citasConfirmadas: number;
  citasCanceladas: number;
  citasNoAsistio: number;
  pacientesActivos: number;
  pacientesSinProximaCita: number;
  sinActividad90d: ReporteSinActividad[];
}

export interface ReporteProduccionClinica {
  procedimiento: string;
  cantidad: number;
  total: number;
}

export interface ReporteClinicoData {
  desde: string;
  hasta: string;
  totales: ReporteClinicoTotales;
  diagnosticosFrecuentes: ReporteDiagnosticoFrecuente[];
  tratamientosPendientes: ReporteTratamientoPendiente[];
  continuidad: ReporteContinuidad;
  produccionClinica: ReporteProduccionClinica[];
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
  destinatario: string;
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
  pacienteNombre?: string;
  telefono: string;
  destinatario?: string;
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

export interface ConversacionInicio {
  conversacionId: string;
  link: string;
  mensaje: string;
}

export interface MensajeRequest {
  telefono: string;
  texto: string;
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
  clientSecretConfigurado?: boolean;
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
// ---------------------------------------------------------------------------
// Historia clínica (Fase B del plan de ampliación)
// ---------------------------------------------------------------------------

export type TipoAntecedente =
  | 'ENFERMEDAD'
  | 'CIRUGIA'
  | 'HOSPITALIZACION'
  | 'ALERGIA'
  | 'MEDICACION'
  | 'ANTECEDENTE_ODONTOLOGICO'
  | 'HABITO';

export type EstadoAntecedente = 'ACTIVO' | 'CONTROLADO' | 'RESUELTO' | 'INACTIVO';
export type SeveridadDiagnostico = 'LEVE' | 'MODERADA' | 'SEVERA';
export type EstadoDiagnostico = 'SOSPECHA' | 'CONFIRMADO' | 'RESUELTO' | 'DESCARTADO';
export type EstadoEvolucion = 'ABIERTA' | 'CERRADA';

export interface Antecedente {
  id?: number;
  tipo: TipoAntecedente;
  nombre: string;
  detalle?: string | null;
  fechaDesde?: string | null;
  estado: EstadoAntecedente;
  observaciones?: string | null;
  createdAt?: string;
}

export interface ExamenClinico {
  id?: number;
  fecha?: string | null;
  odontologoId?: number | null;
  odontologo?: string | null;
  general?: string | null;
  extraoralAtm?: string | null;
  extraoralSimetria?: string | null;
  extraoralGanglios?: string | null;
  extraoralApertura?: string | null;
  intraoralMucosas?: string | null;
  intraoralLengua?: string | null;
  intraoralPaladar?: string | null;
  intraoralEncias?: string | null;
  intraoralHigiene?: string | null;
  intraoralOclusion?: string | null;
  observaciones?: string | null;
}

export interface DiagnosticoClinico {
  id?: number;
  nombre: string;
  region?: string | null;
  severidad: SeveridadDiagnostico;
  estado: EstadoDiagnostico;
  fecha?: string | null;
  odontologoId?: number | null;
  odontologo?: string | null;
  observaciones?: string | null;
}

export interface EvolucionAdenda {
  id: number;
  texto: string;
  autorId?: number | null;
  createdAt: string;
}

export interface Evolucion {
  id?: number;
  fecha?: string;
  odontologoId?: number | null;
  odontologo?: string | null;
  citaId?: number | null;
  motivo: string;
  hallazgos?: string | null;
  diagnostico?: string | null;
  procedimiento?: string | null;
  anestesia?: string | null;
  materiales?: string | null;
  indicaciones?: string | null;
  conducta?: string | null;
  soapSubjetivo?: string | null;
  soapObjetivo?: string | null;
  soapAnalisis?: string | null;
  soapPlan?: string | null;
  estado: EstadoEvolucion;
  cerradaEn?: string | null;
  adendas?: EvolucionAdenda[];
}

export interface HistoriaResumen {
  historiaId: number | null;
  estadoHistoria: 'ABIERTA' | 'CERRADA';
  paciente: {
    id: number;
    nombres: string;
    apellidos: string;
    fechaNacimiento?: string | null;
    edad: number;
    telefono?: string | null;
    email?: string | null;
    menorEdad: boolean;
  };
  odontologoAsignado?: string | null;
  alergias: string[];
  medicacion: string[];
  diagnosticosActivos: DiagnosticoClinico[];
  totalAntecedentes: number;
  totalEvoluciones: number;
  ultimaEvolucion?: Evolucion | null;
  ultimaCita?: CitaResumenHistoria | null;
  proximaCita?: CitaResumenHistoria | null;
  alertas: string[];
}

export interface CitaResumenHistoria {
  id: number;
  fecha: string;
  hora: string;
  odontologo?: string | null;
  servicio?: string | null;
  estado?: string | null;
}

/** Tipos de evento de la línea temporal unificada (plan §4.16). */
export type TipoTimeline =
  | 'EVOLUCION'
  | 'DIAGNOSTICO'
  | 'EXAMEN'
  | 'CITA'
  | 'ODONTOGRAMA'
  | 'PERIODONCIA'
  | 'PLAN'
  | 'PROCEDIMIENTO'
  | 'RECETA'
  | 'ARCHIVO'
  | 'CONSENTIMIENTO';

export interface TimelineEvento {
  tipo: TipoTimeline;
  fecha: string;
  titulo: string;
  detalle?: string | null;
  referenciaId: number;
}

// Odontograma (Fase C del plan de ampliación, §4.5)
export type Denticion = 'PERMANENTE' | 'TEMPORAL' | 'MIXTA';
export type SuperficieDental = 'OCLUSAL' | 'MESIAL' | 'DISTAL' | 'VESTIBULAR' | 'LINGUAL' | 'INCISAL';
export type EstadoOdontograma =
  | 'EXISTENTE'
  | 'DIAGNOSTICO'
  | 'PLANIFICADO'
  | 'PROCESO'
  | 'COMPLETADO'
  | 'REFERIDO';
export type TipoCambioOdontograma = 'NUEVO' | 'MODIFICADO' | 'ELIMINADO';

export interface DienteOdontograma {
  id: number;
  diente: number;
  superficie: SuperficieDental | null;
  condicion: string;
  estado: EstadoOdontograma;
  notas?: string | null;
}

export interface Odontograma {
  id: number;
  version: number;
  fecha: string;
  denticion: Denticion;
  odontologo?: string | null;
  observaciones?: string | null;
  dientes: DienteOdontograma[];
  createdAt: string;
  updatedAt: string;
}

export interface OdontogramaResumen {
  id: number;
  version: number;
  fecha: string;
  denticion: Denticion;
  observaciones?: string | null;
  registros: number;
}

export interface CambioOdontograma {
  diente: number;
  superficie: SuperficieDental | null;
  condicion: string;
  estadoAnterior: EstadoOdontograma | null;
  estadoNuevo: EstadoOdontograma | null;
  tipo: TipoCambioOdontograma;
}

export interface ComparacionOdontograma {
  desde: OdontogramaResumen;
  hasta: OdontogramaResumen;
  cambios: CambioOdontograma[];
}

export interface EventoOdontograma {
  id: number;
  accion: 'CREAR_VERSION' | 'AGREGAR' | 'MODIFICAR' | 'ELIMINAR';
  diente?: number | null;
  superficie?: SuperficieDental | null;
  condicion?: string | null;
  estadoAnterior?: string | null;
  estadoNuevo?: string | null;
  detalle?: string | null;
  fecha: string;
}

// Planes de tratamiento y presupuestos (Fase D del plan, §4.8-§4.9)
export type EstadoPlanTratamiento =
  | 'BORRADOR'
  | 'PRESENTADO'
  | 'ACEPTADO'
  | 'PARCIALMENTE_ACEPTADO'
  | 'RECHAZADO'
  | 'EN_EJECUCION'
  | 'COMPLETADO'
  | 'CANCELADO';

export type EstadoLineaPlan = 'PENDIENTE' | 'EN_EJECUCION' | 'COMPLETADO' | 'CANCELADO' | 'REFERIDO';

export type PrioridadLinea = 'BAJA' | 'MEDIA' | 'ALTA' | 'URGENTE';

export type EstadoPresupuesto = 'BORRADOR' | 'PRESENTADO' | 'ACEPTADO' | 'RECHAZADO' | 'ANULADO';

export interface LineaPlan {
  id: number;
  numero: number;
  fase: number;
  diente: number | null;
  superficie: string | null;
  diagnostico: string | null;
  procedimiento: string;
  servicioId: number | null;
  servicio: string | null;
  prioridad: PrioridadLinea;
  sesiones: number;
  precioUnitario: number;
  descuentoPct: number;
  subtotal: number;
  estado: EstadoLineaPlan;
  notas: string | null;
}

export interface PlanResumen {
  id: number;
  pacienteId: number;
  titulo: string;
  estado: EstadoPlanTratamiento;
  fecha: string;
  subtotal: number;
  total: number;
  lineas: number;
  presupuestos: number;
  odontologo: string | null;
}

export interface Plan {
  id: number;
  pacienteId: number;
  paciente: string;
  odontologoId: number | null;
  odontologo: string | null;
  titulo: string;
  estado: EstadoPlanTratamiento;
  observaciones: string | null;
  fecha: string;
  subtotal: number;
  total: number;
  comentario: string | null;
  lineas: LineaPlan[];
  presupuestos: PresupuestoResumen[];
}

export interface PresupuestoResumen {
  id: number;
  numero: number;
  estado: EstadoPresupuesto;
  fecha: string;
  vigente: boolean;
  subtotal: number;
  descuento: number;
  total: number;
  firmante: string | null;
  firmaHash: string | null;
  aceptadoEn: string | null;
  observaciones: string | null;
}

export interface LineaPresupuesto {
  id: number;
  planLineaId: number | null;
  numero: number;
  fase: number;
  diente: number | null;
  superficie: string | null;
  diagnostico: string | null;
  procedimiento: string;
  prioridad: PrioridadLinea | null;
  sesiones: number;
  precioUnitario: number;
  descuentoPct: number;
  subtotal: number;
}

export interface Presupuesto extends PresupuestoResumen {
  planId: number;
  comentario: string | null;
  lineas: LineaPresupuesto[];
}

// Examen periodontal (Fase E del plan, §4.11)
export type SitioPeriodontal = 'VESTIBULAR' | 'MESIAL' | 'DISTAL' | 'LINGUAL';

export interface MedicionSitio {
  id: number | null;
  diente: number;
  sitio: SitioPeriodontal;
  sondajeMm: number | null;
  margenMm: number | null;
  insercionMm: number | null;
  sangrado: boolean;
  placa: boolean;
  supuracion: boolean;
}

export interface PiezaPeriodontal {
  id: number | null;
  diente: number;
  movilidad: number | null;
  furcacion: number | null;
  notas: string | null;
}

export interface ExamenPeriodontal {
  id: number;
  historiaId: number;
  version: number;
  fecha: string;
  odontologo: string | null;
  observaciones: string | null;
  sitios: MedicionSitio[];
  dientes: PiezaPeriodontal[];
  createdAt: string;
  updatedAt: string;
}

export interface ExamenPeriodontalResumen {
  id: number;
  version: number;
  fecha: string;
  odontologo: string | null;
  observaciones: string | null;
  sitios: number;
  dientes: number;
}

export interface CambioPeriodontal {
  diente: number;
  sitio: string | null;
  tipo: 'NUEVO' | 'MODIFICADO' | 'ELIMINADO';
  campo: string | null;
  anterior: string | null;
  nuevo: string | null;
}

export interface ComparacionPeriodontal {
  desde: ExamenPeriodontalResumen;
  hasta: ExamenPeriodontalResumen;
  cambios: CambioPeriodontal[];
  agregados: number;
  eliminados: number;
}

// Recetas médicas (Fase E del plan, §4.12)
export type ViaReceta =
  | 'ORAL'
  | 'SUBLINGUAL'
  | 'TOPICA'
  | 'INTRAMUSCULAR'
  | 'INTRAVENOSA'
  | 'RECTAL'
  | 'OFTALMICA'
  | 'OTICA'
  | 'INHALATORIA';

export interface LineaReceta {
  id: number;
  numero: number;
  medicamento: string;
  concentracion: string | null;
  presentacion: string | null;
  dosis: string;
  via: ViaReceta | null;
  frecuencia: string | null;
  duracion: string | null;
  instrucciones: string | null;
}

export interface RecetaResumen {
  id: number;
  fecha: string;
  estado: 'VIGENTE' | 'ANULADA';
  odontologo: string | null;
  diagnostico: string | null;
  motivoAnulacion: string | null;
  lineas: number;
}

export interface Receta {
  id: number;
  pacienteId: number;
  paciente: string;
  odontologoId: number | null;
  odontologo: string | null;
  fecha: string;
  diagnostico: string | null;
  observaciones: string | null;
  estado: 'VIGENTE' | 'ANULADA';
  motivoAnulacion: string | null;
  lineas: LineaReceta[];
  createdAt: string;
  updatedAt: string;
}

// Consentimientos informados (Fase E del plan, §4.13)
export type EstadoConsentimiento = 'BORRADOR' | 'FIRMADO';

export interface PlantillaConsentimiento {
  id: number;
  codigo: string;
  nombre: string;
  categoria: string | null;
  contenido: string;
  version: number;
}

export interface ConsentimientoResumen {
  id: number;
  fecha: string;
  titulo: string;
  categoria: string | null;
  plantillaCodigo: string;
  plantillaVersion: number;
  estado: EstadoConsentimiento;
  odontologo: string | null;
  tutorNombre: string | null;
  tutorId: number | null;
  tutorParentesco: string | null;
  tutorTelefono: string | null;
  firmante: string | null;
  firmaHash: string | null;
  firmadoEn: string | null;
}

export interface Consentimiento {
  id: number;
  pacienteId: number;
  paciente: string;
  plantillaCodigo: string;
  plantillaVersion: number;
  titulo: string;
  categoria: string | null;
  contenido: string;
  fecha: string;
  tutorNombre: string | null;
  tutorId: number | null;
  tutorParentesco: string | null;
  tutorTelefono: string | null;
  odontologoId: number | null;
  odontologo: string | null;
  estado: EstadoConsentimiento;
  firmante: string | null;
  firmaImagen: string | null;
  firmaHash: string | null;
  firmadoEn: string | null;
  observaciones: string | null;
  createdAt: string;
  updatedAt: string;
}

// Archivos clínicos del paciente (Fase E del plan, §4.15)
export type CategoriaArchivo =
  | 'RADIOGRAFIA'
  | 'FOTOGRAFIA'
  | 'PANORAMICA'
  | 'TOMOGRAFIA'
  | 'RESULTADO'
  | 'DOCUMENTO';

export interface ArchivoClinico {
  id: number;
  pacienteId: number;
  categoria: CategoriaArchivo;
  nombre: string;
  mime: string;
  tamano: number;
  sha256: string | null;
  fecha: string | null;
  observaciones: string | null;
  subidoEn: string | null;
}

// ---------------------------------------------------------------------------
// Fase F — Finanzas internas (§3.3 cuenta, §3.4 caja, §3.12 dashboard,
// §3.13 reportes). Sin SRI: esa parte llega en la Fase G.
// ---------------------------------------------------------------------------

export type EstadoCuenta = 'ABIERTA' | 'PARCIAL' | 'PAGADA' | 'CERRADA' | 'ANULADA';
export type TipoMovimientoCuenta =
  | 'CARGO'
  | 'DESCUENTO'
  | 'ABONO'
  | 'ANTICIPO'
  | 'DEVOLUCION';
export type FormaPago = 'EFECTIVO' | 'TRANSFERENCIA' | 'TARJETA' | 'OTRO';
export type TipoMovimientoCaja = 'INGRESO' | 'EGRESO';
export type EstadoCaja = 'ABIERTA' | 'CERRADA';

export interface MovimientoCuenta {
  id: number;
  cuentaId: number;
  tipo: TipoMovimientoCuenta;
  origen: string;
  referenciaId: number | null;
  concepto: string;
  diente: number | null;
  superficie: string | null;
  cantidad: number;
  precioUnitario: number;
  descuentoPct: number;
  total: number;
  formaPago: FormaPago | null;
  fecha: string;
  anulado: boolean;
  anuladoMotivo: string | null;
  observaciones: string | null;
  creadoPor: number | null;
  createdAt: string;
}

export interface CuentaPaciente {
  id: number;
  pacienteId: number;
  paciente: string;
  cedula: string | null;
  estado: EstadoCuenta;
  saldo: number;
  totalCargos: number;
  totalDescuentos: number;
  totalAbonos: number;
  cantidadMovimientos: number;
  movimientos: MovimientoCuenta[];
  createdAt: string;
  updatedAt: string;
}

export interface CuentaResumen {
  id: number;
  pacienteId: number;
  paciente: string;
  cedula: string | null;
  estado: EstadoCuenta;
  saldo: number;
  totalCargos: number;
  totalAbonos: number;
  cantidadMovimientos: number;
  updatedAt: string;
}

export interface NuevoMovimientoCuenta {
  tipo: TipoMovimientoCuenta;
  concepto: string;
  diente?: number | null;
  superficie?: string | null;
  cantidad?: number;
  precioUnitario?: number;
  descuentoPct?: number;
  total?: number;
  formaPago?: FormaPago;
  fecha?: string;
  observaciones?: string;
}

export interface MovimientoCaja {
  id: number;
  cajaId: number;
  tipo: TipoMovimientoCaja;
  concepto: string;
  formaPago: FormaPago;
  monto: number;
  cuentaDetalleId: number | null;
  pacienteId: number | null;
  paciente: string | null;
  fecha: string;
  anulado: boolean;
  anuladoMotivo: string | null;
  creadoPor: number | null;
  createdAt: string;
}

export interface FormaPagoTotal {
  formaPago: string;
  monto: number;
}

export interface TotalesCaja {
  apertura: number;
  ingresos: number;
  egresos: number;
  declarado: number | null;
  esperadoEfectivo: number;
  diferencia: number | null;
  porForma: FormaPagoTotal[];
  movimientos: number;
}

export interface Caja {
  id: number;
  nombre: string;
  estado: EstadoCaja;
  fecha: string;
  montoApertura: number;
  montoDeclarado: number | null;
  diferencia: number | null;
  responsable: string | null;
  abiertoPor: string | null;
  abiertoEn: string | null;
  cerradoPor: string | null;
  cerradoEn: string | null;
  observacionesApertura: string | null;
  observacionesCierre: string | null;
  totales: TotalesCaja;
  movimientos: MovimientoCaja[];
  createdAt: string;
  updatedAt: string;
}

export interface NuevoMovimientoCaja {
  tipo: TipoMovimientoCaja;
  concepto: string;
  formaPago: FormaPago;
  monto: number;
  pacienteId?: number;
}

export interface TotalPorOdontologo {
  odontologo: string;
  facturado: number;
  cargos: number;
}

export interface TotalPorServicio {
  servicio: string;
  facturado: number;
  cargos: number;
}

export interface DashboardFinanciero {
  desde: string;
  hasta: string;
  facturado: number;
  cobrado: number;
  descuentos: number;
  devoluciones: number;
  facturadoHoy: number;
  cobradoHoy: number;
  facturadoMes: number;
  cobradoMes: number;
  cuentasPorCobrar: number;
  cuentasPendientes: number;
  anticipos: number;
  cobros: number;
  ticketPromedio: number;
  porFormaPago: FormaPagoTotal[];
  porOdontologo: TotalPorOdontologo[];
  porServicio: TotalPorServicio[];
}

export interface FilaDiariaVentas {
  fecha: string;
  facturado: number;
  cobrado: number;
  movimientos: number;
}

export interface ReporteVentas {
  desde: string;
  hasta: string;
  facturado: number;
  cobrado: number;
  descuentos: number;
  devoluciones: number;
  cargos: number;
  cobros: number;
  porDia: FilaDiariaVentas[];
  porFormaPago: FormaPagoTotal[];
  porServicio: TotalPorServicio[];
}

export interface FilaProduccion {
  odontologo: string;
  facturado: number;
  cargos: number;
  descuentos: number;
}

export interface ReporteProduccion {
  desde: string;
  hasta: string;
  total: number;
  cargos: number;
  porOdontologo: FilaProduccion[];
}

export interface ReporteSaldos {
  totalPorCobrar: number;
  cantidadCuentas: number;
  cuentas: CuentaResumen[];
}

export interface SesionCajaResumen {
  id: number;
  nombre: string;
  fecha: string;
  estado: string;
  apertura: number;
  declarado: number | null;
  diferencia: number | null;
  movimientos: number;
}

export interface ReporteCaja {
  desde: string;
  hasta: string;
  cantidadSesiones: number;
  aperturas: number;
  ingresos: number;
  egresos: number;
  diferenciaTotal: number;
  sesiones: SesionCajaResumen[];
}

export type EstadoTributario =
  | 'PENDIENTE'
  | 'AUTORIZADO'
  | 'DEVUELTO'
  | 'NO_AUTORIZADO'
  | 'ANULADO';

export type EstadoGeneracion =
  | 'BORRADOR'
  | 'GENERADO'
  | 'FIRMADO'
  | 'ENVIADO'
  | 'ERROR';

/** Fila de la bandeja de comprobantes (plan §3.7). */
export interface ComprobanteResumen {
  id: number;
  tipo: string;
  numeroComprobante: string;
  claveAcceso: string;
  pacienteId: number | null;
  paciente: string | null;
  identificacion: string | null;
  total: number;
  fechaEmision: string;
  estadoGeneracion: EstadoGeneracion;
  estadoTributario: EstadoTributario;
  simulado: boolean;
  errorMensaje: string | null;
  cuentaId: number;
  motivo: string | null;
  comprobanteReferenciaId: number | null;
  createdAt: string;
}

/**
 * Línea del detalle del comprobante leída del XML (plan §3.9): es lo que
 * la nota de crédito parcial usa para elegir qué devuelve. `total` es la
 * base sin IVA; `total + impuesto` es lo que se cobró al paciente.
 */
export interface ComprobanteLinea {
  linea: number;
  codigo: string | null;
  descripcion: string;
  cantidad: number;
  precioUnitario: number;
  descuento: number;
  total: number;
  impuesto: number | null;
}

/** Comprobante completo: estados, respuesta del SRI y autorización. */
export interface ComprobanteElectronico {
  id: number;
  tipo: string;
  numeroComprobante: string;
  claveAcceso: string;
  ambiente: string;
  modoFirma: string;
  simulado: boolean;
  estadoGeneracion: EstadoGeneracion;
  estadoTributario: EstadoTributario;
  sriEstado: string | null;
  sriRespuesta: string | null;
  errorMensaje: string | null;
  autorizacionNumero: string | null;
  autorizacionFecha: string | null;
  total: number;
  fechaEmision: string;
  cuentaId: number;
  pacienteId: number;
  comprobanteReferenciaId: number | null;
  motivo: string | null;
  detalles: ComprobanteLinea[];
  createdAt: string;
}
