import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
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

@Component({
  selector: 'app-redes-sociales',
  templateUrl: './redes-sociales.html',
  imports: [CommonModule, UiPageHeaderComponent],
})
export class RedesSocialesComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly confirmacion = inject(UiConfirmService);

  status?: SocialStatus;
  error = '';
  aviso = '';
  cargando = false;
  conectando = '';

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
}