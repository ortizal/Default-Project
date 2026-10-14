import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { UiPageHeaderComponent } from '../ui';

interface Consultorio {
  nombre: string;
  razonSocial: string;
  ruc: string;
  correoElectronico: string;
  direccion: string;
  telefono: string;
  enlaceUbicacion: string;
  horarioAtencion: string;
  firmaDigitalNombre?: string;
  firmaDigitalCargada?: boolean;
}

interface ConfiguracionFirma {
  ambiente: 'PRUEBAS' | 'PRODUCCION';
  tipoEmision: string;
  contribuyenteEspecial: boolean;
  agenteRetencion: boolean;
  obligadoContabilidad: boolean;
  establecimiento: string;
  puntoEmision: string;
  secuencialActual: number;
  secuencialSiguiente: string;
  sriUrlPruebas: string;
  sriUrlProduccion: string;
  sriUrlActiva: string;
  firmaModo: string;
  firmaMotivo: string | null;
  firmaArchivo: string | null;
  firmaPasswordConfigurada: boolean;
  firmaTitular: string | null;
  firmaValidoDesde: string | null;
  firmaValidoHasta: string | null;
}

@Component({
  selector: 'app-consultorio',
  templateUrl: './consultorio.html',
  styleUrl: './consultorio.css',
  imports: [CommonModule, FormsModule, UiPageHeaderComponent],
})
export class ConsultorioComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly auth = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  form: Consultorio = {
    nombre: '',
    razonSocial: '',
    ruc: '',
    correoElectronico: '',
    direccion: '',
    telefono: '',
    enlaceUbicacion: '',
    horarioAtencion: '',
  };
  cargando = true;
  guardando = false;
  error = '';
  exito = '';
  firmaSeleccionada: File | null = null;
  firmaSubiendo = false;
  firmaPassword = '';
  firmaPasswordGuardando = false;
  cargandoFacturacion = false;
  guardandoFacturacion = false;
  puedeConfigurarFacturacion = false;
  configFacturacion: ConfiguracionFirma | null = null;

  ngOnInit(): void {
    this.puedeConfigurarFacturacion = this.auth.puede('CONFIGURACION_FACTURACION');
    this.api.get<Consultorio>('/configuracion/consultorio').subscribe({
      next: (consultorio) => {
        this.form = { ...this.form, ...consultorio };
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = e?.error?.message || 'No se pudo cargar la configuración.';
        this.cargando = false;
        this.cdr.markForCheck();
      },
    });
    if (this.puedeConfigurarFacturacion) this.cargarConfiguracionFacturacion();
  }

  cargarConfiguracionFacturacion(): void {
    this.cargandoFacturacion = true;
    this.api.get<ConfiguracionFirma>('/facturacion/configuracion').subscribe({
      next: (configuracion) => {
        this.configFacturacion = configuracion;
        this.cargandoFacturacion = false;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = e?.error?.message || 'No se pudo cargar la configuración tributaria.';
        this.cargandoFacturacion = false;
        this.cdr.markForCheck();
      },
    });
  }

  guardarConfiguracionFacturacion(): void {
    const actual = this.configFacturacion;
    if (!actual || this.guardandoFacturacion) return;
    this.guardandoFacturacion = true;
    this.error = '';
    this.exito = '';
    this.api.put<ConfiguracionFirma>('/facturacion/configuracion', {
      ambiente: actual.ambiente,
      tipoEmision: actual.tipoEmision,
      contribuyenteEspecial: actual.contribuyenteEspecial,
      agenteRetencion: actual.agenteRetencion,
      obligadoContabilidad: actual.obligadoContabilidad,
      establecimiento: actual.establecimiento,
      puntoEmision: actual.puntoEmision,
      secuencialActual: actual.secuencialActual,
      sriUrlPruebas: actual.sriUrlPruebas,
      sriUrlProduccion: actual.sriUrlProduccion,
      sriUsuario: null,
      sriClave: null,
      limpiarSriClave: false,
    }).subscribe({
      next: (configuracion) => {
        this.configFacturacion = configuracion;
        this.guardandoFacturacion = false;
        this.exito = 'Configuración tributaria guardada.';
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = e?.error?.message || 'No se pudo guardar la configuración tributaria.';
        this.guardandoFacturacion = false;
        this.cdr.markForCheck();
      },
    });
  }

  guardarPasswordFirma(): void {
    if (!this.form.firmaDigitalCargada || this.firmaPasswordGuardando) return;
    if (!this.firmaPassword) {
      this.error = 'Escribe la contraseña del archivo .p12.';
      return;
    }
    this.firmaPasswordGuardando = true;
    this.error = '';
    this.exito = '';
    this.api.put<ConfiguracionFirma>('/facturacion/configuracion/firma', {
      password: this.firmaPassword,
    }).subscribe({
      next: (configuracion) => {
        this.configFacturacion = configuracion;
        this.firmaPassword = '';
        this.firmaPasswordGuardando = false;
        this.exito = 'Certificado validado y contraseña protegida.';
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.firmaPassword = '';
        this.error = e?.error?.message || 'No se pudo validar el certificado .p12.';
        this.firmaPasswordGuardando = false;
        this.cdr.markForCheck();
      },
    });
  }

  guardar(): void {
    if (this.guardando || !this.form.nombre.trim()) {
      this.error = 'El nombre del consultorio es obligatorio.';
      return;
    }
    this.guardando = true;
    this.error = '';
    this.exito = '';
    const request = {
      nombre: this.form.nombre,
      razonSocial: this.form.razonSocial,
      ruc: this.form.ruc,
      correoElectronico: this.form.correoElectronico,
      direccion: this.form.direccion,
      telefono: this.form.telefono,
      enlaceUbicacion: this.form.enlaceUbicacion,
      horarioAtencion: this.form.horarioAtencion,
    };
    this.api.put<Consultorio>('/configuracion/consultorio', request).subscribe({
      next: (consultorio) => {
        this.form = { ...this.form, ...consultorio };
        this.guardando = false;
        this.exito = 'Datos del consultorio guardados.';
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = e?.error?.message || 'No se pudo guardar la configuración.';
        this.guardando = false;
        this.cdr.markForCheck();
      },
    });
  }

  seleccionarFirma(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    this.error = '';
    this.exito = '';
    if (archivo && archivo.size > 5 * 1024 * 1024) {
      this.error = 'El archivo .p12 no puede superar 5 MB.';
      input.value = '';
      this.firmaSeleccionada = null;
      this.cdr.markForCheck();
      return;
    }
    if (archivo && !archivo.name.toLowerCase().endsWith('.p12')) {
      this.error = 'Selecciona un archivo con extensión .p12.';
      input.value = '';
      this.firmaSeleccionada = null;
      this.cdr.markForCheck();
      return;
    }
    this.firmaSeleccionada = archivo;
  }

  cargarFirma(): void {
    if (!this.firmaSeleccionada || this.firmaSubiendo) return;
    const datos = new FormData();
    datos.append('archivo', this.firmaSeleccionada);
    this.firmaSubiendo = true;
    this.error = '';
    this.exito = '';
    this.api.put<Consultorio>('/configuracion/consultorio/firma-digital', datos).subscribe({
      next: (consultorio) => {
        this.form = { ...this.form, ...consultorio };
        this.firmaSeleccionada = null;
        this.firmaSubiendo = false;
        this.exito = 'Firma digital cargada.';
        if (this.puedeConfigurarFacturacion) this.cargarConfiguracionFacturacion();
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = e?.error?.message || 'No se pudo cargar la firma digital.';
        this.firmaSubiendo = false;
        this.cdr.markForCheck();
      },
    });
  }

  eliminarFirma(): void {
    if (!this.form.firmaDigitalCargada || this.firmaSubiendo) return;
    this.firmaSubiendo = true;
    this.error = '';
    this.exito = '';
    this.api.del<Consultorio>('/configuracion/consultorio/firma-digital').subscribe({
      next: (consultorio) => {
        this.form = { ...this.form, ...consultorio };
        this.firmaSubiendo = false;
        this.exito = 'Firma digital eliminada.';
        if (this.puedeConfigurarFacturacion) this.cargarConfiguracionFacturacion();
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = e?.error?.message || 'No se pudo eliminar la firma digital.';
        this.firmaSubiendo = false;
        this.cdr.markForCheck();
      },
    });
  }
}