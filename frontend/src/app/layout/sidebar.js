import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MENU } from './menu';
import * as i0 from "@angular/core";
const _forTrack0 = ($index, $item) => $item.section;
const _forTrack1 = ($index, $item) => $item.path;
function SidebarComponent_For_5_For_3_Conditional_1_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "span", 5);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const item_r1 = i0.ɵɵnextContext().$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(item_r1.label);
} }
function SidebarComponent_For_5_For_3_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "a", 4);
    i0.ɵɵconditionalCreate(1, SidebarComponent_For_5_For_3_Conditional_1_Template, 2, 1, "span", 5);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const item_r1 = ctx.$implicit;
    const ctx_r1 = i0.ɵɵnextContext(2);
    i0.ɵɵproperty("routerLink", item_r1.path);
    i0.ɵɵattribute("title", ctx_r1.colapsado() ? item_r1.label : null);
    i0.ɵɵadvance();
    i0.ɵɵconditional(!ctx_r1.colapsado() ? 1 : -1);
} }
function SidebarComponent_For_5_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 3);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
    i0.ɵɵrepeaterCreate(2, SidebarComponent_For_5_For_3_Template, 2, 3, "a", 4, _forTrack1);
} if (rf & 2) {
    const sec_r3 = ctx.$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(sec_r3.section);
    i0.ɵɵadvance();
    i0.ɵɵrepeater(sec_r3.items);
} }
export class SidebarComponent {
    colapsado = input(false, /* @ts-ignore */
    ...(ngDevMode ? [{ debugName: "colapsado" }] : /* istanbul ignore next */ []));
    MENU = MENU;
    usuario = '';
    salir() {
        /* el layout se encarga del logout */
    }
    static ɵfac = function SidebarComponent_Factory(__ngFactoryType__) { return new (__ngFactoryType__ || SidebarComponent)(); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: SidebarComponent, selectors: [["app-sidebar"]], inputs: { colapsado: [1, "colapsado"] }, decls: 13, vars: 3, consts: [[1, "sidebar"], [1, "brand"], [1, "btn", "small", 2, "margin-top", "6px", 3, "click"], [1, "section"], ["routerLinkActive", "active", 3, "routerLink"], [1, "txt"]], template: function SidebarComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "aside", 0)(1, "div", 1);
            i0.ɵɵtext(2, "\uD83E\uDDB7 CRM Dental");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(3, "nav");
            i0.ɵɵrepeaterCreate(4, SidebarComponent_For_5_Template, 4, 1, null, null, _forTrack0);
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(6, "footer")(7, "div");
            i0.ɵɵtext(8, "Usuario: ");
            i0.ɵɵelementStart(9, "strong");
            i0.ɵɵtext(10);
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(11, "button", 2);
            i0.ɵɵlistener("click", function SidebarComponent_Template_button_click_11_listener() { return ctx.salir(); });
            i0.ɵɵtext(12, " Cerrar sesi\u00F3n ");
            i0.ɵɵelementEnd()()();
        } if (rf & 2) {
            i0.ɵɵclassProp("collapsed", ctx.colapsado());
            i0.ɵɵadvance(4);
            i0.ɵɵrepeater(ctx.MENU);
            i0.ɵɵadvance(6);
            i0.ɵɵtextInterpolate(ctx.usuario);
        } }, dependencies: [RouterLink, RouterLinkActive], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(SidebarComponent, [{
        type: Component,
        args: [{
                selector: 'app-sidebar',
                template: `
    <aside class="sidebar" [class.collapsed]="colapsado()">
      <div class="brand">🦷 CRM Dental</div>
      <nav>
        @for (sec of MENU; track sec.section) {
          <div class="section">{{ sec.section }}</div>
          @for (item of sec.items; track item.path) {
            <a
              [routerLink]="item.path"
              routerLinkActive="active"
              [attr.title]="colapsado() ? item.label : null"
            >
              @if (!colapsado()) {
                <span class="txt">{{ item.label }}</span>
              }
            </a>
          }
        }
      </nav>
      <footer>
        <div>Usuario: <strong>{{ usuario }}</strong></div>
        <button class="btn small" style="margin-top:6px" (click)="salir()">
          Cerrar sesión
        </button>
      </footer>
    </aside>
  `,
                imports: [RouterLink, RouterLinkActive],
            }]
    }], null, { colapsado: [{ type: i0.Input, args: [{ isSignal: true, alias: "colapsado", required: false }] }] }); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(SidebarComponent, { className: "SidebarComponent", filePath: "src/app/layout/sidebar.ts", lineNumber: 40 }); })();
