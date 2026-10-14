import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import {
  AgendaPublicaComponent,
  ESTADOS_DIA,
  ETIQUETA_DIA,
  nombreMes,
} from './agenda-publica';

const TOKEN = 'abc.def';

function setup(token: string | null = TOKEN) {
  TestBed.configureTestingModule({
    imports: [AgendaPublicaComponent],
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      provideRouter([]),
      {
        provide: ActivatedRoute,
        useValue: { snapshot: { queryParamMap: convertToParamMap(token ? { t: token } : {}) } },
      },
    ],
  });
  return {
    fixture: TestBed.createComponent(AgendaPublicaComponent),
    http: TestBed.inject(HttpTestingController),
  };
}

describe('Agenda pública (§10–§13 del plan)', () => {
  it('sin token muestra el enlace inválido sin llamar a la API', () => {
    const { fixture, http } = setup(null);
    fixture.detectChanges();
    expect(fixture.componentInstance.error).toContain('no es válido');
    http.expectNone((r) => r.url.includes('/public/agenda'));
  });

  it('con token carga contexto y días del mes actual', () => {
    const { fixture, http } = setup();
    fixture.detectChanges();

    http.expectOne(`/api/v1/public/agenda/context?t=${TOKEN}`).flush({
      odontologoId: 10,
      odontologo: 'Ana Paz',
      especialidad: 'Endodoncia',
      paciente: 'Alexis',
      servicioId: 1,
      expiraEnMinutos: 30,
    });

    const hoy = new Date();
    const diasUrl = `/api/v1/public/agenda/dias?t=${TOKEN}&year=${hoy.getFullYear()}&month=${hoy.getMonth() + 1}`;
    http.expectOne(diasUrl).flush([
      { fecha: '2026-10-01', estado: 'PASADO', cupos: 0 },
      { fecha: '2026-10-20', estado: 'DISPONIBLE', cupos: 4 },
    ]);

    expect(fixture.componentInstance.contexto?.paciente).toBe('Alexis');
    expect(fixture.componentInstance.dias.length).toBe(2);
  });

  it('token inválido muestra el error de enlace caducado', () => {
    const { fixture, http } = setup();
    fixture.detectChanges();
    http.expectOne((r) => r.url.includes('/public/agenda/context')).flush(
      { message: 'no' }, { status: 404, statusText: 'Not Found' });
    expect(fixture.componentInstance.error).toContain('caducó');
  });

  it('al elegir cupo y confirmar llama a reservar con el token', () => {
    const { fixture, http } = setup();
    fixture.detectChanges();
    http.expectOne((r) => r.url.includes('/context')).flush({
      odontologoId: 10, odontologo: 'Ana Paz', especialidad: null,
      paciente: 'Alexis', servicioId: 1, expiraEnMinutos: 30,
    });
    const hoy = new Date();
    http.expectOne((r) => r.url.includes('/dias')).flush([
      { fecha: '2099-01-15', estado: 'DISPONIBLE', cupos: 2 },
    ]);

    fixture.componentInstance.seleccionarDia({ fecha: '2099-01-15', estado: 'DISPONIBLE', cupos: 2 });
    http.expectOne((r) => r.url.includes('/slots')).flush([
      {
        horaInicio: '10:00', horaFin: '10:45', odontologoId: 10, odontologo: 'Ana Paz',
        slotToken: 'slot-firmado-1',
      },
    ]);

    fixture.componentInstance.seleccionarSlot({
      horaInicio: '10:00', horaFin: '10:45', odontologoId: 10, odontologo: 'Ana Paz',
      slotToken: 'slot-firmado-1',
    });
    fixture.componentInstance.confirmar();

    const reserva = http.expectOne((r) => r.url.includes('/reservar'));
    const body = reserva.request.body;
    expect(body.token).toBe(TOKEN);
    expect(body.fecha).toBe('2099-01-15');
    expect(body.horaInicio).toBe('10:00');
    expect(body.odontologoId).toBe(10);
    expect(body.servicioId).toBe(1);
    // §15: cupo firmado + clave idempotente para reintentos seguros.
    expect(body.slotToken).toBe('slot-firmado-1');
    expect(typeof body.idempotencyKey).toBe('string');
    expect(body.idempotencyKey.length).toBeGreaterThan(0);
    reserva.flush({
      estado: 'CREADA',
      cita: { servicioNombre: 'Limpieza', doctorNombre: 'Ana Paz', fecha: '2099-01-15', horaInicio: '10:00' },
      alternativas: [],
      mensaje: '¡Listo!',
    });

    expect(fixture.componentInstance.reserva?.estado).toBe('CREADA');
    expect(hoy).toBeTruthy();
  });

  it('ocupada reemplaza la lista por las alternativas', () => {
    const { fixture, http } = setup();
    fixture.detectChanges();
    http.expectOne((r) => r.url.includes('/context')).flush({
      odontologoId: 10, odontologo: 'Ana Paz', especialidad: null,
      paciente: 'Alexis', servicioId: 1, expiraEnMinutos: 30,
    });
    http.expectOne((r) => r.url.includes('/dias')).flush([
      { fecha: '2099-01-15', estado: 'DISPONIBLE', cupos: 2 },
    ]);
    fixture.componentInstance.seleccionarDia({ fecha: '2099-01-15', estado: 'DISPONIBLE', cupos: 2 });
    http.expectOne((r) => r.url.includes('/slots')).flush([
      { horaInicio: '10:00', horaFin: '10:45', odontologoId: 10, odontologo: 'Ana Paz' },
    ]);
    fixture.componentInstance.seleccionarSlot({
      horaInicio: '10:00', horaFin: '10:45', odontologo: 'Ana Paz', odontologoId: 10,
    });
    fixture.componentInstance.confirmar();
    http.expectOne((r) => r.url.includes('/reservar')).flush({
      estado: 'OCUPADA',
      cita: null,
      alternativas: [{ horaInicio: '11:00', horaFin: '11:45', odontologoId: 10, odontologo: 'Ana Paz' }],
      mensaje: 'Ese horario acaba de ocuparse.',
    });

    expect(fixture.componentInstance.reserva?.estado).toBe('OCUPADA');
    expect(fixture.componentInstance.slots.map((s) => s.horaInicio)).toEqual(['11:00']);
    expect(fixture.componentInstance.slotSeleccionado).toBeNull();
  });

  it('las etiquetas de estado cubren los casos del §12 y el no laborable del §14', () => {
    expect(Object.keys(ESTADOS_DIA).sort()).toEqual(
      ['DISPONIBLE', 'NO_LABORABLE', 'PASADO', 'POCA', 'SIN_DISPONIBILIDAD']);
    expect(ETIQUETA_DIA.POCA).toBe('Pocos cupos');
    expect(ETIQUETA_DIA.NO_LABORABLE).toBe('No atiende');
    expect(nombreMes(2026, 10)).toBe('Octubre 2026');
  });

  it('un día no laborable queda deshabilitado y no pide cupos', () => {
    const { fixture, http } = setup();
    fixture.detectChanges();
    http.expectOne((r) => r.url.includes('/context')).flush({
      odontologoId: 10, odontologo: 'Ana Paz', especialidad: null,
      paciente: 'Alexis', servicioId: 1, expiraEnMinutos: 30,
    });
    http.expectOne((r) => r.url.includes('/dias')).flush([
      { fecha: '2099-01-15', estado: 'NO_LABORABLE', cupos: 0 },
    ]);

    fixture.componentInstance.seleccionarDia({ fecha: '2099-01-15', estado: 'NO_LABORABLE', cupos: 0 });

    expect(fixture.componentInstance.diaSeleccionado).toBeNull();
    http.expectNone((r) => r.url.includes('/slots'));
  });

  it('la cabecera muestra la marca y la página ofrece reintentar tras un error', () => {
    const { fixture, http } = setup();
    fixture.detectChanges();
    http.expectOne((r) => r.url.includes('/context')).flush(
      {}, { status: 500, statusText: 'Server Error' });

    expect(fixture.nativeElement.querySelector('h1').textContent).toContain('Little Smile');
    expect(fixture.componentInstance.error).toBeTruthy();

    fixture.componentInstance.reintentar();
    http.expectOne((r) => r.url.includes('/context')).flush({
      odontologoId: 10, odontologo: 'Ana Paz', especialidad: null,
      paciente: 'Alexis', servicioId: 1, expiraEnMinutos: 15,
    });
    http.expectOne((r) => r.url.includes('/dias')).flush([]);

    expect(fixture.componentInstance.error).toBe('');
    expect(fixture.componentInstance.contexto?.odontologo).toBe('Ana Paz');
  });

  it('un enlace de sólo consulta (sin paciente) no saluda por nombre', () => {
    const { fixture, http } = setup();
    fixture.detectChanges();
    http.expectOne((r) => r.url.includes('/context')).flush({
      odontologoId: 10, odontologo: 'Ana Paz', especialidad: null,
      paciente: '', servicioId: 1, expiraEnMinutos: 15,
    });
    http.expectOne((r) => r.url.includes('/dias')).flush([]);
    fixture.detectChanges();

    const sub = fixture.nativeElement.querySelector('.ap-sub').textContent;
    expect(sub).not.toContain('Hola,');
    expect(sub).toContain('Elige el día y la hora con');
  });
});
