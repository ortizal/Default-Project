import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { UiPageHeaderComponent } from '../ui';

type CategoriaReporte = 'Operación' | 'Finanzas' | 'Clínica';

interface ReporteCatalogo {
  nombre: string;
  categoria: CategoriaReporte;
  descripcion: string;
  ruta: string;
  permiso: string;
  fuente: string;
  filtros: string;
  formatos: string;
}

@Component({
  selector: 'app-centro-reportes',
  templateUrl: './centro-reportes.html',
  imports: [FormsModule, RouterLink, UiPageHeaderComponent],
})
export class CentroReportesComponent {
  private readonly auth = inject(AuthService);

  readonly categorias = ['Todas', 'Operación', 'Finanzas', 'Clínica'] as const;
  readonly reportes: ReporteCatalogo[] = [
    {
      nombre: 'Estadísticas de operación',
      categoria: 'Operación',
      descripcion: 'Citas, confirmaciones, cancelaciones, inasistencias, servicios y actividad por odontólogo.',
      ruta: '/reportes/operacion',
      permiso: 'REPORTES',
      fuente: 'GET /reportes/estadisticas',
      filtros: 'Rango de fechas; límite de servicios.',
      formatos: 'Vista en pantalla',
    },
    {
      nombre: 'Reportes financieros',
      categoria: 'Finanzas',
      descripcion: 'Ventas diarias, producción, cuentas con saldo y conciliación de sesiones de caja.',
      ruta: '/facturacion/reportes',
      permiso: 'REPORTES_FINANCIEROS',
      fuente: 'GET /facturacion/reportes/{ventas,produccion,saldos,caja}',
      filtros: 'Rango de fechas; saldos muestran el estado actual.',
      formatos: 'Vista en pantalla',
    },
    {
      nombre: 'Reportes clínicos',
      categoria: 'Clínica',
      descripcion: 'Actividad clínica, diagnósticos frecuentes, tratamientos pendientes y continuidad de atención.',
      ruta: '/reportes-clinicos',
      permiso: 'REPORTES_CLINICOS',
      fuente: 'GET /historia-clinica/reportes',
      filtros: 'Rango de fechas y límite de filas (máximo 50).',
      formatos: 'Vista en pantalla',
    },
  ];

  busqueda = '';
  categoria: (typeof this.categorias)[number] = 'Todas';

  get disponibles(): ReporteCatalogo[] {
    const termino = this.busqueda.trim().toLocaleLowerCase('es');
    return this.reportes.filter((reporte) => {
      const autorizado = this.auth.puede(reporte.permiso);
      const categoriaValida = this.categoria === 'Todas' || reporte.categoria === this.categoria;
      const texto = `${reporte.nombre} ${reporte.descripcion} ${reporte.categoria}`.toLocaleLowerCase('es');
      return autorizado && categoriaValida && (!termino || texto.includes(termino));
    });
  }

  limpiar(): void {
    this.busqueda = '';
    this.categoria = 'Todas';
  }
}