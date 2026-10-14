export interface MenuItem {
  path: string;
  label: string;
  /** Permiso requerido para verlo en el menú (undefined = siempre visible). */
  permiso?: string;
  /** El enlace aparece si el usuario tiene al menos uno de estos permisos. */
  permisosAlternativos?: string[];
}

export interface MenuSection {
  section: string;
  items: MenuItem[];
}

export const MENU: MenuSection[] = [
  {
    section: 'Operación',
    items: [
      {
        path: '/dashboard',
        label: 'Centro de inteligencia',
        permisosAlternativos: ['REPORTES', 'FACTURACION_READ', 'REPORTES_CLINICOS'],
      },
      { path: '/dashboard/operacion', label: 'Resumen operativo', permiso: 'REPORTES' },
      { path: '/pacientes', label: 'Pacientes', permiso: 'PACIENTES_READ' },
      { path: '/odontologos', label: 'Odontólogos', permiso: 'ODONTOLOGOS_READ' },
      { path: '/servicios', label: 'Servicios', permiso: 'SERVICIOS_READ' },
      { path: '/horarios', label: 'Horarios', permiso: 'AGENDA_READ' },
      { path: '/agenda', label: 'Agenda', permiso: 'AGENDA_READ' },
      { path: '/citas', label: 'Citas', permiso: 'CITAS_READ' },
    ],
  },
  {
    section: 'Comunicación',
    items: [
      { path: '/whatsapp/inbox', label: 'WhatsApp Inbox', permiso: 'WHATSAPP_READ' },
      { path: '/whatsapp/campana', label: 'Publicidad WhatsApp', permiso: 'WHATSAPP_WRITE' },
      { path: '/redes-sociales', label: 'Redes sociales', permiso: 'REDES_SOCIALES' },
      { path: '/whatsapp/sesiones', label: 'Sesiones WhatsApp', permiso: 'WHATSAPP_WRITE' },
      { path: '/plantillas', label: 'Plantillas', permiso: 'AUTOMATIZACIONES' },
      { path: '/notificaciones', label: 'Notificaciones', permiso: 'AUTOMATIZACIONES' },
    ],
  },
  {
    section: 'Automatización',
    items: [
      { path: '/automatizaciones', label: 'Automatizaciones', permiso: 'AUTOMATIZACIONES' },
      { path: '/agente', label: 'Agente IA', permiso: 'AGENTE_IA' },
    ],
  },
  {
    section: 'Calendario',
    items: [{ path: '/google', label: 'Google Calendar', permiso: 'INTEGRACIONES' }],
  },
  {
    section: 'Finanzas',
    items: [
      { path: '/facturacion', label: 'Dashboard financiero', permiso: 'FACTURACION_READ' },
      { path: '/facturacion/cuentas', label: 'Cuentas', permiso: 'FACTURACION_READ' },
      { path: '/facturacion/comprobantes', label: 'Comprobantes', permiso: 'FACTURACION_READ' },
      { path: '/facturacion/caja', label: 'Caja', permiso: 'CAJA_READ' },
      { path: '/facturacion/reportes', label: 'Reportes financieros', permiso: 'REPORTES_FINANCIEROS' },
    ],
  },
  {
    section: 'Gestión',
    items: [
      {
        path: '/reportes',
        label: 'Centro de reportes',
        permisosAlternativos: ['REPORTES', 'REPORTES_FINANCIEROS', 'REPORTES_CLINICOS'],
      },
      { path: '/reportes-clinicos', label: 'Reportes clínicos', permiso: 'REPORTES_CLINICOS' },
      { path: '/auditoria', label: 'Auditoría', permiso: 'AUDITORIA_READ' },
      { path: '/usuarios', label: 'Usuarios', permiso: 'USUARIOS_READ' },
      { path: '/configuracion/consultorio', label: 'Datos del consultorio', permiso: 'CONFIGURACION' },
      { path: '/configuracion/integraciones', label: 'Configuración de integraciones', permiso: 'CONFIGURACION' },
    ],
  },
];
