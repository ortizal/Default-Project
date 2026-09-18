import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
import * as i3 from "@angular/forms";
const _forTrack0 = ($index, $item) => $item.id;
function SesionesComponent_Conditional_6_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 3);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function SesionesComponent_For_25_Conditional_11_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "span", 7);
    i0.ɵɵtext(1, "QR pendiente");
    i0.ɵɵelementEnd();
} }
function SesionesComponent_For_25_Conditional_12_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "span", 8);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const s_r2 = i0.ɵɵnextContext().$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(s_r2.estadoDetalle);
} }
function SesionesComponent_For_25_Conditional_13_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵtext(0, " \u2014 ");
} }
function SesionesComponent_For_25_Conditional_14_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 8);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const s_r2 = i0.ɵɵnextContext().$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(s_r2.lastError);
} }
function SesionesComponent_For_25_Conditional_18_Template(rf, ctx) { if (rf & 1) {
    const _r3 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "button", 12);
    i0.ɵɵlistener("click", function SesionesComponent_For_25_Conditional_18_Template_button_click_0_listener() { i0.ɵɵrestoreView(_r3); const s_r2 = i0.ɵɵnextContext().$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.conectar(s_r2)); });
    i0.ɵɵtext(1, "Conectar");
    i0.ɵɵelementEnd();
} }
function SesionesComponent_For_25_Conditional_19_Template(rf, ctx) { if (rf & 1) {
    const _r4 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "button", 13);
    i0.ɵɵlistener("click", function SesionesComponent_For_25_Conditional_19_Template_button_click_0_listener() { i0.ɵɵrestoreView(_r4); const s_r2 = i0.ɵɵnextContext().$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.desconectar(s_r2)); });
    i0.ɵɵtext(1, "Desconectar");
    i0.ɵɵelementEnd();
} }
function SesionesComponent_For_25_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "td");
    i0.ɵɵtext(4);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(5, "td");
    i0.ɵɵtext(6);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(7, "td")(8, "span", 6);
    i0.ɵɵtext(9);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(10, "td");
    i0.ɵɵconditionalCreate(11, SesionesComponent_For_25_Conditional_11_Template, 2, 0, "span", 7)(12, SesionesComponent_For_25_Conditional_12_Template, 2, 1, "span", 8)(13, SesionesComponent_For_25_Conditional_13_Template, 1, 0);
    i0.ɵɵconditionalCreate(14, SesionesComponent_For_25_Conditional_14_Template, 2, 1, "div", 8);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(15, "td");
    i0.ɵɵtext(16);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(17, "td", 9);
    i0.ɵɵconditionalCreate(18, SesionesComponent_For_25_Conditional_18_Template, 2, 0, "button", 10);
    i0.ɵɵconditionalCreate(19, SesionesComponent_For_25_Conditional_19_Template, 2, 0, "button", 11);
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const s_r2 = ctx.$implicit;
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(s_r2.id);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(s_r2.sesionId);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(s_r2.nombre || "-");
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", ctx_r0.badge(s_r2.estado));
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(s_r2.estado);
    i0.ɵɵadvance(2);
    i0.ɵɵconditional(s_r2.qr ? 11 : s_r2.estadoDetalle ? 12 : 13);
    i0.ɵɵadvance(3);
    i0.ɵɵconditional(s_r2.lastError ? 14 : -1);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(s_r2.updatedAt || "-");
    i0.ɵɵadvance(2);
    i0.ɵɵconditional(ctx_r0.conectable(s_r2.estado) ? 18 : -1);
    i0.ɵɵadvance();
    i0.ɵɵconditional(ctx_r0.desconectable(s_r2.estado) ? 19 : -1);
} }
function SesionesComponent_ForEmpty_26_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 14);
    i0.ɵɵtext(2, "Sin sesiones configuradas.");
    i0.ɵɵelementEnd()();
} }
function SesionesComponent_Conditional_27_Template(rf, ctx) { if (rf & 1) {
    const _r5 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 15);
    i0.ɵɵlistener("click", function SesionesComponent_Conditional_27_Template_div_click_0_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.close($event)); });
    i0.ɵɵelementStart(1, "div", 16);
    i0.ɵɵlistener("click", function SesionesComponent_Conditional_27_Template_div_click_1_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.stop($event)); });
    i0.ɵɵelementStart(2, "h2");
    i0.ɵɵtext(3, "Nueva sesi\u00F3n");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "label", 17);
    i0.ɵɵtext(5, "Identificador de sesi\u00F3n *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(6, "input", 18);
    i0.ɵɵtwoWayListener("ngModelChange", function SesionesComponent_Conditional_27_Template_input_ngModelChange_6_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.sesionId, $event) || (ctx_r0.sesionId = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementStart(7, "label", 17);
    i0.ɵɵtext(8, "Nombre");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(9, "input", 19);
    i0.ɵɵtwoWayListener("ngModelChange", function SesionesComponent_Conditional_27_Template_input_ngModelChange_9_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.nombre, $event) || (ctx_r0.nombre = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementStart(10, "div", 20)(11, "button", 21);
    i0.ɵɵlistener("click", function SesionesComponent_Conditional_27_Template_button_click_11_listener() { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.showForm = false); });
    i0.ɵɵtext(12, "Cancelar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(13, "button", 2);
    i0.ɵɵlistener("click", function SesionesComponent_Conditional_27_Template_button_click_13_listener() { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.crear()); });
    i0.ɵɵtext(14, "Crear");
    i0.ɵɵelementEnd()()()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(6);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.sesionId);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(3);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.nombre);
    i0.ɵɵcontrol();
} }
export class SesionesComponent {
    api;
    items = [];
    error = '';
    showForm = false;
    sesionId = '';
    nombre = '';
    constructor(api) {
        this.api = api;
        this.cargar();
    }
    cargar() {
        this.api.get('/whatsapp/sesiones').subscribe({
            next: (r) => (this.items = r),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    nueva() {
        this.sesionId = '';
        this.nombre = '';
        this.showForm = true;
    }
    crear() {
        if (!this.sesionId.trim())
            return;
        this.api.post('/whatsapp/sesiones', { sesionId: this.sesionId.trim(), nombre: this.nombre.trim() || null }).subscribe({
            next: () => {
                this.showForm = false;
                this.cargar();
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    conectar(s) {
        this.api.post(`/whatsapp/sesiones/${s.id}/conectar`, {}).subscribe({
            next: () => this.cargar(),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    desconectar(s) {
        this.api.post(`/whatsapp/sesiones/${s.id}/desconectar`, {}).subscribe({
            next: () => this.cargar(),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    conectable(estado) {
        return estado === 'DESCONECTADA' || estado === 'ERROR' || estado === 'NUEVA';
    }
    desconectable(estado) {
        return estado === 'CONECTADA' || estado === 'CONECTANDO';
    }
    badge(estado) {
        switch (estado) {
            case 'CONECTADA':
                return 'ok';
            case 'CONECTANDO':
                return 'info';
            case 'ERROR':
                return 'bad';
            case 'PENDIENTE_QR':
                return 'warn';
            default:
                return 'dim';
        }
    }
    close(e) {
        if (e.target === e.currentTarget)
            this.showForm = false;
    }
    stop(e) {
        e.stopPropagation();
    }
    msg(e) {
        const a = e;
        return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
    }
    static ɵfac = function SesionesComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || SesionesComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: SesionesComponent, selectors: [["app-sesiones"]], decls: 28, vars: 3, consts: [[1, "card"], [1, "between"], [1, "btn", "primary", 3, "click"], [1, "msg", "error"], [1, "tbl"], [1, "modal-backdrop"], [1, "badge", 3, "ngClass"], [1, "badge", "info"], [1, "muted"], [1, "flex"], [1, "btn", "small"], [1, "btn", "small", "danger"], [1, "btn", "small", 3, "click"], [1, "btn", "small", "danger", 3, "click"], ["colspan", "7", 1, "empty"], [1, "modal-backdrop", 3, "click"], [1, "modal", 3, "click"], [1, "label"], ["placeholder", "ej. principal", 1, "input", 3, "ngModelChange", "ngModel"], ["placeholder", "ej. WhatsApp recepci\u00F3n", 1, "input", 3, "ngModelChange", "ngModel"], [1, "modal-actions"], [1, "btn", 3, "click"]], template: function SesionesComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Sesiones de WhatsApp");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "button", 2);
            i0.ɵɵlistener("click", function SesionesComponent_Template_button_click_4_listener() { return ctx.nueva(); });
            i0.ɵɵtext(5, "+ Nueva sesi\u00F3n");
            i0.ɵɵelementEnd()();
            i0.ɵɵconditionalCreate(6, SesionesComponent_Conditional_6_Template, 2, 1, "div", 3);
            i0.ɵɵelementStart(7, "table", 4)(8, "thead")(9, "tr")(10, "th");
            i0.ɵɵtext(11, "ID");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(12, "th");
            i0.ɵɵtext(13, "Sesi\u00F3n");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(14, "th");
            i0.ɵɵtext(15, "Nombre");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(16, "th");
            i0.ɵɵtext(17, "Estado");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(18, "th");
            i0.ɵɵtext(19, "Detalle / QR");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(20, "th");
            i0.ɵɵtext(21, "Actualizado");
            i0.ɵɵelementEnd();
            i0.ɵɵelement(22, "th");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(23, "tbody");
            i0.ɵɵrepeaterCreate(24, SesionesComponent_For_25_Template, 20, 10, "tr", null, _forTrack0, false, SesionesComponent_ForEmpty_26_Template, 3, 0, "tr");
            i0.ɵɵelementEnd()()();
            i0.ɵɵconditionalCreate(27, SesionesComponent_Conditional_27_Template, 15, 2, "div", 5);
        } if (rf & 2) {
            i0.ɵɵadvance(6);
            i0.ɵɵconditional(ctx.error ? 6 : -1);
            i0.ɵɵadvance(18);
            i0.ɵɵrepeater(ctx.items);
            i0.ɵɵadvance(3);
            i0.ɵɵconditional(ctx.showForm ? 27 : -1);
        } }, dependencies: [CommonModule, i2.NgClass, FormsModule, i3.DefaultValueAccessor, i3.NgControlStatus, i3.NgModel], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(SesionesComponent, [{
        type: Component,
        args: [{
                selector: 'app-sesiones',
                template: `
    <div class="card">
      <div class="between">
        <h2>Sesiones de WhatsApp</h2>
        <button class="btn primary" (click)="nueva()">+ Nueva sesión</button>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead><tr><th>ID</th><th>Sesión</th><th>Nombre</th><th>Estado</th><th>Detalle / QR</th><th>Actualizado</th><th></th></tr></thead>
        <tbody>
          @for (s of items; track s.id) {
            <tr>
              <td>{{ s.id }}</td>
              <td>{{ s.sesionId }}</td>
              <td>{{ s.nombre || '-' }}</td>
              <td><span class="badge" [ngClass]="badge(s.estado)">{{ s.estado }}</span></td>
              <td>
                @if (s.qr) {
                  <span class="badge info">QR pendiente</span>
                } @else if (s.estadoDetalle) {
                  <span class="muted">{{ s.estadoDetalle }}</span>
                } @else {
                  —
                }
                @if (s.lastError) {
                  <div class="muted">{{ s.lastError }}</div>
                }
              </td>
              <td>{{ s.updatedAt || '-' }}</td>
              <td class="flex">
                @if (conectable(s.estado)) {
                  <button class="btn small" (click)="conectar(s)">Conectar</button>
                }
                @if (desconectable(s.estado)) {
                  <button class="btn small danger" (click)="desconectar(s)">Desconectar</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="empty">Sin sesiones configuradas.</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (showForm) {
      <div class="modal-backdrop" (click)="close($event)">
        <div class="modal" (click)="stop($event)">
          <h2>Nueva sesión</h2>
          <label class="label">Identificador de sesión *</label>
          <input class="input" [(ngModel)]="sesionId" placeholder="ej. principal" />
          <label class="label">Nombre</label>
          <input class="input" [(ngModel)]="nombre" placeholder="ej. WhatsApp recepción" />
          <div class="modal-actions">
            <button class="btn" (click)="showForm = false">Cancelar</button>
            <button class="btn primary" (click)="crear()">Crear</button>
          </div>
        </div>
      </div>
    }
  `,
                imports: [CommonModule, FormsModule],
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(SesionesComponent, { className: "SesionesComponent", filePath: "src/app/whatsapp/sesiones.ts", lineNumber: 74 }); })();
