export interface MenuItem {
  path: string;
  label: string;
}

export interface MenuSection {
  section: string;
  items: MenuItem[];
}

export const MENU: MenuSection[] = [
  {
    section: 'Operación',
    items: [
      { path: '/dashboard', label: 'Inicio' },
      { path: '/pacientes', label: 'Pacientes' },
      { path: '/odontologos', label: 'Odontólogos' },
      { path: '/servicios', label: 'Servicios' },
      { path: '/horarios', label: 'Horarios' },
      { path: '/agenda', label: 'Agenda' },
      { path: '/citas', label: 'Citas' },
    ],
  },
  {
    section: 'Comunicación',
    items: [
      { path: '/whatsapp/inbox', label: 'WhatsApp Inbox' },
      { path: '/whatsapp/sesiones', label: 'Sesiones WhatsApp' },
      { path: '/plantillas', label: 'Plantillas' },
      { path: '/notificaciones', label: 'Notificaciones' },
    ],
  },
  {
    section: 'Automatización',
    items: [
      { path: '/automatizaciones', label: 'Automatizaciones' },
      { path: '/agente', label: 'Agente IA' },
    ],
  },
  {
    section: 'Calendario',
    items: [{ path: '/google', label: 'Google Calendar' }],
  },
  {
    section: 'Gestión',
    items: [
      { path: '/reportes', label: 'Reportes' },
      { path: '/auditoria', label: 'Auditoría' },
      { path: '/usuarios', label: 'Usuarios' },
    ],
  },
];
