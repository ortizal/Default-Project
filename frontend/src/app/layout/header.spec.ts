import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { HeaderComponent } from './header';

const CLAVE = 'ls-theme';
type Oyente = () => void;

let sistemaOscuro = false;
const oyentes: Oyente[] = [];

/** jsdom no trae matchMedia: se instala uno controlable por el test. */
function instalarMatchMedia(): void {
  (window as unknown as { matchMedia: (q: string) => unknown }).matchMedia = (query: string) => ({
    get matches(): boolean {
      return sistemaOscuro && query.includes('dark');
    },
    media: query,
    onchange: null,
    addEventListener: (_tipo: string, cb: Oyente) => oyentes.push(cb),
    removeEventListener: (_tipo: string, cb: Oyente) => {
      const i = oyentes.indexOf(cb);
      if (i >= 0) oyentes.splice(i, 1);
    },
    addListener: () => undefined,
    removeListener: () => undefined,
    dispatchEvent: () => false,
  });
}

function cambiarSistema(): void {
  for (const cb of [...oyentes]) cb();
}

function clases(): string[] {
  return Array.from(document.documentElement.classList);
}

describe('HeaderComponent — alternador de tema del topbar', () => {
  let fixture: ComponentFixture<HeaderComponent>;

  beforeEach(async () => {
    sistemaOscuro = false;
    oyentes.length = 0;
    localStorage.clear();
    document.documentElement.classList.remove('theme-dark', 'theme-light');
    instalarMatchMedia();

    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
      providers: [provideNoopAnimations()],
    }).compileComponents();
    fixture = TestBed.createComponent(HeaderComponent);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    document.documentElement.classList.remove('theme-dark', 'theme-light');
    localStorage.clear();
  });

  it('sigue al sistema mientras no haya preferencia guardada', () => {
    expect(clases()).toEqual(['theme-light']);
    expect(fixture.componentInstance.oscuro()).toBe(false);
    expect(fixture.componentInstance.etiquetaTema()).toBe('Cambiar a modo oscuro');
  });

  it('aplica el tema oscuro del sistema en caliente', () => {
    sistemaOscuro = true;
    cambiarSistema();

    expect(clases()).toEqual(['theme-dark']);
    expect(fixture.componentInstance.oscuro()).toBe(true);
    expect(fixture.componentInstance.etiquetaTema()).toBe('Cambiar a modo claro');
  });

  it('la preferencia guardada prevalece sobre el sistema', () => {
    localStorage.setItem(CLAVE, 'dark');
    fixture = TestBed.createComponent(HeaderComponent);
    fixture.detectChanges();

    expect(clases()).toEqual(['theme-dark']);
    expect(fixture.componentInstance.oscuro()).toBe(true);

    // El sistema pasa a claro: no debe tocar la elección del usuario.
    sistemaOscuro = false;
    cambiarSistema();
    expect(clases()).toEqual(['theme-dark']);
    expect(fixture.componentInstance.oscuro()).toBe(true);
  });

  it('alternarTema cambia la clase, la etiqueta y persiste la elección', () => {
    expect(fixture.componentInstance.oscuro()).toBe(false);

    fixture.componentInstance.alternarTema();
    fixture.detectChanges();

    expect(clases()).toEqual(['theme-dark']);
    expect(fixture.componentInstance.oscuro()).toBe(true);
    expect(fixture.componentInstance.etiquetaTema()).toBe('Cambiar a modo claro');
    expect(localStorage.getItem(CLAVE)).toBe('dark');

    fixture.componentInstance.alternarTema();
    fixture.detectChanges();

    expect(clases()).toEqual(['theme-light']);
    expect(localStorage.getItem(CLAVE)).toBe('light');
  });

  it('el botón del topbar expone nombre accesible y acciona el alternador', () => {
    const boton = fixture.nativeElement.querySelector('.topbar-tema') as HTMLButtonElement;
    expect(boton.getAttribute('aria-label')).toBe('Cambiar a modo oscuro');

    boton.click();
    fixture.detectChanges();

    expect(boton.getAttribute('aria-label')).toBe('Cambiar a modo claro');
    expect(clases()).toContain('theme-dark');
  });

  it('al destruirse deja de escuchar los cambios del sistema', () => {
    expect(oyentes.length).toBe(1);
    fixture.destroy();
    expect(oyentes.length).toBe(0);
  });
});
