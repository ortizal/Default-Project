import { Component } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import * as i0 from "@angular/core";
import * as i1 from "@angular/material/toolbar";
export class FooterComponent {
    anio = new Date().getFullYear();
    static ɵfac = function FooterComponent_Factory(__ngFactoryType__) { return new (__ngFactoryType__ || FooterComponent)(); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: FooterComponent, selectors: [["app-footer"]], decls: 6, vars: 1, consts: [[1, "app-footer", "mat-elevation-z2"], [1, "spacer"], [1, "mat-body-small", "muted"]], template: function FooterComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "mat-toolbar", 0)(1, "span");
            i0.ɵɵtext(2);
            i0.ɵɵelementEnd();
            i0.ɵɵelement(3, "span", 1);
            i0.ɵɵelementStart(4, "span", 2);
            i0.ɵɵtext(5, "v1.0");
            i0.ɵɵelementEnd()();
        } if (rf & 2) {
            i0.ɵɵadvance(2);
            i0.ɵɵtextInterpolate1("\u00A9 ", ctx.anio, " CRM Dental");
        } }, dependencies: [MatToolbarModule, i1.MatToolbar], styles: [".spacer[_ngcontent-%COMP%] {\n        flex: 1 1 auto;\n      }"] });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(FooterComponent, [{
        type: Component,
        args: [{ selector: 'app-footer', template: `
    <mat-toolbar class="app-footer mat-elevation-z2">
      <span>© {{ anio }} CRM Dental</span>
      <span class="spacer"></span>
      <span class="mat-body-small muted">v1.0</span>
    </mat-toolbar>
  `, imports: [MatToolbarModule], styles: ["\n      .spacer {\n        flex: 1 1 auto;\n      }\n    "] }]
    }], null, null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(FooterComponent, { className: "FooterComponent", filePath: "src/app/layout/footer.ts", lineNumber: 22 }); })();
