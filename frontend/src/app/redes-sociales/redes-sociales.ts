import { ChangeDetectorRef, Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Api } from '../core/api';
import { UiPageHeaderComponent, UiConfirmService } from '../ui';

interface SocialAccount {
  id: number;
  platform: 'FACEBOOK' | 'INSTAGRAM' | 'TIKTOK';
  accountName: string;
}

interface SocialProviderStatus {
  configured: boolean;
  accounts: SocialAccount[];
}

interface SocialStatus {
  meta: SocialProviderStatus;
  tiktok: SocialProviderStatus;
}

interface SocialConnectResponse {
  authUrl: string;
}

interface SocialPublishResponse {
  publicacionId: string;
}

const MAX_TEXTO = 2200;
const MAX_IMAGEN_BYTES = 5 * 1024 * 1024;
const TIPOS_IMAGEN = ['image/jpeg', 'image/png', 'image/webp'];

@Component({
  selector: 'app-redes-sociales',
  templateUrl: './redes-sociales.html',
  imports: [CommonModule, FormsModule, UiPageHeaderComponent],
})
export class RedesSocialesComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly confirmacion = inject(UiConfirmService);

  @ViewChild('archivoImagen') archivoImagen?: ElementRef<HTMLInputElement>;

  status?: SocialStatus;
  error = '';
  aviso = '';
  cargando = false;
  conectando = '';

  cuentaPublicacion: number | null = null;
  textoPublicacion = '';
  imagenPublicacion: File | null = null;
  publicando = false;
  exitoPublicacion = '';
  errorPublicacion = '';

  ngOnInit(): void {
    const resultado = this.route.snapshot.queryParamMap.get('social');
    if (resultado === 'connected') this.aviso = 'Cuenta conectada. Actualizando perfiles...';
    if (resultado === 'error') this.error = 'No se pudo completar la conexión. Revisa la configuración e inténtalo de nuevo.';
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.api.get<SocialStatus>('/social/status').subscribe({
      next: (status) => {
        this.status = status;
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.error = 'No se pudo consultar el estado de las redes sociales.';
        this.cargando = false;
        this.cdr.markForCheck();
      },
    });
  }

  conectar(provider: 'meta' | 'tiktok'): void {
    this.error = '';
    this.conectando = provider;
    this.api.get<SocialConnectResponse>(`/social/${provider}/connect`).subscribe({
      next: ({ authUrl }) => {
        window.location.assign(authUrl);
      },
      error: (e) => {
        this.error = e?.error?.message ?? 'No se pudo iniciar la conexión.';
        this.conectando = '';
        this.cdr.markForCheck();
      },
    });
  }

  async desconectar(provider: 'meta' | 'tiktok', nombre: string): Promise<void> {
    if (!(await this.confirmacion.abrir({
      titulo: `Desconectar ${nombre}`,
      mensaje: `¿Desconectar las cuentas de ${nombre} de esta clínica?`,
      confirmarTexto: 'Desconectar',
    }))) return;

    this.api.del<void>(`/social/${provider}/disconnect`).subscribe({
      next: () => this.cargar(),
      error: () => {
        this.error = `No se pudo desconectar ${nombre}.`;
        this.cdr.markForCheck();
      },
    });
  }

  get cuentasFacebook(): SocialAccount[] {
    return this.status?.meta.accounts.filter((cuenta) => cuenta.platform === 'FACEBOOK') ?? [];
  }

  get cuentasRestringidas(): SocialAccount[] {
    const meta = this.status?.meta.accounts.filter((cuenta) => cuenta.platform === 'INSTAGRAM') ?? [];
    return [...meta, ...(this.status?.tiktok.accounts ?? [])];
  }

  get longitudTexto(): number {
    return this.textoPublicacion.length;
  }

  get textoValido(): boolean {
    const texto = this.textoPublicacion.trim();
    return texto.length > 0 && texto.length <= MAX_TEXTO;
  }

  get puedePublicar(): boolean {
    return this.cuentaPublicacion !== null && this.textoValido && !this.publicando;
  }

  get tamanoImagen(): string {
    if (!this.imagenPublicacion) return '';
    const mb = this.imagenPublicacion.size / (1024 * 1024);
    return mb >= 1 ? `${mb.toFixed(1)} MB` : `${Math.ceil(this.imagenPublicacion.size / 1024)} KB`;
  }

  seleccionarImagen(event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0] ?? null;
    this.errorPublicacion = '';
    this.imagenPublicacion = null;
    if (!archivo) return;
    if (!TIPOS_IMAGEN.includes(archivo.type)) {
      this.errorPublicacion = 'La imagen debe estar en formato JPEG, PNG o WebP.';
      input.value = '';
      this.cdr.markForCheck();
      return;
    }
    if (archivo.size > MAX_IMAGEN_BYTES) {
      this.errorPublicacion = 'La imagen no puede superar 5 MB.';
      input.value = '';
      this.cdr.markForCheck();
      return;
    }
    this.imagenPublicacion = archivo;
    this.cdr.markForCheck();
  }

  quitarImagen(): void {
    this.imagenPublicacion = null;
    this.errorPublicacion = '';
    if (this.archivoImagen) this.archivoImagen.nativeElement.value = '';
    this.cdr.markForCheck();
  }

  publicar(): void {
    if (!this.puedePublicar || this.cuentaPublicacion === null) return;

    const datos = new FormData();
    datos.append('cuentaId', String(this.cuentaPublicacion));
    datos.append('texto', this.textoPublicacion.trim());
    if (this.imagenPublicacion) datos.append('imagen', this.imagenPublicacion);

    this.errorPublicacion = '';
    this.exitoPublicacion = '';
    this.publicando = true;
    this.api.post<SocialPublishResponse>('/social/publish', datos).subscribe({
      next: ({ publicacionId }) => {
        this.publicando = false;
        this.exitoPublicacion = `Publicación enviada correctamente (id ${publicacionId}).`;
        this.textoPublicacion = '';
        this.imagenPublicacion = null;
        if (this.archivoImagen) this.archivoImagen.nativeElement.value = '';
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.publicando = false;
        this.errorPublicacion = e?.error?.message ?? 'No se pudo publicar en la red social.';
        this.cdr.markForCheck();
      },
    });
  }
}