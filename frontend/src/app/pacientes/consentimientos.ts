import { CommonModule } from '@angular/common';
import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, HostListener, Input, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../core/api';
import { AuthService } from '../core/auth.service';
import { withLoading } from '../core/loading';
import { Consentimiento, ConsentimientoResumen, Odontologo, Paciente, PlantillaConsentimiento, Tutor } from '../core/models';
import { UiBadgeComponent, UiBadgeTone, UiButtonComponent } from '../ui';

export function etiquetaEstadoConsentimiento(estado: ConsentimientoResumen['estado']): string {
  return estado === 'FIRMADO' ? 'Firmado' : 'Borrador';
}

export function toneEstadoConsentimiento(estado: ConsentimientoResumen['estado']): UiBadgeTone {
  return estado === 'FIRMADO' ? 'ok' : 'warn';
}

/** Etiqueta de un tutor registrado: "Alexis Ortiz (PADRE)". */
export function etiquetaTutor(t: Tutor): string {
  const nombre = `${t.nombres ?? ''} ${t.apellidos ?? ''}`.trim();
  return `${nombre} (${t.parentesco})`;
}

/** El consentimiento de un menor sólo lo autoriza su tutor (§4.14). */
export function esMenor(fechaNacimiento: string | null | undefined, hoy = new Date()): boolean {
  if (!fechaNacimiento) return false;
  const nacimiento = new Date(fechaNacimiento);
  if (Number.isNaN(nacimiento.getTime())) return false;
  let edad = hoy.getFullYear() - nacimiento.getFullYear();
  const m = hoy.getMonth() - nacimiento.getMonth();
  if (m < 0 || (m === 0 && hoy.getDate() < nacimiento.getDate())) edad--;
  return edad < 18;
}

/** Parrafos del texto de un consentimiento para el PDF/lectura en pantalla. */
export function parrafos(texto: string): string[] {
  return (texto || '')
    .split(/\n{2,}/)
    .map((p) => p.trim())
    .filter((p) => !!p);
}

interface FormNuevo {
  plantillaCodigo: string;
  fecha: string;
  tutorNombre: string;
  tutorId: number | null;
  odontologoId: number | null;
  observaciones: string;
}

interface FormEditar {
  fecha: string;
  titulo: string;
  contenido: string;
  tutorNombre: string;
  tutorId: number | null;
  odontologoId: number | null;
  observaciones: string;
}

@Component({
  selector: 'app-consentimientos',
  templateUrl: './consentimientos.html',
  styleUrl: './consentimientos.css',
  imports: [CommonModule, FormsModule, UiButtonComponent, UiBadgeComponent],
})
export class ConsentimientosComponent implements OnInit, AfterViewInit {
  private readonly api = inject(Api);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);

  @Input() pacienteId = 0;
  @ViewChild('firmaCanvas') firmaCanvas?: ElementRef<HTMLCanvasElement>;

  readonly etiquetaEstado = etiquetaEstadoConsentimiento;
  readonly toneEstado = toneEstadoConsentimiento;
  readonly parrafos = parrafos;
  readonly etiquetaTutor = etiquetaTutor;

  escribir = false;
  cargando = false;
  error = '';
  exito = '';

  consentimientos: ConsentimientoResumen[] = [];
  consentimiento: Consentimiento | null = null;
  plantillas: PlantillaConsentimiento[] = [];
  odontologos: Odontologo[] = [];
  tutores: Tutor[] = [];
  menor = false;

  modalConsentimiento = false;
  editando = false;
  formNuevo: FormNuevo = {
    plantillaCodigo: '',
    fecha: '',
    tutorNombre: '',
    tutorId: null,
    odontologoId: null,
    observaciones: '',
  };
  formEditar: FormEditar = {
    fecha: '',
    titulo: '',
    contenido: '',
    tutorNombre: '',
    tutorId: null,
    odontologoId: null,
    observaciones: '',
  };

  modalFirmar = false;
  firmante = '';
  firmaDibujada = false;

  modalEliminar = false;

  modalPlantillas = false;
  plantillaSel = '';
  plantillaContenido = '';
  plantillaVersion = 0;

  private trazando = false;
  private ultimoX = 0;
  private ultimoY = 0;

  ngOnInit(): void {
    this.escribir = this.auth.puede('CONSENTIMIENTOS_WRITE');
    this.cargar();
    this.cargarPlantillas();
    this.cargarPaciente();
    if (this.escribir) this.cargarOdontologos();
  }

  ngAfterViewInit(): void {
    if (this.modalFirmar) this.iniciarLienzo();
  }

  // ------------------------------------------------------------------
  // Cargas
  // ------------------------------------------------------------------

  cargar(): void {
    this.error = '';
    withLoading(this, this.api.get<ConsentimientoResumen[]>(`/historia-clinica/pacientes/${this.pacienteId}/consentimientos`), undefined, this.cdr).subscribe({
      next: (r) => {
        this.consentimientos = r;
        if (r.length) this.seleccionarId(this.consentimiento?.id ?? r[0].id);
        else this.consentimiento = null;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  seleccionar(r: ConsentimientoResumen): void {
    this.seleccionarId(r.id);
  }

  private seleccionarId(id: number): void {
    this.error = '';
    this.api.get<Consentimiento>(`/historia-clinica/consentimientos/${id}`).subscribe({
      next: (r) => {
        this.consentimiento = r;
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  private cargarPlantillas(): void {
    this.api.get<PlantillaConsentimiento[]>('/historia-clinica/plantillas-consentimiento').subscribe({
      next: (r) => {
        this.plantillas = r;
        if (!this.formNuevo.plantillaCodigo && r.length) this.formNuevo.plantillaCodigo = r[0].codigo;
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  /** Tutores del paciente (§4.14) y control de menor de edad. */
  private cargarPaciente(): void {
    this.api.get<Paciente>('/pacientes/' + this.pacienteId).subscribe({
      next: (p) => {
        this.tutores = (p.tutores ?? []).filter((t) => !!t.id);
        this.menor = esMenor(p.fechaNacimiento);
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  private cargarOdontologos(): void {
    this.api.get<Odontologo[]>('/odontologos/activos').subscribe({
      next: (r) => {
        this.odontologos = r;
        this.cdr.markForCheck();
      },
      error: () => undefined,
    });
  }

  // ------------------------------------------------------------------
  // Alta y edición
  // ------------------------------------------------------------------

  nuevo(): void {
    if (!this.escribir) return;
    this.editando = false;
    this.formNuevo = {
      plantillaCodigo: this.plantillas[0]?.codigo ?? '',
      fecha: this.hoy(),
      tutorNombre: '',
      tutorId: this.menor ? this.tutores[0]?.id ?? null : null,
      odontologoId: null,
      observaciones: '',
    };
    this.error = '';
    this.modalConsentimiento = true;
  }

  editar(): void {
    if (!this.escribir || !this.consentimiento || this.consentimiento.estado === 'FIRMADO') return;
    this.editando = true;
    this.formEditar = {
      fecha: this.consentimiento.fecha,
      titulo: this.consentimiento.titulo,
      contenido: this.consentimiento.contenido,
      tutorNombre: this.consentimiento.tutorNombre ?? '',
      tutorId: this.consentimiento.tutorId,
      odontologoId: this.consentimiento.odontologoId,
      observaciones: this.consentimiento.observaciones ?? '',
    };
    this.error = '';
    this.modalConsentimiento = true;
  }

  /**
   * §4.14: el tutor se elige de los ya registrados en el paciente; al
   * elegirlo se copia su nombre y al quitarlo vuelve el texto libre.
   */
  asignarTutor(tutorId: number | null): void {
    const t = this.tutores.find((x) => x.id === tutorId);
    const nombre = t ? `${t.nombres ?? ''} ${t.apellidos ?? ''}`.trim() : '';
    if (this.editando) {
      this.formEditar.tutorId = tutorId;
      this.formEditar.tutorNombre = nombre;
    } else {
      this.formNuevo.tutorId = tutorId;
      this.formNuevo.tutorNombre = nombre;
    }
    this.cdr.markForCheck();
  }

  get errorForm(): string {
    if (this.editando) {
      if (!this.formEditar.fecha) return 'La fecha es obligatoria';
      if (!this.formEditar.titulo.trim()) return 'El título es obligatorio';
      if (!this.formEditar.contenido.trim()) return 'El texto del consentimiento no puede quedar vacío';
      return '';
    }
    if (!this.formNuevo.plantillaCodigo) return 'Elija una plantilla';
    if (!this.formNuevo.fecha) return 'La fecha es obligatoria';
    if (this.menor && !this.formNuevo.tutorId && !this.formNuevo.tutorNombre.trim())
      return 'El paciente es menor de edad: elija el tutor que autoriza';
    return '';
  }

  guardar(): void {
    if (this.errorForm) return;
    const destino = this.editando
      ? this.api.put<Consentimiento>(`/historia-clinica/consentimientos/${this.consentimiento!.id}`, {
          fecha: this.formEditar.fecha || null,
          titulo: this.formEditar.titulo.trim(),
          contenido: this.formEditar.contenido,
          tutorNombre: this.formEditar.tutorNombre.trim() || null,
          tutorId: this.formEditar.tutorId,
          odontologoId: this.formEditar.odontologoId,
          observaciones: this.formEditar.observaciones.trim() || null,
        })
      : this.api.post<Consentimiento>(`/historia-clinica/pacientes/${this.pacienteId}/consentimientos`, {
          plantillaCodigo: this.formNuevo.plantillaCodigo,
          fecha: this.formNuevo.fecha || null,
          tutorNombre: this.formNuevo.tutorNombre.trim() || null,
          tutorId: this.formNuevo.tutorId,
          odontologoId: this.formNuevo.odontologoId,
          observaciones: this.formNuevo.observaciones.trim() || null,
        });
    destino.subscribe({
      next: (r) => {
        this.modalConsentimiento = false;
        this.ok(this.editando ? 'Consentimiento actualizado' : `Consentimiento No. ${r.id} emitido en borrador`);
        this.consentimiento = r;
        this.cargar();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  // ------------------------------------------------------------------
  // Firma y borrado
  // ------------------------------------------------------------------

  abrirFirmar(): void {
    if (!this.escribir || !this.consentimiento || this.consentimiento.estado === 'FIRMADO') return;
    this.firmante = this.consentimiento.tutorNombre ?? '';
    this.firmaDibujada = false;
    this.error = '';
    this.modalFirmar = true;
    setTimeout(() => this.iniciarLienzo());
  }

  firmar(): void {
    if (!this.consentimiento || !this.firmante.trim()) return;
    this.error = '';
    // §4.14: en un menor sólo puede firmar su tutor registrado.
    if (this.menor && !this.coincideConTutor(this.firmante, this.consentimiento.tutorNombre)) {
      this.error = `Sólo el tutor registrado${
        this.consentimiento.tutorNombre ? ' (' + this.consentimiento.tutorNombre + ')' : ''
      } puede firmar este consentimiento.`;
      this.cdr.markForCheck();
      return;
    }
    this.api
      .post<Consentimiento>(`/historia-clinica/consentimientos/${this.consentimiento.id}/firmar`, {
        firmante: this.firmante.trim(),
        firmaImagen: this.imagenFirma(),
      })
      .subscribe({
        next: (r) => {
          this.modalFirmar = false;
          this.ok('Consentimiento firmado y congelado');
          this.consentimiento = r;
          this.cargar();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  abrirEliminar(): void {
    if (!this.escribir || !this.consentimiento || this.consentimiento.estado === 'FIRMADO') return;
    this.error = '';
    this.modalEliminar = true;
  }

  eliminar(): void {
    if (!this.consentimiento) return;
    this.api.del<void>(`/historia-clinica/consentimientos/${this.consentimiento.id}`).subscribe({
      next: () => {
        this.modalEliminar = false;
        this.ok('Consentimiento eliminado');
        this.consentimiento = null;
        this.cargar();
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  // ------------------------------------------------------------------
  // Plantillas
  // ------------------------------------------------------------------

  abrirPlantillas(): void {
    if (!this.escribir || !this.plantillas.length) return;
    this.error = '';
    this.seleccionarPlantilla(this.plantillaSel || this.plantillas[0].codigo);
    this.modalPlantillas = true;
  }

  seleccionarPlantilla(codigo: string): void {
    const p = this.plantillas.find((x) => x.codigo === codigo);
    if (!p) return;
    this.plantillaSel = p.codigo;
    this.plantillaContenido = p.contenido;
    this.plantillaVersion = p.version;
  }

  guardarPlantilla(): void {
    if (!this.plantillaSel || !this.plantillaContenido.trim()) return;
    this.error = '';
    this.api
      .put<PlantillaConsentimiento>(`/historia-clinica/plantillas-consentimiento/${this.plantillaSel}`, {
        contenido: this.plantillaContenido,
      })
      .subscribe({
        next: (r) => {
          this.modalPlantillas = false;
          this.ok(`Plantilla actualizada a la versión ${r.version}`);
          this.cargarPlantillas();
        },
        error: (e) => {
          this.error = this.msg(e);
          this.cdr.markForCheck();
        },
      });
  }

  // ------------------------------------------------------------------
  // Lienzo de firma
  // ------------------------------------------------------------------

  private iniciarLienzo(): void {
    const canvas = this.firmaCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.strokeStyle = '#14213d';
    this.limpiarFirma();
  }

  limpiarFirma(): void {
    const canvas = this.firmaCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    this.firmaDibujada = false;
    this.cdr.markForCheck();
  }

  private posicion(e: PointerEvent): { x: number; y: number } {
    const canvas = this.firmaCanvas?.nativeElement;
    if (!canvas) return { x: 0, y: 0 };
    const rect = canvas.getBoundingClientRect();
    const scaleX = rect.width ? canvas.width / rect.width : 1;
    const scaleY = rect.height ? canvas.height / rect.height : 1;
    return { x: (e.clientX - rect.left) * scaleX, y: (e.clientY - rect.top) * scaleY };
  }

  inicioTrazo(e: PointerEvent): void {
    const ctx = this.firmaCanvas?.nativeElement.getContext('2d');
    if (!ctx) return;
    this.trazando = true;
    const p = this.posicion(e);
    this.ultimoX = p.x;
    this.ultimoY = p.y;
    ctx.beginPath();
    ctx.moveTo(p.x, p.y);
    ctx.lineTo(p.x, p.y);
    ctx.stroke();
    this.firmaDibujada = true;
    e.preventDefault();
  }

  trazar(e: PointerEvent): void {
    if (!this.trazando) return;
    const ctx = this.firmaCanvas?.nativeElement.getContext('2d');
    if (!ctx) return;
    const p = this.posicion(e);
    ctx.beginPath();
    ctx.moveTo(this.ultimoX, this.ultimoY);
    ctx.lineTo(p.x, p.y);
    ctx.stroke();
    this.ultimoX = p.x;
    this.ultimoY = p.y;
    e.preventDefault();
  }

  finTrazo(): void {
    this.trazando = false;
  }

  private imagenFirma(): string | null {
    if (!this.firmaDibujada) return null;
    const canvas = this.firmaCanvas?.nativeElement;
    if (!canvas) return null;
    try {
      return canvas.toDataURL('image/png');
    } catch {
      return null;
    }
  }

  // ------------------------------------------------------------------
  // PDF y utilidades
  // ------------------------------------------------------------------

  descargarPdf(): void {
    if (!this.consentimiento) return;
    this.error = '';
    this.api.getBlob(`/historia-clinica/consentimientos/${this.consentimiento.id}/pdf`).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `consentimiento-${this.consentimiento!.id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (e) => {
        this.error = this.msg(e);
        this.cdr.markForCheck();
      },
    });
  }

  /** Compara el firmante con el tutor sin importar mayúsculas ni espacios. */
  private coincideConTutor(firmante: string, tutor: string | null): boolean {
    const norm = (v: string) => v.trim().toLowerCase().replace(/\s+/g, ' ');
    return !!tutor && !!tutor.trim() && norm(firmante) === norm(tutor);
  }

  trackById(_: number, item: { id: number }): number {
    return item.id;
  }

  trackByCodigo(_: number, item: { codigo: string }): string {
    return item.codigo;
  }

  close(e: MouseEvent): void {
    if (e.target !== e.currentTarget) return;
    this.cerrarTodos();
  }

  stop(e: MouseEvent): void {
    e.stopPropagation();
  }

  cerrarTodos(): void {
    this.modalConsentimiento = false;
    this.modalFirmar = false;
    this.modalEliminar = false;
    this.modalPlantillas = false;
  }

  @HostListener('document:keydown.escape')
  cerrarConEscape(): void {
    this.cerrarTodos();
  }

  private hoy(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private ok(mensaje: string): void {
    this.exito = mensaje;
    this.error = '';
    setTimeout(() => {
      this.exito = '';
      this.cdr.markForCheck();
    }, 3000);
    this.cdr.markForCheck();
  }

  private msg(e: unknown): string {
    const a = e as { error?: { message?: string; mensaje?: string }; status?: number };
    if (a?.status === 409 || a?.status === 400 || a?.status === 404)
      return a.error?.message ?? a.error?.mensaje ?? 'Datos inválidos';
    return 'Error de conexión';
  }
}
