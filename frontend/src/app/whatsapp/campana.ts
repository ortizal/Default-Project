import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../core/api';

interface ResultadoDestinatario {
  nombre: string;
  telefono: string;
  enviado: boolean;
  error?: string;
}

interface CampanaResponse {
  total: number;
  enviados: number;
  fallidos: number;
  resultados: ResultadoDestinatario[];
}

@Component({
  selector: 'app-whatsapp-campana',
  templateUrl: './campana.html',
  styleUrl: './campana.css',
  imports: [CommonModule, FormsModule, RouterLink],
})
export class CampanaComponent {
  texto = '';
  enlace = '';
  archivo: File | null = null;
  enviando = false;
  error = '';
  resultado?: CampanaResponse;

  constructor(private readonly api: Api, private readonly cdr: ChangeDetectorRef) {}

  seleccionarArchivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.archivo = input.files?.[0] ?? null;
    this.error = '';
    this.cdr.markForCheck();
  }

  quitarArchivo(): void {
    this.archivo = null;
  }

  enviar(): void {
    if (!this.texto.trim() && !this.enlace.trim() && !this.archivo) {
      this.error = 'Agrega un texto, enlace, imagen o video.';
      return;
    }
    const contenido = this.archivo ? ` y el archivo “${this.archivo.name}”` : '';
    if (!confirm(`Se enviará esta publicidad a todos los pacientes activos${contenido}. ¿Continuar?`)) return;

    const form = new FormData();
    if (this.texto.trim()) form.append('texto', this.texto.trim());
    if (this.enlace.trim()) form.append('enlace', this.enlace.trim());
    if (this.archivo) form.append('archivo', this.archivo, this.archivo.name);

    this.enviando = true;
    this.error = '';
    this.resultado = undefined;
    this.api.post<CampanaResponse>('/whatsapp/campanas', form).subscribe({
      next: (r) => {
        this.resultado = r;
        this.enviando = false;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = e?.error?.message || 'No se pudo enviar la campaña.';
        this.enviando = false;
        this.cdr.markForCheck();
      },
    });
  }
}
