import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  ConsentimientosComponent,
  esMenor,
  etiquetaEstadoConsentimiento,
  etiquetaTutor,
  parrafos,
  toneEstadoConsentimiento,
} from './consentimientos';
import { Consentimiento, ConsentimientoResumen, PlantillaConsentimiento, Tutor } from '../core/models';

function plantilla(extra: Partial<PlantillaConsentimiento> = {}): PlantillaConsentimiento {
  return {
    id: 1,
    codigo: 'TRATAMIENTO_GENERAL',
    nombre: 'Consentimiento para tratamiento dental',
    categoria: 'Tratamientos',
    contenido: 'Primera línea.\n\nSegunda línea con riesgos.',
    version: 1,
    ...extra,
  };
}

function tutor(extra: Partial<Tutor> = {}): Tutor {
  return { id: 5, parentesco: 'PADRE', nombres: 'Alexis', apellidos: 'Ortiz', telefono: '+593987654321', ...extra };
}

function resumen(extra: Partial<ConsentimientoResumen> = {}): ConsentimientoResumen {
  return {
    id: 41,
    fecha: '2026-10-05',
    titulo: 'Consentimiento para tratamiento dental',
    categoria: 'Tratamientos',
    plantillaCodigo: 'TRATAMIENTO_GENERAL',
    plantillaVersion: 1,
    estado: 'BORRADOR',
    odontologo: 'Luis Ortiz',
    tutorNombre: null,
    tutorId: null,
    tutorParentesco: null,
    tutorTelefono: null,
    firmante: null,
    firmaHash: null,
    firmadoEn: null,
    ...extra,
  };
}

function consentimiento(extra: Partial<Consentimiento> = {}): Consentimiento {
  return {
    id: 41,
    pacienteId: 1,
    paciente: 'Ana Pérez',
    plantillaCodigo: 'TRATAMIENTO_GENERAL',
    plantillaVersion: 1,
    titulo: 'Consentimiento para tratamiento dental',
    categoria: 'Tratamientos',
    contenido: 'Primera línea.\n\nSegunda línea con riesgos.',
    fecha: '2026-10-05',
    tutorNombre: null,
    tutorId: null,
    tutorParentesco: null,
    tutorTelefono: null,
    odontologoId: 7,
    odontologo: 'Luis Ortiz',
    estado: 'BORRADOR',
    firmante: null,
    firmaImagen: null,
    firmaHash: null,
    firmadoEn: null,
    observaciones: null,
    createdAt: '',
    updatedAt: '',
    ...extra,
  };
}

describe('Consentimientos — helpers (Fase E, §4.13)', () => {
  it('rotula el estado y su color', () => {
    expect(etiquetaEstadoConsentimiento('BORRADOR')).toBe('Borrador');
    expect(etiquetaEstadoConsentimiento('FIRMADO')).toBe('Firmado');
    expect(toneEstadoConsentimiento('BORRADOR')).toBe('warn');
    expect(toneEstadoConsentimiento('FIRMADO')).toBe('ok');
  });

  it('etiqueta a los tutores con su parentesco (§4.14)', () => {
    expect(etiquetaTutor(tutor())).toBe('Alexis Ortiz (PADRE)');
    expect(etiquetaTutor(tutor({ parentesco: 'MADRE', apellidos: null as unknown as string }))).toBe(
      'Alexis (MADRE)',
    );
  });

  it('calcula la mayoría de edad a partir de la fecha de nacimiento', () => {
    const hoy = new Date('2026-10-05');
    expect(esMenor('2014-10-06', hoy)).toBe(true);
    expect(esMenor('2008-10-05', hoy)).toBe(false);
    expect(esMenor('2008-10-04', hoy)).toBe(false);
    expect(esMenor(null, hoy)).toBe(false);
    expect(esMenor('no-es-fecha', hoy)).toBe(false);
  });

  it('parte el texto en párrafos ignorando líneas sueltas', () => {
    expect(parrafos('Uno.\n\n\n\nDos.\nTercera línea')).toEqual(['Uno.', 'Dos.\nTercera línea']);
    expect(parrafos('')).toEqual([]);
    expect(parrafos(null as unknown as string)).toEqual([]);
  });
});

describe('ConsentimientosComponent — composición', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsentimientosComponent],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();
  });

  it('arranca sin permisos, sin consentimiento y sin modales', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;

    expect(c.escribir).toBe(false);
    expect(c.consentimiento).toBeNull();
    expect(c.consentimientos).toEqual([]);
    expect(c.modalConsentimiento).toBe(false);
    expect(c.modalFirmar).toBe(false);
    expect(c.modalEliminar).toBe(false);
    expect(c.modalPlantillas).toBe(false);
    expect(c.firmante).toBe('');
  });

  it('el consentimiento nuevo sólo se abre con permiso y elige la primera plantilla', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;

    c.nuevo();
    expect(c.modalConsentimiento).toBe(false);

    c.escribir = true;
    c.plantillas = [plantilla(), plantilla({ codigo: 'IMPLANTE', nombre: 'Implante' })];
    c.nuevo();
    expect(c.modalConsentimiento).toBe(true);
    expect(c.editando).toBe(false);
    expect(c.formNuevo.plantillaCodigo).toBe('TRATAMIENTO_GENERAL');
    expect(c.formNuevo.fecha).toBeTruthy();
  });

  it('valida plantilla y fecha en el alta', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;

    c.formNuevo.plantillaCodigo = '';
    expect(c.errorForm).toContain('plantilla');

    c.formNuevo.plantillaCodigo = 'IMPLANTE';
    c.formNuevo.fecha = '';
    expect(c.errorForm).toContain('fecha');

    c.formNuevo.fecha = '2026-10-05';
    expect(c.errorForm).toBe('');
  });

  it('editar exige permiso y consentimiento en borrador', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;

    c.editar();
    expect(c.modalConsentimiento).toBe(false);

    c.escribir = true;
    c.consentimiento = consentimiento({ estado: 'FIRMADO', firmante: 'Ana Pérez' });
    c.editar();
    expect(c.modalConsentimiento).toBe(false);

    c.consentimiento = consentimiento();
    c.editar();
    expect(c.modalConsentimiento).toBe(true);
    expect(c.editando).toBe(true);
    expect(c.formEditar.titulo).toBe('Consentimiento para tratamiento dental');
    expect(c.formEditar.contenido).toContain('Segunda línea');
    expect(c.formEditar.odontologoId).toBe(7);
  });

  it('valida título y texto al editar', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;
    c.editando = true;

    c.formEditar.fecha = '';
    expect(c.errorForm).toContain('fecha');

    c.formEditar.fecha = '2026-10-05';
    c.formEditar.titulo = '  ';
    expect(c.errorForm).toContain('título');

    c.formEditar.titulo = 'Extracción de la 48';
    c.formEditar.contenido = ' ';
    expect(c.errorForm).toContain('texto');

    c.formEditar.contenido = 'Texto nuevo.';
    expect(c.errorForm).toBe('');
  });

  it('la firma sólo se abre en borradores con permiso y precarga el tutor', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;

    c.abrirFirmar();
    expect(c.modalFirmar).toBe(false);

    c.escribir = true;
    c.consentimiento = consentimiento({ estado: 'FIRMADO', firmante: 'Ana Pérez' });
    c.abrirFirmar();
    expect(c.modalFirmar).toBe(false);

    c.consentimiento = consentimiento({ tutorNombre: 'María Pérez' });
    c.abrirFirmar();
    expect(c.modalFirmar).toBe(true);
    expect(c.firmante).toBe('María Pérez');
    expect(c.firmaDibujada).toBe(false);
  });

  it('el borrado sólo se ofrece en borradores con permiso', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;

    c.abrirEliminar();
    expect(c.modalEliminar).toBe(false);

    c.escribir = true;
    c.consentimiento = consentimiento({ estado: 'FIRMADO' });
    c.abrirEliminar();
    expect(c.modalEliminar).toBe(false);

    c.consentimiento = consentimiento();
    c.abrirEliminar();
    expect(c.modalEliminar).toBe(true);
  });

  it('las plantillas se seleccionan y conservan su versión', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;

    c.abrirPlantillas();
    expect(c.modalPlantillas).toBe(false);

    c.escribir = true;
    c.plantillas = [plantilla(), plantilla({ codigo: 'IMPLANTE', version: 4, contenido: 'Texto 4' })];
    c.abrirPlantillas();
    expect(c.modalPlantillas).toBe(true);
    expect(c.plantillaSel).toBe('TRATAMIENTO_GENERAL');
    expect(c.plantillaContenido).toContain('Primera línea');

    c.seleccionarPlantilla('IMPLANTE');
    expect(c.plantillaVersion).toBe(4);
    expect(c.plantillaContenido).toBe('Texto 4');

    c.seleccionarPlantilla('NO_EXISTE');
    expect(c.plantillaSel).toBe('IMPLANTE');
  });

  it('el lienzo de firma empieza vacío y limpia sin error si no hay canvas', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;

    expect(c.firmaDibujada).toBe(false);
    c.limpiarFirma();
    expect(c.firmaDibujada).toBe(false);
    c.finTrazo();
    expect(c.firmaDibujada).toBe(false);
  });

  it('el tutor se elige de los registrados y al quitarlo deja texto libre (§4.14)', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;
    c.tutores = [tutor(), tutor({ id: 6, parentesco: 'MADRE', nombres: 'Annie' })];

    c.nuevo = c.nuevo.bind(c);
    c.asignarTutor(6);
    expect(c.formNuevo.tutorId).toBe(6);
    expect(c.formNuevo.tutorNombre).toBe('Annie Ortiz');

    c.asignarTutor(null);
    expect(c.formNuevo.tutorId).toBeNull();
    expect(c.formNuevo.tutorNombre).toBe('');

    c.editando = true;
    c.formEditar.tutorId = 5;
    c.asignarTutor(5);
    expect(c.formEditar.tutorId).toBe(5);
    expect(c.formEditar.tutorNombre).toBe('Alexis Ortiz');
  });

  it('un menor de edad necesita tutor para emitir el consentimiento', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;
    c.tutores = [tutor()];
    c.formNuevo.plantillaCodigo = 'TRATAMIENTO_GENERAL';
    c.formNuevo.fecha = '2026-10-05';

    c.menor = true;
    expect(c.errorForm).toContain('menor de edad');
    c.asignarTutor(5);
    expect(c.errorForm).toBe('');

    c.asignarTutor(null);
    c.formNuevo.tutorNombre = 'Alexis Ortiz';
    expect(c.errorForm).toBe('');

    c.menor = false;
    expect(c.errorForm).toBe('');
  });

  it('la firma de un menor sólo se acepta si coincide con su tutor', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;
    c.escribir = true;
    c.menor = true;
    c.consentimiento = consentimiento({ tutorNombre: 'Alexis Ortiz', tutorId: 5 });

    c.firmante = 'Juan Pérez';
    c.firmar();
    expect(c.error).toContain('Sólo el tutor registrado');
    expect(c.error).toContain('Alexis Ortiz');
    expect(c.error).toContain('puede firmar este consentimiento');
    expect(c.modalFirmar).toBe(false);

    c.abrirFirmar();
    expect(c.firmante).toBe('Alexis Ortiz');
  });

  it('Escape cierra los cuatro modales y conserva la lista', () => {
    const fixture = TestBed.createComponent(ConsentimientosComponent);
    const c = fixture.componentInstance;

    c.consentimientos = [resumen(), resumen({ id: 40, fecha: '2026-09-01' })];
    c.modalConsentimiento = true;
    c.modalFirmar = true;
    c.modalEliminar = true;
    c.modalPlantillas = true;
    c.cerrarConEscape();

    expect(c.modalConsentimiento).toBe(false);
    expect(c.modalFirmar).toBe(false);
    expect(c.modalEliminar).toBe(false);
    expect(c.modalPlantillas).toBe(false);
    expect(c.consentimientos.length).toBe(2);
  });
});
