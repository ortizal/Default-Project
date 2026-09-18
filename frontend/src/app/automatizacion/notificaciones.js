import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
const _forTrack0 = ($index, $item) => $item.id;
function NotificacionesComponent_Conditional_6_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 3);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function NotificacionesComponent_For_30_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "td");
    i0.ɵɵtext(4);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(5, "td");
    i0.ɵɵtext(6);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(7, "td")(8, "span", 5);
    i0.ɵɵtext(9);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(10, "td");
    i0.ɵɵtext(11);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(12, "td");
    i0.ɵɵtext(13);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(14, "td");
    i0.ɵɵtext(15);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(16, "td")(17, "span", 6);
    i0.ɵɵtext(18);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(19, "td");
    i0.ɵɵtext(20);
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const n_r2 = ctx.$implicit;
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(n_r2.id);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(n_r2.pacienteId);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(n_r2.telefono);
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(n_r2.evento);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(n_r2.plantilla);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(n_r2.programadaAt || "-");
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(n_r2.enviadaAt || "-");
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", ctx_r0.badge(n_r2.estado));
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(n_r2.estado);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(n_r2.error || (n_r2.intentos ? "Intentos: " + n_r2.intentos : "-"));
} }
function NotificacionesComponent_ForEmpty_31_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 7);
    i0.ɵɵtext(2, "Sin notificaciones.");
    i0.ɵɵelementEnd()();
} }
export class NotificacionesComponent {
    api;
    items = [];
    error = '';
    constructor(api) {
        this.api = api;
        this.cargar();
    }
    cargar() {
        this.api.get('/automatizaciones/notificaciones?limite=100').subscribe({
            next: (r) => (this.items = r),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    badge(estado) {
        switch (estado) {
            case 'PENDIENTE':
                return 'warn';
            case 'ENVIADA':
                return 'ok';
            case 'ERROR':
                return 'bad';
            case 'CANCELADA':
                return 'dim';
            default:
                return 'dim';
        }
    }
    msg(e) {
        const a = e;
        return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
    }
    static ɵfac = function NotificacionesComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || NotificacionesComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: NotificacionesComponent, selectors: [["app-notificaciones"]], decls: 32, vars: 2, consts: [[1, "card"], [1, "between"], [1, "btn", 3, "click"], [1, "msg", "error"], [1, "tbl"], [1, "badge", "info"], [1, "badge", 3, "ngClass"], ["colspan", "9", 1, "empty"]], template: function NotificacionesComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Notificaciones programadas");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "button", 2);
            i0.ɵɵlistener("click", function NotificacionesComponent_Template_button_click_4_listener() { return ctx.cargar(); });
            i0.ɵɵtext(5, "\u27F3 Refrescar");
            i0.ɵɵelementEnd()();
            i0.ɵɵconditionalCreate(6, NotificacionesComponent_Conditional_6_Template, 2, 1, "div", 3);
            i0.ɵɵelementStart(7, "table", 4)(8, "thead")(9, "tr")(10, "th");
            i0.ɵɵtext(11, "ID");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(12, "th");
            i0.ɵɵtext(13, "Paciente");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(14, "th");
            i0.ɵɵtext(15, "Tel\u00E9fono");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(16, "th");
            i0.ɵɵtext(17, "Evento");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(18, "th");
            i0.ɵɵtext(19, "Plantilla");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(20, "th");
            i0.ɵɵtext(21, "Programada");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(22, "th");
            i0.ɵɵtext(23, "Enviada");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(24, "th");
            i0.ɵɵtext(25, "Estado");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(26, "th");
            i0.ɵɵtext(27, "Errores");
            i0.ɵɵelementEnd()()();
            i0.ɵɵelementStart(28, "tbody");
            i0.ɵɵrepeaterCreate(29, NotificacionesComponent_For_30_Template, 21, 10, "tr", null, _forTrack0, false, NotificacionesComponent_ForEmpty_31_Template, 3, 0, "tr");
            i0.ɵɵelementEnd()()();
        } if (rf & 2) {
            i0.ɵɵadvance(6);
            i0.ɵɵconditional(ctx.error ? 6 : -1);
            i0.ɵɵadvance(23);
            i0.ɵɵrepeater(ctx.items);
        } }, dependencies: [CommonModule, i2.NgClass], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(NotificacionesComponent, [{
        type: Component,
        args: [{
                selector: 'app-notificaciones',
                template: `
    <div class="card">
      <div class="between">
        <h2>Notificaciones programadas</h2>
        <button class="btn" (click)="cargar()">⟳ Refrescar</button>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead><tr><th>ID</th><th>Paciente</th><th>Teléfono</th><th>Evento</th><th>Plantilla</th><th>Programada</th><th>Enviada</th><th>Estado</th><th>Errores</th></tr></thead>
        <tbody>
          @for (n of items; track n.id) {
            <tr>
              <td>{{ n.id }}</td>
              <td>{{ n.pacienteId }}</td>
              <td>{{ n.telefono }}</td>
              <td><span class="badge info">{{ n.evento }}</span></td>
              <td>{{ n.plantilla }}</td>
              <td>{{ n.programadaAt || '-' }}</td>
              <td>{{ n.enviadaAt || '-' }}</td>
              <td><span class="badge" [ngClass]="badge(n.estado)">{{ n.estado }}</span></td>
              <td>{{ n.error || (n.intentos ? 'Intentos: ' + n.intentos : '-') }}</td>
            </tr>
          } @empty {
            <tr><td colspan="9" class="empty">Sin notificaciones.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
                imports: [CommonModule],
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(NotificacionesComponent, { className: "NotificacionesComponent", filePath: "src/app/automatizacion/notificaciones.ts", lineNumber: 41 }); })();
