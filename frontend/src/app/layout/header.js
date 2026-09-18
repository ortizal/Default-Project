import { Component, input, output } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import * as i0 from "@angular/core";
import * as i1 from "@angular/material/toolbar";
import * as i2 from "@angular/material/icon";
import * as i3 from "@angular/material/button";
export class HeaderComponent {
    colapsado = input(false, /* @ts-ignore */
    ...(ngDevMode ? [{ debugName: "colapsado" }] : /* istanbul ignore next */ []));
    titulo = input('', /* @ts-ignore */
    ...(ngDevMode ? [{ debugName: "titulo" }] : /* istanbul ignore next */ []));
    fecha = input('', /* @ts-ignore */
    ...(ngDevMode ? [{ debugName: "fecha" }] : /* istanbul ignore next */ []));
    toggle = output();
    salir = output();
    static ɵfac = function HeaderComponent_Factory(__ngFactoryType__) { return new (__ngFactoryType__ || HeaderComponent)(); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: HeaderComponent, selectors: [["app-header"]], inputs: { colapsado: [1, "colapsado"], titulo: [1, "titulo"], fecha: [1, "fecha"] }, outputs: { toggle: "toggle", salir: "salir" }, decls: 14, vars: 5, consts: [["color", "primary", 1, "topbar"], ["mat-icon-button", "", 3, "click"], [1, "mat-title-medium", "title"], [1, "spacer"], [1, "mat-body-medium", "muted"], ["mat-flat-button", "", "color", "accent", 3, "click"], [1, "mat-label-large"]], template: function HeaderComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "mat-toolbar", 0)(1, "button", 1);
            i0.ɵɵlistener("click", function HeaderComponent_Template_button_click_1_listener() { return ctx.toggle.emit(); });
            i0.ɵɵelementStart(2, "mat-icon");
            i0.ɵɵtext(3);
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(4, "h1", 2);
            i0.ɵɵtext(5);
            i0.ɵɵelementEnd();
            i0.ɵɵelement(6, "span", 3);
            i0.ɵɵelementStart(7, "span", 4);
            i0.ɵɵtext(8);
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(9, "button", 5);
            i0.ɵɵlistener("click", function HeaderComponent_Template_button_click_9_listener() { return ctx.salir.emit(); });
            i0.ɵɵelementStart(10, "mat-icon");
            i0.ɵɵtext(11, "logout");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(12, "span", 6);
            i0.ɵɵtext(13, "Cerrar sesi\u00F3n");
            i0.ɵɵelementEnd()()();
        } if (rf & 2) {
            i0.ɵɵadvance();
            i0.ɵɵattribute("aria-label", ctx.colapsado() ? "Expandir men\u00FA" : "Colapsar men\u00FA")("title", ctx.colapsado() ? "Expandir men\u00FA" : "Colapsar men\u00FA");
            i0.ɵɵadvance(2);
            i0.ɵɵtextInterpolate(ctx.colapsado() ? "menu_open" : "menu");
            i0.ɵɵadvance(2);
            i0.ɵɵtextInterpolate(ctx.titulo());
            i0.ɵɵadvance(3);
            i0.ɵɵtextInterpolate(ctx.fecha());
        } }, dependencies: [MatToolbarModule, i1.MatToolbar, MatIconModule, i2.MatIcon, MatButtonModule, i3.MatButton, i3.MatIconButton], styles: [".topbar[_ngcontent-%COMP%] {\n        flex: 0 0 auto;\n        display: flex;\n        align-items: center;\n        gap: 12px;\n      }\n      .title[_ngcontent-%COMP%] {\n        margin: 0;\n        white-space: nowrap;\n      }\n      .spacer[_ngcontent-%COMP%] {\n        flex: 1 1 auto;\n      }\n      .muted[_ngcontent-%COMP%] {\n        white-space: nowrap;\n      }"] });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(HeaderComponent, [{
        type: Component,
        args: [{ selector: 'app-header', template: `
    <mat-toolbar color="primary" class="topbar">
      <button
        mat-icon-button
        [attr.aria-label]="colapsado() ? 'Expandir menú' : 'Colapsar menú'"
        [attr.title]="colapsado() ? 'Expandir menú' : 'Colapsar menú'"
        (click)="toggle.emit()"
      >
        <mat-icon>{{ colapsado() ? 'menu_open' : 'menu' }}</mat-icon>
      </button>
      <h1 class="mat-title-medium title">{{ titulo() }}</h1>
      <span class="spacer"></span>
      <span class="mat-body-medium muted">{{ fecha() }}</span>
      <button mat-flat-button color="accent" (click)="salir.emit()">
        <mat-icon>logout</mat-icon>
        <span class="mat-label-large">Cerrar sesión</span>
      </button>
    </mat-toolbar>
  `, imports: [MatToolbarModule, MatIconModule, MatButtonModule], styles: ["\n      .topbar {\n        flex: 0 0 auto;\n        display: flex;\n        align-items: center;\n        gap: 12px;\n      }\n      .title {\n        margin: 0;\n        white-space: nowrap;\n      }\n      .spacer {\n        flex: 1 1 auto;\n      }\n      .muted {\n        white-space: nowrap;\n      }\n    "] }]
    }], null, { colapsado: [{ type: i0.Input, args: [{ isSignal: true, alias: "colapsado", required: false }] }], titulo: [{ type: i0.Input, args: [{ isSignal: true, alias: "titulo", required: false }] }], fecha: [{ type: i0.Input, args: [{ isSignal: true, alias: "fecha", required: false }] }], toggle: [{ type: i0.Output, args: ["toggle"] }], salir: [{ type: i0.Output, args: ["salir"] }] }); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(HeaderComponent, { className: "HeaderComponent", filePath: "src/app/layout/header.ts", lineNumber: 49 }); })();
