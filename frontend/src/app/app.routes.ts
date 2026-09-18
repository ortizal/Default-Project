import { Routes } from '@angular/router';
import { authGuard } from './core/guards';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/login').then((m) => m.LoginComponent) },
  {
    path: '',
    loadComponent: () => import('./layout/layout').then((m) => m.LayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./reportes/dashboard').then((m) => m.DashboardComponent) },
      { path: 'reportes', loadComponent: () => import('./reportes/reportes').then((m) => m.ReportesComponent) },
      { path: 'pacientes', loadComponent: () => import('./pacientes/pacientes').then((m) => m.PacientesComponent) },
      { path: 'odontologos', loadComponent: () => import('./odontologos/odontologos').then((m) => m.OdontologosComponent) },
      { path: 'servicios', loadComponent: () => import('./servicios/servicios').then((m) => m.ServiciosComponent) },
      { path: 'horarios', loadComponent: () => import('./horarios/horarios').then((m) => m.HorariosComponent) },
      { path: 'agenda', loadComponent: () => import('./agenda/agenda').then((m) => m.AgendaComponent) },
      { path: 'citas', loadComponent: () => import('./citas/citas').then((m) => m.CitasComponent) },
      { path: 'whatsapp/sesiones', loadComponent: () => import('./whatsapp/sesiones').then((m) => m.SesionesComponent) },
      { path: 'whatsapp/inbox', loadComponent: () => import('./whatsapp/inbox').then((m) => m.InboxComponent) },
      { path: 'plantillas', loadComponent: () => import('./automatizacion/plantillas').then((m) => m.PlantillasComponent) },
      { path: 'automatizaciones', loadComponent: () => import('./automatizacion/automatizaciones').then((m) => m.AutomatizacionesComponent) },
      { path: 'notificaciones', loadComponent: () => import('./automatizacion/notificaciones').then((m) => m.NotificacionesComponent) },
      { path: 'agente', loadComponent: () => import('./agente/agente').then((m) => m.AgenteComponent) },
      { path: 'google', loadComponent: () => import('./google/calendario').then((m) => m.CalendarioComponent) },
      { path: 'auditoria', loadComponent: () => import('./auditoria/auditoria').then((m) => m.AuditoriaComponent) },
      { path: 'usuarios', loadComponent: () => import('./usuarios/usuarios').then((m) => m.UsuariosComponent) },
    ],
  },
  { path: '**', redirectTo: '' },
];