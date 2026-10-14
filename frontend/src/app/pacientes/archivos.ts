import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, ElementRef, HostListener, Input, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { withLoading } from '../core/loading';
import { ArchivoClinico, CategoriaArchivo } from '../core/models';
import { UiBadgeComponent, UiBadgeTone, UiButtonComponent, UiConfirmService } from '../ui';

// ---------------------------------------------------------------------
// Catálogos y helpers (espejo de ArchivoClinicoService en el backend)
// ---------------------------------------------------------------------

export const CATEGORIAS_ARCHIVO: { id: CategoriaArchivo; label: string }[] = [
  { id: 'RADIOGRAFIA', label: 'Radiografía' },
  { id: 'FOTOGRAFIA', label: 'Fotografía' },
  { id: 'PANORAMICA', label: 'Panorámica' },
  { id: 'TOMOGRAFIA', label: 'Tomografía' },
  { id: 'RESULTADO', label: 'Resultado' },
  { id: 'DOCUMENTO', label: 'Documento' },
];

/** Extensiones aceptadas por el servidor (plan §4.15). */
export const EXTENSIONES_ARCHIVO = ['jpg', 'jpeg', 'png', 'webp', 'tif', 'tiff', 'pdf', 'dcm'];

/** Límite de tamaño por archivo (20 MB), el mismo que valida el backend. */
export const MAX_ARCHIVO_BYTES = 20 * 1024 * 1024;

export function etiquetaCategoria(categoria: CategoriaArchivo): string {
  return CATEGORIAS_ARCHIVO.find((c) => c.id === categoria)?.label ?? categoria;
}

export function toneCategoria(categoria: CategoriaArchivo): UiBadgeTone {
  switch (categoria) {
    case 'RESULTADO':
      return 'ok';
    case 'DOCUMENTO':
      return 'muted';
    case 'FOTOGRAFIA':
      return 'accent';
    default:
      return 'info';
  }
}

export function extensionArchivo(nombre: string): string {
  const punto = nombre.lastIndexOf('.');
  return punto < 0 ? '' : nombre.slice(punto + 1).toLowerCase();
}

export function formatoBytes(bytes: number): string {
  if (!bytes) return '0 B';
  const unidades = ['B', 'KB', 'MB', 'GB'];
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), unidades.length - 1);
  const valor = bytes / Math.pow(1024, i);
  return `${i === 0 ? valor : valor.toFixed(1)} ${unidades[i]}`;
}

export function esImagen(mime: string): boolean {
  return mime.startsWith('image/');
}

import { MatMenuModule } from '@angular/material/menu';

import { MatIconModule } from '@angular/material/icon';
@Component({
  selector: 'app-archivos',
  templateUrl: './archivos.html',
  styleUrl: './archivos.css',
  imports: [
    MatIconModule,
    MatMenuModule,CommonModule, FormsModule, UiButtonComponent, UiBadgeComponent],
})
export class ArchivosComponent implements OnInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);
  private readonly confirmacion = inject(UiConfirmService);

  @Input() pacienteId = 0;
  @ViewChild('inputArchivo') inputArchivo?: ElementRef<HTMLInputElement>;

  readonly categorias = CATEGORIAS_ARCHIVO;
  readonly etiquetaCategoria = etiquetaCategoria;
  readonly toneCategoria = toneCategoria;
  readonly extensionArchivo = extensionArchivo;
  readonly formatoBytes = formatoBytes;
  readonly esImagen = esImagen;
  readonly extensionesAceptadas = `.${EXTENSIONES_ARCHIVO.join(',.')}`;

  escribir = false;
  cargando = false;
  error = '';
  exito = '';

  archivos: ArchivoClinico[] = [];

  categoria: CategoriaArchivo = 'RADIOGRAFIA';
  fecha = '';
  observaciones = '';
  archivo: File | null = null;
  subiendo = false;

  modalPreview = false;
  previewUrl = '';
  previewNombre = '';
  previewEsImagen = false;

  ngOnInit(): void {
    this.escribir = this.auth.puede('ARCHIVOS_CLINICOS_WRITE');
    this.cargar();
  }

  // ------------------------------------------------------------------
  // Cargas
  // ------------------------------------------------------------------

  cargar(): void {
    this.error = '';
    withLoading(
      this,
      this.api.get<ArchivoClinico[]>(`/historia-clinica/pacientes/${this.pacienteId}/archivos`),
      undefined,
      this.cdr,
    ).subscribe({
      next: (r) => {
        this.archivos = r;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  // ------------------------------------------------------------------
  // Subida
  // ------------------------------------------------------------------

  get errorAlta(): string {
    if (!this.archivo) return 'Selecciona un archivo';
    if (!EXTENSIONES_ARCHIVO.includes(extensionArchivo(this.archivo.name)))
      return 'Formato no permitido: usa JPG, PNG, WEBP, TIFF, PDF o DCM';
    if (this.archivo.size > MAX_ARCHIVO_BYTES) return 'El archivo no puede superar 20 MB';
    if (!this.categoria) return 'Elige la categoría del archivo';
    return '';
  }

  seleccionarArchivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.archivo = input.files?.[0] ?? null;
    this.error = '';
    this.cdr.markForCheck();
  }

  subir(): void {
    if (this.errorAlta || this.subiendo) return;
    this.error = '';
    this.exito = '';
    this.subiendo = true;

    const datos = new FormData();
    datos.append('archivo', this.archivo as File);
    datos.append('categoria', this.categoria);
    if (this.fecha) datos.append('fecha', this.fecha);
    if (this.observaciones.trim()) datos.append('observaciones', this.observaciones.trim());

    this.api
      .post<ArchivoClinico>(`/historia-clinica/pacientes/${this.pacienteId}/archivos`, datos)
      .subscribe({
        next: (a) => {
          this.subiendo = false;
          this.exito = `Archivo "${a.nombre}" subido correctamente.`;
          this.limpiarAlta();
          this.cargar();
          this.cdr.markForCheck();
        },
        error: (e) => {
          this.subiendo = false;
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  limpiarAlta(): void {
    this.archivo = null;
    this.fecha = '';
    this.observaciones = '';
    this.categoria = 'RADIOGRAFIA';
    if (this.inputArchivo) this.inputArchivo.nativeElement.value = '';
    this.cdr.markForCheck();
  }

  // ------------------------------------------------------------------
  // Descarga, vista previa y borrado
  // ------------------------------------------------------------------

  descargar(a: ArchivoClinico): void {
    this.error = '';
    this.api.getBlob(`/historia-clinica/archivos/${a.id}/contenido`).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = a.nombre;
        enlace.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  /** Las imágenes se previsualizan en pantalla; el resto se descarga. */
  ver(a: ArchivoClinico): void {
    if (!esImagen(a.mime)) {
      this.descargar(a);
      return;
    }
    this.error = '';
    this.api.getBlob(`/historia-clinica/archivos/${a.id}/contenido`).subscribe({
      next: (blob) => {
        this.cerrarPreview();
        this.previewUrl = URL.createObjectURL(blob);
        this.previewNombre = a.nombre;
        this.previewEsImagen = true;
        this.modalPreview = true;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  async eliminar(a: ArchivoClinico): Promise<void> {
    if (
      !(await this.confirmacion.abrir({
        titulo: 'Eliminar archivo',
        mensaje: `¿Eliminar "${a.nombre}"? El contenido deja de estar disponible.`,
        confirmarTexto: 'Eliminar',
      }))
    )
      return;
    this.error = '';
    this.api.del<void>(`/historia-clinica/archivos/${a.id}`).subscribe({
      next: () => {
        this.exito = `Archivo "${a.nombre}" eliminado.`;
        this.cargar();
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  close(e: MouseEvent): void {
    if (e.target !== e.currentTarget) return;
    this.cerrarPreview();
  }

  stop(e: MouseEvent): void {
    e.stopPropagation();
  }

  cerrarPreview(): void {
    if (this.previewUrl) URL.revokeObjectURL(this.previewUrl);
    this.previewUrl = '';
    this.modalPreview = false;
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrarPreview();
  }

  trackById(_: number, item: { id: number }): number {
    return item.id;
  }

  private msg(e: { error?: { message?: string } }): string {
    return e?.error?.message ?? 'No se pudo completar la operación.';
  }
}
