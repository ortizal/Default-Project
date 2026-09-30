import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
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

@Component({
  selector: 'app-consultorio',
  templateUrl: './consultorio.html',
  styleUrl: './consultorio.css',
  imports: [CommonModule, FormsModule, UiPageHeaderComponent],
})
export class ConsultorioComponent implements OnInit {
  private readonly api = inject(Api);
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

  ngOnInit(): void {
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