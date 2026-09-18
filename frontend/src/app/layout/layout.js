import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { SidebarComponent } from './sidebar';
import { HeaderComponent } from './header';
import { FooterComponent } from './footer';
import { MENU } from './menu';
import * as i0 from "@angular/core";
import * as i1 from "../core/auth.service";
import * as i2 from "@angular/router";
import * as i3 from "@angular/material/sidenav";
export class LayoutComponent {
    auth;
    router;
    colapsado = signal(false, /* @ts-ignore */
    ...(ngDevMode ? [{ debugName: "colapsado" }] : /* istanbul ignore next */ []));
    constructor(auth, router) {
        this.auth = auth;
        this.router = router;
    }
    get titulo() {
        const seg = this.router.url.split('/')[1];
        if (seg === 'citas')
            return 'Citas';
        if (seg === 'dashboard')
            return 'Inicio';
        if (seg === 'pacientes')
            return 'Pacientes';
        if (seg === 'odontologos')
            return 'Odontólogos';
        if (seg === 'servicios')
            return 'Servicios';
        if (seg === 'horarios')
            return 'Horarios';
        if (seg === 'agenda')
            return 'Agenda';
        if (seg === 'whatsapp')
            return 'WhatsApp';
        if (seg === 'google')
            return 'Google Calendar';
        if (seg === 'plantillas')
            return 'Plantillas';
        if (seg === 'automatizaciones')
            return 'Automatizaciones';
        if (seg === 'notificaciones')
            return 'Notificaciones';
        if (seg === 'agente')
            return 'Agente IA';
        if (seg === 'reportes')
            return 'Reportes';
        if (seg === 'auditoria')
            return 'Auditoría';
        if (seg === 'usuarios')
            return 'Usuarios';
        return (MENU.flatMap((s) => s.items).find((i) => i.path === '/' + seg)?.label) ?? 'Inicio';
    }
    get fecha() {
        return new Date().toLocaleDateString('es-EC', {
            weekday: 'long',
            day: 'numeric',
            month: 'long',
            year: 'numeric',
        });
    }
    alternar() {
        this.colapsado.update((c) => !c);
    }
    salir() {
        this.auth.logout();
    }
    static ɵfac = function LayoutComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || LayoutComponent)(i0.ɵɵdirectiveInject(i1.AuthService), i0.ɵɵdirectiveInject(i2.Router)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: LayoutComponent, selectors: [["app-layout"]], decls: 9, vars: 7, consts: [["side", ""], [1, "app-shell"], [1, "side", 3, "opened", "mode", "fixedInViewport"], [3, "colapsado"], [1, "main"], [3, "toggle", "salir", "colapsado", "titulo", "fecha"], [1, "content"]], template: function LayoutComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "mat-sidenav-container", 1)(1, "mat-sidenav", 2, 0);
            i0.ɵɵelement(3, "app-sidebar", 3);
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "mat-sidenav-content", 4)(5, "app-header", 5);
            i0.ɵɵlistener("toggle", function LayoutComponent_Template_app_header_toggle_5_listener() { return ctx.alternar(); })("salir", function LayoutComponent_Template_app_header_salir_5_listener() { return ctx.salir(); });
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(6, "div", 6);
            i0.ɵɵelement(7, "router-outlet");
            i0.ɵɵelementEnd();
            i0.ɵɵelement(8, "app-footer");
            i0.ɵɵelementEnd()();
        } if (rf & 2) {
            i0.ɵɵadvance();
            i0.ɵɵproperty("opened", ctx.colapsado())("mode", "side")("fixedInViewport", true);
            i0.ɵɵadvance(2);
            i0.ɵɵproperty("colapsado", ctx.colapsado());
            i0.ɵɵadvance(2);
            i0.ɵɵproperty("colapsado", ctx.colapsado())("titulo", ctx.titulo)("fecha", ctx.fecha);
        } }, dependencies: [RouterOutlet,
            SidebarComponent,
            HeaderComponent,
            FooterComponent,
            MatSidenavModule, i3.MatSidenav, i3.MatSidenavContainer, i3.MatSidenavContent, MatToolbarModule], styles: ["[_nghost-%COMP%] {\n        display: block;\n        height: 100%;\n      }\n      .app-shell[_ngcontent-%COMP%] {\n        display: flex;\n        height: 100vh;\n        overflow: hidden;\n      }\n      app-sidebar[_ngcontent-%COMP%] {\n        flex: 0 0 auto;\n      }\n      .main[_ngcontent-%COMP%] {\n        flex: 1 1 auto;\n        display: flex;\n        flex-direction: column;\n        min-width: 0;\n        height: 100vh;\n      }\n      .content[_ngcontent-%COMP%] {\n        flex: 1 1 auto;\n        overflow-y: auto;\n        padding: 20px;\n      }"] });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(LayoutComponent, [{
        type: Component,
        args: [{ selector: 'app-layout', template: `
    <mat-sidenav-container class="app-shell">
      <mat-sidenav
        class="side"
        #side
        [opened]="colapsado()"
        [mode]="'side'"
        [fixedInViewport]="true"
      >
        <app-sidebar [colapsado]="colapsado()" />
      </mat-sidenav>
      <mat-sidenav-content class="main">
        <app-header
          [colapsado]="colapsado()"
          [titulo]="titulo"
          [fecha]="fecha"
          (toggle)="alternar()"
          (salir)="salir()"
        />
        <div class="content">
          <router-outlet />
        </div>
        <app-footer />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `, imports: [
                    RouterOutlet,
                    SidebarComponent,
                    HeaderComponent,
                    FooterComponent,
                    MatSidenavModule,
                    MatToolbarModule,
                ], styles: ["\n      :host {\n        display: block;\n        height: 100%;\n      }\n      .app-shell {\n        display: flex;\n        height: 100vh;\n        overflow: hidden;\n      }\n      app-sidebar {\n        flex: 0 0 auto;\n      }\n      .main {\n        flex: 1 1 auto;\n        display: flex;\n        flex-direction: column;\n        min-width: 0;\n        height: 100vh;\n      }\n      .content {\n        flex: 1 1 auto;\n        overflow-y: auto;\n        padding: 20px;\n      }\n    "] }]
    }], () => [{ type: i1.AuthService }, { type: i2.Router }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(LayoutComponent, { className: "LayoutComponent", filePath: "src/app/layout/layout.ts", lineNumber: 76 }); })();
