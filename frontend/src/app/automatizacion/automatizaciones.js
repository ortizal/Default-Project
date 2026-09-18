import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
import * as i3 from "@angular/forms";
const _forTrack0 = ($index, $item) => $item.id;
function AutomatizacionesComponent_Conditional_6_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 3);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function AutomatizacionesComponent_For_27_Template(rf, ctx) { if (rf & 1) {
    const _r2 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "td");
    i0.ɵɵtext(4);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(5, "td")(6, "span", 6);
    i0.ɵɵtext(7);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(8, "td");
    i0.ɵɵtext(9);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(10, "td");
    i0.ɵɵtext(11);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(12, "td");
    i0.ɵɵtext(13);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(14, "td")(15, "button", 7);
    i0.ɵɵlistener("click", function AutomatizacionesComponent_For_27_Template_button_click_15_listener() { const a_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.toggle(a_r3)); });
    i0.ɵɵtext(16);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(17, "td", 8)(18, "button", 9);
    i0.ɵɵlistener("click", function AutomatizacionesComponent_For_27_Template_button_click_18_listener() { const a_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.editar(a_r3)); });
    i0.ɵɵtext(19, "Editar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(20, "button", 10);
    i0.ɵɵlistener("click", function AutomatizacionesComponent_For_27_Template_button_click_20_listener() { const a_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.eliminar(a_r3)); });
    i0.ɵɵtext(21, "Eliminar");
    i0.ɵɵelementEnd()()();
} if (rf & 2) {
    const a_r3 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(a_r3.id);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(a_r3.nombre);
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(a_r3.evento);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(a_r3.minutosAntes);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(a_r3.plantillaNombre);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(a_r3.condicion || "-");
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", a_r3.activa ? "primary" : "");
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate1(" ", a_r3.activa ? "Activada" : "Desactivada", " ");
} }
function AutomatizacionesComponent_ForEmpty_28_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 11);
    i0.ɵɵtext(2, "Sin automatizaciones.");
    i0.ɵɵelementEnd()();
} }
function AutomatizacionesComponent_Conditional_29_For_11_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "option", 17);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ev_r5 = ctx.$implicit;
    i0.ɵɵproperty("value", ev_r5);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ev_r5);
} }
function AutomatizacionesComponent_Conditional_29_For_24_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "option", 17);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const p_r6 = ctx.$implicit;
    i0.ɵɵproperty("value", p_r6.id);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(p_r6.nombre);
} }
function AutomatizacionesComponent_Conditional_29_Template(rf, ctx) { if (rf & 1) {
    const _r4 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 12);
    i0.ɵɵlistener("click", function AutomatizacionesComponent_Conditional_29_Template_div_click_0_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.close($event)); });
    i0.ɵɵelementStart(1, "div", 13);
    i0.ɵɵlistener("click", function AutomatizacionesComponent_Conditional_29_Template_div_click_1_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.stop($event)); });
    i0.ɵɵelementStart(2, "h2");
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "label", 14);
    i0.ɵɵtext(5, "Nombre *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(6, "input", 15);
    i0.ɵɵtwoWayListener("ngModelChange", function AutomatizacionesComponent_Conditional_29_Template_input_ngModelChange_6_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.nombre, $event) || (ctx_r0.form.nombre = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementStart(7, "label", 14);
    i0.ɵɵtext(8, "Evento *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(9, "select", 16);
    i0.ɵɵtwoWayListener("ngModelChange", function AutomatizacionesComponent_Conditional_29_Template_select_ngModelChange_9_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.evento, $event) || (ctx_r0.form.evento = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵrepeaterCreate(10, AutomatizacionesComponent_Conditional_29_For_11_Template, 2, 2, "option", 17, i0.ɵɵrepeaterTrackByIdentity);
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementStart(12, "div", 18)(13, "div")(14, "label", 14);
    i0.ɵɵtext(15, "Minutos antes *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(16, "input", 19);
    i0.ɵɵtwoWayListener("ngModelChange", function AutomatizacionesComponent_Conditional_29_Template_input_ngModelChange_16_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.minutosAntes, $event) || (ctx_r0.form.minutosAntes = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(17, "div")(18, "label", 14);
    i0.ɵɵtext(19, "Plantilla *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(20, "select", 16);
    i0.ɵɵtwoWayListener("ngModelChange", function AutomatizacionesComponent_Conditional_29_Template_select_ngModelChange_20_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.plantillaId, $event) || (ctx_r0.form.plantillaId = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementStart(21, "option", 20);
    i0.ɵɵtext(22, "\u2014");
    i0.ɵɵelementEnd();
    i0.ɵɵrepeaterCreate(23, AutomatizacionesComponent_Conditional_29_For_24_Template, 2, 2, "option", 17, _forTrack0);
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(25, "label", 14);
    i0.ɵɵtext(26, "Condici\u00F3n");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(27, "input", 21);
    i0.ɵɵtwoWayListener("ngModelChange", function AutomatizacionesComponent_Conditional_29_Template_input_ngModelChange_27_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.condicion, $event) || (ctx_r0.form.condicion = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementStart(28, "label", 22)(29, "input", 23);
    i0.ɵɵtwoWayListener("ngModelChange", function AutomatizacionesComponent_Conditional_29_Template_input_ngModelChange_29_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.activa, $event) || (ctx_r0.form.activa = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵtext(30, " Activa ");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(31, "div", 24)(32, "button", 25);
    i0.ɵɵlistener("click", function AutomatizacionesComponent_Conditional_29_Template_button_click_32_listener() { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.showForm = false); });
    i0.ɵɵtext(33, "Cancelar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(34, "button", 2);
    i0.ɵɵlistener("click", function AutomatizacionesComponent_Conditional_29_Template_button_click_34_listener() { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.guardar()); });
    i0.ɵɵtext(35, "Guardar");
    i0.ɵɵelementEnd()()()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(ctx_r0.form.id ? "Editar automatizaci\u00F3n" : "Nueva automatizaci\u00F3n");
    i0.ɵɵadvance(3);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.nombre);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(3);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.evento);
    i0.ɵɵcontrol();
    i0.ɵɵadvance();
    i0.ɵɵrepeater(ctx_r0.EVENTOS);
    i0.ɵɵadvance(6);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.minutosAntes);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.plantillaId);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(3);
    i0.ɵɵrepeater(ctx_r0.plantillas);
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.condicion);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(2);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.activa);
    i0.ɵɵcontrol();
} }
const EVENTOS = ['CITA_CREADA', 'CITA_PROXIMA', 'CITA_CONFIRMADA', 'CITA_CANCELADA', 'CITA_ATENDIDA', 'NO_ASISTIO'];
export class AutomatizacionesComponent {
    api;
    items = [];
    plantillas = [];
    EVENTOS = EVENTOS;
    error = '';
    showForm = false;
    form = {};
    constructor(api) {
        this.api = api;
        this.cargar();
        this.api.get('/plantillas').subscribe((r) => (this.plantillas = r));
    }
    cargar() {
        this.api.get('/automatizaciones').subscribe({
            next: (r) => (this.items = r),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    nueva() {
        this.form = { nombre: '', evento: EVENTOS[0], minutosAntes: 0, plantillaId: undefined, condicion: '', activa: true };
        this.showForm = true;
    }
    editar(a) {
        this.form = { ...a };
        this.showForm = true;
    }
    toggle(a) {
        const r = a.activa
            ? this.api.post(`/automatizaciones/${a.id}/desactivar`, {})
            : this.api.post(`/automatizaciones/${a.id}/activar`, {});
        r.subscribe({
            next: () => this.cargar(),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    guardar() {
        if (!this.form.nombre || !this.form.evento || !this.form.plantillaId)
            return;
        const body = {
            nombre: this.form.nombre,
            evento: this.form.evento,
            minutosAntes: this.form.minutosAntes ?? 0,
            plantillaId: Number(this.form.plantillaId),
            condicion: this.form.condicion || null,
            activa: this.form.activa ?? true,
        };
        const req = this.form.id
            ? this.api.put(`/automatizaciones/${this.form.id}`, body)
            : this.api.post('/automatizaciones', body);
        req.subscribe({
            next: () => {
                this.showForm = false;
                this.cargar();
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    eliminar(a) {
        if (!confirm('¿Eliminar esta automatización?'))
            return;
        this.api.del(`/automatizaciones/${a.id}`).subscribe({
            next: () => this.cargar(),
            error: (e) => (this.error = this.msg(e)),
        });
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
    static ɵfac = function AutomatizacionesComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || AutomatizacionesComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: AutomatizacionesComponent, selectors: [["app-automatizaciones"]], decls: 30, vars: 3, consts: [[1, "card"], [1, "between"], [1, "btn", "primary", 3, "click"], [1, "msg", "error"], [1, "tbl"], [1, "modal-backdrop"], [1, "badge", "info"], [1, "btn", "small", 3, "click", "ngClass"], [1, "flex"], [1, "btn", "small", 3, "click"], [1, "btn", "small", "danger", 3, "click"], ["colspan", "8", 1, "empty"], [1, "modal-backdrop", 3, "click"], [1, "modal", 3, "click"], [1, "label"], ["required", "", 1, "input", 3, "ngModelChange", "ngModel"], [1, "input", 3, "ngModelChange", "ngModel"], [3, "value"], [1, "form-row"], ["type", "number", "min", "0", 1, "input", 3, "ngModelChange", "ngModel"], ["value", ""], ["placeholder", "ej. estado:cita", 1, "input", 3, "ngModelChange", "ngModel"], [1, "label", 2, "display", "flex", "align-items", "center", "gap", "6px"], ["type", "checkbox", 3, "ngModelChange", "ngModel"], [1, "modal-actions"], [1, "btn", 3, "click"]], template: function AutomatizacionesComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Automatizaciones de mensajes");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "button", 2);
            i0.ɵɵlistener("click", function AutomatizacionesComponent_Template_button_click_4_listener() { return ctx.nueva(); });
            i0.ɵɵtext(5, "+ Nueva automatizaci\u00F3n");
            i0.ɵɵelementEnd()();
            i0.ɵɵconditionalCreate(6, AutomatizacionesComponent_Conditional_6_Template, 2, 1, "div", 3);
            i0.ɵɵelementStart(7, "table", 4)(8, "thead")(9, "tr")(10, "th");
            i0.ɵɵtext(11, "ID");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(12, "th");
            i0.ɵɵtext(13, "Nombre");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(14, "th");
            i0.ɵɵtext(15, "Evento");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(16, "th");
            i0.ɵɵtext(17, "Minutos antes");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(18, "th");
            i0.ɵɵtext(19, "Plantilla");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(20, "th");
            i0.ɵɵtext(21, "Condici\u00F3n");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(22, "th");
            i0.ɵɵtext(23, "Activa");
            i0.ɵɵelementEnd();
            i0.ɵɵelement(24, "th");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(25, "tbody");
            i0.ɵɵrepeaterCreate(26, AutomatizacionesComponent_For_27_Template, 22, 8, "tr", null, _forTrack0, false, AutomatizacionesComponent_ForEmpty_28_Template, 3, 0, "tr");
            i0.ɵɵelementEnd()()();
            i0.ɵɵconditionalCreate(29, AutomatizacionesComponent_Conditional_29_Template, 36, 7, "div", 5);
        } if (rf & 2) {
            i0.ɵɵadvance(6);
            i0.ɵɵconditional(ctx.error ? 6 : -1);
            i0.ɵɵadvance(20);
            i0.ɵɵrepeater(ctx.items);
            i0.ɵɵadvance(3);
            i0.ɵɵconditional(ctx.showForm ? 29 : -1);
        } }, dependencies: [CommonModule, i2.NgClass, FormsModule, i3.NgSelectOption, i3.ɵNgSelectMultipleOption, i3.DefaultValueAccessor, i3.NumberValueAccessor, i3.CheckboxControlValueAccessor, i3.SelectControlValueAccessor, i3.NgControlStatus, i3.RequiredValidator, i3.MinValidator, i3.NgModel], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(AutomatizacionesComponent, [{
        type: Component,
        args: [{
                selector: 'app-automatizaciones',
                template: `
    <div class="card">
      <div class="between">
        <h2>Automatizaciones de mensajes</h2>
        <button class="btn primary" (click)="nueva()">+ Nueva automatización</button>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead><tr><th>ID</th><th>Nombre</th><th>Evento</th><th>Minutos antes</th><th>Plantilla</th><th>Condición</th><th>Activa</th><th></th></tr></thead>
        <tbody>
          @for (a of items; track a.id) {
            <tr>
              <td>{{ a.id }}</td>
              <td>{{ a.nombre }}</td>
              <td><span class="badge info">{{ a.evento }}</span></td>
              <td>{{ a.minutosAntes }}</td>
              <td>{{ a.plantillaNombre }}</td>
              <td>{{ a.condicion || '-' }}</td>
              <td>
                <button class="btn small" [ngClass]="a.activa ? 'primary' : ''" (click)="toggle(a)">
                  {{ a.activa ? 'Activada' : 'Desactivada' }}
                </button>
              </td>
              <td class="flex">
                <button class="btn small" (click)="editar(a)">Editar</button>
                <button class="btn small danger" (click)="eliminar(a)">Eliminar</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="8" class="empty">Sin automatizaciones.</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (showForm) {
      <div class="modal-backdrop" (click)="close($event)">
        <div class="modal" (click)="stop($event)">
          <h2>{{ form.id ? 'Editar automatización' : 'Nueva automatización' }}</h2>
          <label class="label">Nombre *</label>
          <input class="input" [(ngModel)]="form.nombre" required />
          <label class="label">Evento *</label>
          <select class="input" [(ngModel)]="form.evento">
            @for (ev of EVENTOS; track ev) {
              <option [value]="ev">{{ ev }}</option>
            }
          </select>
          <div class="form-row">
            <div><label class="label">Minutos antes *</label><input class="input" type="number" min="0" [(ngModel)]="form.minutosAntes" /></div>
            <div>
              <label class="label">Plantilla *</label>
              <select class="input" [(ngModel)]="form.plantillaId">
                <option value="">—</option>
                @for (p of plantillas; track p.id) {
                  <option [value]="p.id">{{ p.nombre }}</option>
                }
              </select>
            </div>
          </div>
          <label class="label">Condición</label>
          <input class="input" [(ngModel)]="form.condicion" placeholder="ej. estado:cita" />
          <label class="label" style="display:flex; align-items:center; gap:6px">
            <input type="checkbox" [(ngModel)]="form.activa" /> Activa
          </label>
          <div class="modal-actions">
            <button class="btn" (click)="showForm = false">Cancelar</button>
            <button class="btn primary" (click)="guardar()">Guardar</button>
          </div>
        </div>
      </div>
    }
  `,
                imports: [CommonModule, FormsModule],
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(AutomatizacionesComponent, { className: "AutomatizacionesComponent", filePath: "src/app/automatizacion/automatizaciones.ts", lineNumber: 87 }); })();
