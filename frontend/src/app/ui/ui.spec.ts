import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  UiBadgeComponent,
  UiBadgeTone,
  UiButtonComponent,
  UiConfirmHostComponent,
  UiPageHeaderComponent,
  UiPaginationComponent,
} from './index';
import { UiConfirmService, Confirmacion } from './confirm.service';

@Component({
  template: `<ui-badge [tone]="tone">{{ texto }}</ui-badge>`,
  imports: [UiBadgeComponent],
})
class BadgeHost {
  tone: UiBadgeTone = 'ok';
  texto = 'ACTIVO';
}

@Component({
  template: `<ui-page-header [title]="titulo" [subtitle]="subtitulo">
    <button class="btn">Acción</button>
  </ui-page-header>`,
  imports: [UiPageHeaderComponent],
})
class HeaderHost {
  titulo = 'Pacientes';
  subtitulo = '';
}

describe('Sistema de componentes UI (§6)', () => {
  describe('UiBadgeComponent', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({ imports: [BadgeHost] }).compileComponents();
    });

    it('proyecta el texto y aplica la clase de tono junto a .badge', () => {
      const fixture = TestBed.createComponent(BadgeHost);
      fixture.detectChanges();
      const span = fixture.nativeElement.querySelector('span') as HTMLElement;
      expect(span.textContent).toContain('ACTIVO');
      expect(span.classList.contains('badge')).toBe(true);
      expect(span.classList.contains('ok')).toBe(true);
    });

    it('cambia de tono sin perder la clase base', () => {
      const fixture = TestBed.createComponent(BadgeHost);
      fixture.componentInstance.tone = 'bad';
      fixture.detectChanges();
      const span = fixture.nativeElement.querySelector('span') as HTMLElement;
      expect(span.classList.contains('badge')).toBe(true);
      expect(span.classList.contains('bad')).toBe(true);
      expect(span.classList.contains('ok')).toBe(false);
    });
  });

  describe('UiPageHeaderComponent', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({ imports: [HeaderHost] }).compileComponents();
    });

    it('renderiza el título como h1 y proyecta las acciones', () => {
      const fixture = TestBed.createComponent(HeaderHost);
      fixture.detectChanges();
      const root = fixture.nativeElement as HTMLElement;
      expect(root.querySelector('h1.page-title')?.textContent).toContain('Pacientes');
      expect(root.querySelector('.page-header-actions button')).toBeTruthy();
    });

    it('omite el subtítulo cuando no se informa', () => {
      const fixture = TestBed.createComponent(HeaderHost);
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.page-subtitle')).toBeNull();
    });

    it('renderiza el subtítulo cuando se informa', () => {
      const fixture = TestBed.createComponent(HeaderHost);
      fixture.componentInstance.subtitulo = 'Listado general';
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.page-subtitle')?.textContent).toContain(
        'Listado general',
      );
    });
  });

  describe('UiButtonComponent', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({ imports: [UiButtonComponent] }).compileComponents();
    });

    it('emite clicked y refleja variante y tamaño', () => {
      const fixture = TestBed.createComponent(UiButtonComponent);
      fixture.componentRef.setInput('variant', 'primary');
      fixture.componentRef.setInput('size', 'sm');
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
      expect(button.classList.contains('primary')).toBe(true);
      expect(button.classList.contains('sm')).toBe(true);

      let clicked = false;
      fixture.componentInstance.clicked.subscribe(() => (clicked = true));
      button.click();
      expect(clicked).toBe(true);
    });

    it('se deshabilita y marca aria-busy mientras carga', () => {
      const fixture = TestBed.createComponent(UiButtonComponent);
      fixture.componentRef.setInput('loading', true);
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
      expect(button.disabled).toBe(true);
      expect(button.getAttribute('aria-busy')).toBe('true');
    });

    it('expone aria-label cuando se pasa', () => {
      const fixture = TestBed.createComponent(UiButtonComponent);
      fixture.componentRef.setInput('ariaLabel', 'Borrar sesión');
      fixture.detectChanges();
      const button = fixture.nativeElement.querySelector('button') as HTMLButtonElement;
      expect(button.getAttribute('aria-label')).toBe('Borrar sesión');
    });
  });

  describe('UiPaginationComponent', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({ imports: [UiPaginationComponent] }).compileComponents();
    });

    it('deshabilita Anterior en la primera página y Siguiente en la última', () => {
      const fixture = TestBed.createComponent(UiPaginationComponent);
      fixture.componentRef.setInput('page', 1);
      fixture.componentRef.setInput('pages', 3);
      fixture.detectChanges();

      const [prev, next] = fixture.nativeElement.querySelectorAll('button');
      expect(prev.disabled).toBe(true);
      expect(next.disabled).toBe(false);
    });

    it('emite prev/next y habilita ambos en páginas intermedias', () => {
      const fixture = TestBed.createComponent(UiPaginationComponent);
      fixture.componentRef.setInput('page', 2);
      fixture.componentRef.setInput('pages', 3);
      fixture.detectChanges();

      const [prev, next] = fixture.nativeElement.querySelectorAll('button');
      expect(prev.disabled).toBe(false);
      expect(next.disabled).toBe(false);

      let prevs = 0;
      let nexts = 0;
      fixture.componentInstance.prev.subscribe(() => prevs++);
      fixture.componentInstance.next.subscribe(() => nexts++);
      prev.click();
      next.click();
      expect(prevs).toBe(1);
      expect(nexts).toBe(1);
    });

    it('muestra el indicador "Página X de Y"', () => {
      const fixture = TestBed.createComponent(UiPaginationComponent);
      fixture.componentRef.setInput('page', 2);
      fixture.componentRef.setInput('pages', 5);
      fixture.detectChanges();
      expect(fixture.nativeElement.textContent).toContain('Página 2 de 5');
    });
  });
  describe('UiConfirmService', () => {
    it('abrir deja la petición visible y confirmar resuelve true', async () => {
      const svc = new UiConfirmService();
      const peticion: Confirmacion = { titulo: 'Borrar paciente', mensaje: '¿Continuar?' };

      const promesa = svc.abrir(peticion);
      expect(svc.peticion()).toEqual(peticion);

      svc.confirmado();
      await expect(promesa).resolves.toBe(true);
      expect(svc.peticion()).toBeNull();
    });

    it('cancelar resuelve false', async () => {
      const svc = new UiConfirmService();
      const promesa = svc.abrir({ titulo: 'x', mensaje: 'y' });

      svc.cancelado();
      await expect(promesa).resolves.toBe(false);
      expect(svc.peticion()).toBeNull();
    });

    it('abrir otro diálogo cancela el anterior (no deja promesas colgadas)', async () => {
      const svc = new UiConfirmService();
      const primera = svc.abrir({ titulo: '1', mensaje: '1' });
      const segunda = svc.abrir({ titulo: '2', mensaje: '2' });

      await expect(primera).resolves.toBe(false);
      expect(svc.peticion()?.titulo).toBe('2');

      svc.confirmado();
      await expect(segunda).resolves.toBe(true);
    });
  });

  describe('UiConfirmHostComponent', () => {
    beforeEach(async () => {
      await TestBed.configureTestingModule({ imports: [UiConfirmHostComponent] }).compileComponents();
    });

    const dialogo = (fixture: ComponentFixture<UiConfirmHostComponent>) =>
      fixture.nativeElement.querySelector('[role="dialog"]') as HTMLElement | null;

    it('no monta nada hasta que haya una petición y luego pinta textos y botones', async () => {
      const fixture = TestBed.createComponent(UiConfirmHostComponent);
      fixture.detectChanges();
      expect(dialogo(fixture)).toBeNull();

      const svc = fixture.debugElement.injector.get(UiConfirmService);
      const promesa = svc.abrir({
        titulo: 'Cancelar cita',
        mensaje: '¿Seguro que quieres cancelarla?',
        confirmarTexto: 'Sí, cancelar',
        cancelarTexto: 'No, volver',
      });
      fixture.detectChanges();

      const dlg = dialogo(fixture)!;
      expect(dlg.getAttribute('aria-label')).toBe('Cancelar cita');
      expect(dlg.textContent).toContain('¿Seguro que quieres cancelarla?');
      const textos = Array.from(dlg.querySelectorAll('button')).map(b => b.textContent?.trim());
      expect(textos).toContain('Sí, cancelar');
      expect(textos).toContain('No, volver');

      const confirmar = Array.from(dlg.querySelectorAll('button')).find(
        b => b.textContent?.trim() === 'Sí, cancelar',
      ) as HTMLButtonElement;
      confirmar.click();
      fixture.detectChanges();

      await expect(promesa).resolves.toBe(true);
      expect(dialogo(fixture)).toBeNull();
    });

    it('cancelar desde el botón resuelve false y desmonta el diálogo', async () => {
      const fixture = TestBed.createComponent(UiConfirmHostComponent);
      const svc = fixture.debugElement.injector.get(UiConfirmService);
      const promesa = svc.abrir({ titulo: 'Borrar', mensaje: '¿Borrar?' });
      fixture.detectChanges();

      const cancelar = Array.from(dialogo(fixture)!.querySelectorAll('button')).find(
        b => b.textContent?.trim() === 'Cancelar',
      ) as HTMLButtonElement;
      cancelar.click();
      fixture.detectChanges();

      await expect(promesa).resolves.toBe(false);
      expect(dialogo(fixture)).toBeNull();
    });

    it('Escape cierra el diálogo sin confirmar', async () => {
      const fixture = TestBed.createComponent(UiConfirmHostComponent);
      const svc = fixture.debugElement.injector.get(UiConfirmService);
      const promesa = svc.abrir({ titulo: 'Borrar', mensaje: '¿Borrar?' });
      fixture.detectChanges();
      expect(dialogo(fixture)).not.toBeNull();

      document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
      fixture.detectChanges();

      await expect(promesa).resolves.toBe(false);
      expect(dialogo(fixture)).toBeNull();
    });
  });
});
