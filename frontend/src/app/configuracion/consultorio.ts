import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { UiPageHeaderComponent } from '../ui';

interface Consultorio {
  nombre: string;
  direccion: string;
  telefono: string;
  enlaceUbicacion: string;
  horarioAtencion: string;
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
    direccion: '',
    telefono: '',
    enlaceUbicacion: '',
    horarioAtencion: '',
  };
  cargando = true;
  guardando = false;
  error = '';
  exito = '';

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
    this.api.put<Consultorio>('/configuracion/consultorio', this.form).subscribe({
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
}