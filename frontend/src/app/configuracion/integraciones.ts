import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { UiConfirmService, UiPageHeaderComponent } from '../ui';

interface IntegracionesConfig {
  openWa: {
    url: string;
    webhookUrl: string;
    configurado: boolean;
    apiKeyConfigurada: boolean;
    webhookSecretConfigurado: boolean;
    usaValoresEntorno: boolean;
  };
  correo: {
    host: string;
    puerto: number;
    usuario: string;
    remitente: string;
    configurado: boolean;
    passwordConfigurada: boolean;
    usaValoresEntorno: boolean;
  };
  meta: {
    clientId: string;
    redirectUri: string;
    configurado: boolean;
    clientSecretConfigurado: boolean;
    usaValoresEntorno: boolean;
  };
  tikTok: {
    clientKey: string;
    redirectUri: string;
    configurado: boolean;
    clientSecretConfigurado: boolean;
    usaValoresEntorno: boolean;
  };
}

@Component({
  selector: 'app-integraciones-configuracion',
  templateUrl: './integraciones.html',
  styleUrl: './integraciones.css',
  imports: [CommonModule, FormsModule, UiPageHeaderComponent],
})
export class IntegracionesConfiguracionComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly confirmacion = inject(UiConfirmService);

  config?: IntegracionesConfig;
  cargando = true;
  guardando: 'openwa' | 'correo' | 'meta' | 'tiktok' | '' = '';
  error = '';
  exito = '';

  openwa = { url: '', apiKey: '', webhookUrl: '', webhookSecret: '' };
  correo = { host: '', puerto: 587, usuario: '', password: '', remitente: '' };
  meta = { clientId: '', clientSecret: '', redirectUri: '' };
  tikTok = { clientKey: '', clientSecret: '', redirectUri: '' };

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.api.get<IntegracionesConfig>('/configuracion/integraciones').subscribe({
      next: (config) => {
        this.aplicar(config);
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.error = this.mensaje(error, 'No se pudo cargar la configuración.');
        this.cargando = false;
        this.cdr.markForCheck();
      },
    });
  }

  guardarOpenWa(): void {
    if (!this.openwa.url.trim()) {
      this.error = 'La URL de OpenWA es obligatoria.';
      return;
    }
    this.guardar('openwa', this.api.put<IntegracionesConfig>(
      '/configuracion/integraciones/openwa', this.openwa,
    ));
  }

  guardarCorreo(): void {
    if (!Number.isInteger(Number(this.correo.puerto)) || this.correo.puerto < 1 || this.correo.puerto > 65535) {
      this.error = 'El puerto SMTP debe estar entre 1 y 65535.';
      return;
    }
    this.guardar('correo', this.api.put<IntegracionesConfig>(
      '/configuracion/integraciones/correo', this.correo,
    ));
  }

  guardarMeta(): void {
    this.guardar('meta', this.api.put<IntegracionesConfig>(
      '/configuracion/integraciones/meta', this.meta,
    ));
  }

  guardarTikTok(): void {
    this.guardar('tiktok', this.api.put<IntegracionesConfig>(
      '/configuracion/integraciones/tiktok', this.tikTok,
    ));
  }

  async usarValoresOpenWa(): Promise<void> {
    if (!(await this.confirmarRestablecimiento('OpenWA'))) return;
    this.guardar('openwa', this.api.del<IntegracionesConfig>('/configuracion/integraciones/openwa'));
  }

  async usarValoresCorreo(): Promise<void> {
    if (!(await this.confirmarRestablecimiento('correo'))) return;
    this.guardar('correo', this.api.del<IntegracionesConfig>('/configuracion/integraciones/correo'));
  }

  async usarValoresMeta(): Promise<void> {
    if (!(await this.confirmarRestablecimiento('Meta'))) return;
    this.guardar('meta', this.api.del<IntegracionesConfig>('/configuracion/integraciones/meta'));
  }

  async usarValoresTikTok(): Promise<void> {
    if (!(await this.confirmarRestablecimiento('TikTok'))) return;
    this.guardar('tiktok', this.api.del<IntegracionesConfig>('/configuracion/integraciones/tiktok'));
  }

  private guardar(tipo: 'openwa' | 'correo' | 'meta' | 'tiktok', solicitud: ReturnType<Api['get']>): void {
    if (this.guardando) return;
    this.guardando = tipo;
    this.error = '';
    this.exito = '';
    solicitud.subscribe({
      next: (config) => {
        this.aplicar(config as IntegracionesConfig);
        this.guardando = '';
        this.exito = `Configuración de ${tipo === 'openwa' ? 'OpenWA' : tipo === 'correo' ? 'correo' : tipo === 'meta' ? 'Meta' : 'TikTok'} guardada.`;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.error = this.mensaje(error, 'No se pudo guardar la configuración.');
        this.guardando = '';
        this.cdr.markForCheck();
      },
    });
  }

  private aplicar(config: IntegracionesConfig): void {
    this.config = config;
    this.openwa = { ...this.openwa, url: config.openWa.url || '', webhookUrl: config.openWa.webhookUrl || '', apiKey: '', webhookSecret: '' };
    this.correo = {
      ...this.correo,
      host: config.correo.host || '',
      puerto: config.correo.puerto || 587,
      usuario: config.correo.usuario || '',
      remitente: config.correo.remitente || '',
      password: '',
    };
    this.meta = { clientId: config.meta.clientId || '', redirectUri: config.meta.redirectUri || '', clientSecret: '' };
    this.tikTok = {
      clientKey: config.tikTok.clientKey || '',
      redirectUri: config.tikTok.redirectUri || '',
      clientSecret: '',
    };
  }

  private async confirmarRestablecimiento(nombre: string): Promise<boolean> {
    return this.confirmacion.abrir({
      titulo: 'Usar valores del entorno',
      mensaje: `Se eliminarán los valores guardados para ${nombre} y se volverá a usar .env.`,
      confirmarTexto: 'Restablecer',
    });
  }

  private mensaje(error: unknown, fallback: string): string {
    const apiError = error as { error?: { message?: string } };
    return apiError?.error?.message || fallback;
  }
}