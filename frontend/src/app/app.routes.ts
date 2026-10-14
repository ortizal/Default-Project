import { Routes } from '@angular/router';
import { authGuard } from './core/guards';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./auth/login').then((m) => m.LoginComponent) },
  {
    // Página pública con enlace temporal (§13): sin sesión ni authGuard.
    path: 'agenda-publica',
    loadComponent: () => import('./agenda-publica/agenda-publica').then((m) => m.AgendaPublicaComponent),
  },
  {
    path: '',
    loadComponent: () => import('./layout/layout').then((m) => m.LayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        pathMatch: 'full',
        loadComponent: () => import('./reportes/centro-inteligencia').then((m) => m.CentroInteligenciaComponent),
      },
      {
        path: 'dashboard/operacion',
        loadComponent: () => import('./reportes/dashboard').then((m) => m.DashboardComponent),
      },
      {
        path: 'reportes/operacion',
        loadComponent: () => import('./reportes/reportes').then((m) => m.ReportesComponent),
      },
      {
        path: 'reportes',
        pathMatch: 'full',
        loadComponent: () => import('./reportes/centro-reportes').then((m) => m.CentroReportesComponent),
      },
      {
        path: 'reportes-clinicos',
        loadComponent: () => import('./reportes/reportes-clinicos').then((m) => m.ReportesClinicosComponent),
      },
      { path: 'pacientes', loadComponent: () => import('./pacientes/pacientes').then((m) => m.PacientesComponent) },
      {
        path: 'pacientes/:id/historia',
        loadComponent: () => import('./pacientes/historia-clinica').then((m) => m.HistoriaClinicaComponent),
      },
      { path: 'odontologos', loadComponent: () => import('./odontologos/odontologos').then((m) => m.OdontologosComponent) },
      { path: 'servicios', loadComponent: () => import('./servicios/servicios').then((m) => m.ServiciosComponent) },
      { path: 'horarios', loadComponent: () => import('./horarios/horarios').then((m) => m.HorariosComponent) },
      { path: 'agenda', loadComponent: () => import('./agenda/agenda').then((m) => m.AgendaComponent) },
      { path: 'citas', loadComponent: () => import('./citas/citas').then((m) => m.CitasComponent) },
      { path: 'whatsapp/sesiones', loadComponent: () => import('./whatsapp/sesiones').then((m) => m.SesionesComponent) },
      { path: 'whatsapp/inbox', loadComponent: () => import('./whatsapp/inbox').then((m) => m.InboxComponent) },
      { path: 'whatsapp/campana', loadComponent: () => import('./whatsapp/campana').then((m) => m.CampanaComponent) },
      { path: 'redes-sociales', loadComponent: () => import('./redes-sociales/redes-sociales').then((m) => m.RedesSocialesComponent) },
      { path: 'plantillas', loadComponent: () => import('./automatizacion/plantillas').then((m) => m.PlantillasComponent) },
      { path: 'automatizaciones', loadComponent: () => import('./automatizacion/automatizaciones').then((m) => m.AutomatizacionesComponent) },
      { path: 'notificaciones', loadComponent: () => import('./automatizacion/notificaciones').then((m) => m.NotificacionesComponent) },
      { path: 'agente', loadComponent: () => import('./agente/agente').then((m) => m.AgenteComponent) },
      { path: 'google', loadComponent: () => import('./google/calendario').then((m) => m.CalendarioComponent) },
      { path: 'auditoria', loadComponent: () => import('./auditoria/auditoria').then((m) => m.AuditoriaComponent) },
      { path: 'usuarios', loadComponent: () => import('./usuarios/usuarios').then((m) => m.UsuariosComponent) },
      { path: 'configuracion/consultorio', loadComponent: () => import('./configuracion/consultorio').then((m) => m.ConsultorioComponent) },
      { path: 'configuracion/integraciones', loadComponent: () => import('./configuracion/integraciones').then((m) => m.IntegracionesConfiguracionComponent) },
      { path: 'facturacion', loadComponent: () => import('./facturacion/dashboard').then((m) => m.FacturacionDashboardComponent) },
      { path: 'facturacion/cuentas', loadComponent: () => import('./facturacion/cuentas').then((m) => m.FacturacionCuentasComponent) },
      { path: 'facturacion/comprobantes', loadComponent: () => import('./facturacion/comprobantes').then((m) => m.FacturacionComprobantesComponent) },
      { path: 'facturacion/caja', loadComponent: () => import('./facturacion/caja').then((m) => m.FacturacionCajaComponent) },
      {
        path: 'facturacion/reportes',
        loadComponent: () => import('./facturacion/reportes-financieros').then((m) => m.ReportesFinancierosComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];