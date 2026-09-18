import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DIAS, nombreEstado } from '../core/models';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
import * as i3 from "@angular/forms";
const _forTrack0 = ($index, $item) => $item.id;
function HorariosComponent_For_13_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "option", 7);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const o_r1 = ctx.$implicit;
    i0.ɵɵproperty("value", o_r1.id);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate3("", o_r1.nombres, " ", o_r1.apellidos, " \u2014 ", o_r1.especialidad || "General");
} }
function HorariosComponent_Conditional_14_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 8);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r1.error);
} }
function HorariosComponent_For_33_Template(rf, ctx) { if (rf & 1) {
    const _r3 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "td");
    i0.ɵɵtext(4);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(5, "td");
    i0.ɵɵtext(6);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(7, "td");
    i0.ɵɵtext(8);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(9, "td");
    i0.ɵɵtext(10);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(11, "td")(12, "span", 11);
    i0.ɵɵtext(13);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(14, "td", 12)(15, "button", 13);
    i0.ɵɵlistener("click", function HorariosComponent_For_33_Template_button_click_15_listener() { const h_r4 = i0.ɵɵrestoreView(_r3).$implicit; const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.editar(h_r4)); });
    i0.ɵɵtext(16, "Editar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(17, "button", 14);
    i0.ɵɵlistener("click", function HorariosComponent_For_33_Template_button_click_17_listener() { const h_r4 = i0.ɵɵrestoreView(_r3).$implicit; const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.eliminar(h_r4)); });
    i0.ɵɵtext(18, "Eliminar");
    i0.ɵɵelementEnd()()();
} if (rf & 2) {
    const h_r4 = ctx.$implicit;
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(h_r4.id);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(ctx_r1.DIAS[h_r4.diaSemana - 1] ?? h_r4.diaSemana);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(h_r4.horaInicio);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(h_r4.horaFin);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate1("", h_r4.intervaloMinutos, " min");
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", h_r4.estado === "ACTIVO" ? "ok" : "bad");
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(h_r4.estado);
} }
function HorariosComponent_ForEmpty_34_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 15);
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(ctx_r1.odontologoSeleccionado ? "Sin horarios configurados." : "Selecciona un odont\u00F3logo para ver sus horarios.");
} }
function HorariosComponent_Conditional_35_For_10_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "option", 7);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const d_r6 = ctx.$implicit;
    const $index_r7 = ctx.$index;
    i0.ɵɵproperty("value", $index_r7 + 1);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(d_r6);
} }
function HorariosComponent_Conditional_35_Template(rf, ctx) { if (rf & 1) {
    const _r5 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 16);
    i0.ɵɵlistener("click", function HorariosComponent_Conditional_35_Template_div_click_0_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.close($event)); });
    i0.ɵɵelementStart(1, "div", 17);
    i0.ɵɵlistener("click", function HorariosComponent_Conditional_35_Template_div_click_1_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.stop($event)); });
    i0.ɵɵelementStart(2, "h2");
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "div", 18)(5, "div", 19)(6, "label", 20);
    i0.ɵɵtext(7, "D\u00EDa de la semana *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(8, "select", 21);
    i0.ɵɵtwoWayListener("ngModelChange", function HorariosComponent_Conditional_35_Template_select_ngModelChange_8_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.form.diaSemana, $event) || (ctx_r1.form.diaSemana = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵrepeaterCreate(9, HorariosComponent_Conditional_35_For_10_Template, 2, 2, "option", 7, i0.ɵɵrepeaterTrackByIndex);
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(11, "div")(12, "label", 20);
    i0.ɵɵtext(13, "Hora inicio *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(14, "input", 22);
    i0.ɵɵtwoWayListener("ngModelChange", function HorariosComponent_Conditional_35_Template_input_ngModelChange_14_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.form.horaInicio, $event) || (ctx_r1.form.horaInicio = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(15, "div")(16, "label", 20);
    i0.ɵɵtext(17, "Hora fin *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(18, "input", 22);
    i0.ɵɵtwoWayListener("ngModelChange", function HorariosComponent_Conditional_35_Template_input_ngModelChange_18_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.form.horaFin, $event) || (ctx_r1.form.horaFin = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(19, "div", 19)(20, "label", 20);
    i0.ɵɵtext(21, "Intervalo (min) *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(22, "input", 23);
    i0.ɵɵtwoWayListener("ngModelChange", function HorariosComponent_Conditional_35_Template_input_ngModelChange_22_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.form.intervaloMinutos, $event) || (ctx_r1.form.intervaloMinutos = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(23, "div", 24)(24, "button", 25);
    i0.ɵɵlistener("click", function HorariosComponent_Conditional_35_Template_button_click_24_listener() { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.showForm = false); });
    i0.ɵɵtext(25, "Cancelar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(26, "button", 26);
    i0.ɵɵlistener("click", function HorariosComponent_Conditional_35_Template_button_click_26_listener() { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.guardar()); });
    i0.ɵɵtext(27, "Guardar");
    i0.ɵɵelementEnd()()()();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(ctx_r1.form.id ? "Editar horario" : "Nuevo horario");
    i0.ɵɵadvance(5);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.form.diaSemana);
    i0.ɵɵcontrol();
    i0.ɵɵadvance();
    i0.ɵɵrepeater(ctx_r1.DIAS);
    i0.ɵɵadvance(5);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.form.horaInicio);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.form.horaFin);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.form.intervaloMinutos);
    i0.ɵɵcontrol();
} }
export class HorariosComponent {
    api;
    DIAS = DIAS;
    nombreEstado = nombreEstado;
    odontologos = [];
    odontologoSeleccionado = '';
    items = [];
    error = '';
    showForm = false;
    form = {};
    constructor(api) {
        this.api = api;
        this.api.get('/odontologos/activos').subscribe({
            next: (r) => {
                this.odontologos = r;
                if (r.length) {
                    this.odontologoSeleccionado = String(r[0].id);
                    this.cargar();
                }
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    cargar() {
        if (!this.odontologoSeleccionado)
            return;
        this.api.get(`/horarios?odontologoId=${this.odontologoSeleccionado}`).subscribe({
            next: (r) => (this.items = r),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    nuevo() {
        this.form = { diaSemana: 1, horaInicio: '08:00', horaFin: '17:00', intervaloMinutos: 30, estado: 'ACTIVO' };
        this.showForm = true;
    }
    editar(h) {
        this.form = { ...h };
        this.showForm = true;
    }
    guardar() {
        if (!this.form.diaSemana || !this.form.horaInicio || !this.form.horaFin)
            return;
        const body = { ...this.form, odontologoId: Number(this.odontologoSeleccionado), estado: this.form.estado || 'ACTIVO' };
        const req = this.form.id
            ? this.api.put(`/horarios/${this.form.id}`, body)
            : this.api.post('/horarios', body);
        req.subscribe({
            next: () => {
                this.showForm = false;
                this.cargar();
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    eliminar(h) {
        if (!confirm('¿Eliminar este horario?'))
            return;
        this.api.del(`/horarios/${h.id}`).subscribe({
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
    static ɵfac = function HorariosComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || HorariosComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: HorariosComponent, selectors: [["app-horarios"]], decls: 36, vars: 5, consts: [[1, "card"], [1, "between"], [1, "btn", "primary", 3, "click", "disabled"], [1, "toolbar"], [1, "label", 2, "margin", "0 6px 0 0"], [1, "input", 2, "width", "280px", 3, "ngModelChange", "change", "ngModel"], ["value", "", "disabled", ""], [3, "value"], [1, "msg", "error"], [1, "tbl"], [1, "modal-backdrop"], [1, "badge", 3, "ngClass"], [1, "flex"], [1, "btn", "small", 3, "click"], [1, "btn", "small", "danger", 3, "click"], ["colspan", "7", 1, "empty"], [1, "modal-backdrop", 3, "click"], [1, "modal", 3, "click"], [1, "form-row"], [1, "full"], [1, "label"], [1, "input", 3, "ngModelChange", "ngModel"], ["type", "time", "required", "", 1, "input", 3, "ngModelChange", "ngModel"], ["type", "number", "min", "5", 1, "input", 3, "ngModelChange", "ngModel"], [1, "modal-actions"], [1, "btn", 3, "click"], [1, "btn", "primary", 3, "click"]], template: function HorariosComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Horarios de atenci\u00F3n");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "button", 2);
            i0.ɵɵlistener("click", function HorariosComponent_Template_button_click_4_listener() { return ctx.nuevo(); });
            i0.ɵɵtext(5, "+ Nuevo horario");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(6, "div", 3)(7, "label", 4);
            i0.ɵɵtext(8, "Odont\u00F3logo");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(9, "select", 5);
            i0.ɵɵtwoWayListener("ngModelChange", function HorariosComponent_Template_select_ngModelChange_9_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.odontologoSeleccionado, $event) || (ctx.odontologoSeleccionado = $event); return $event; });
            i0.ɵɵlistener("change", function HorariosComponent_Template_select_change_9_listener() { return ctx.cargar(); });
            i0.ɵɵelementStart(10, "option", 6);
            i0.ɵɵtext(11, "Selecciona un odont\u00F3logo");
            i0.ɵɵelementEnd();
            i0.ɵɵrepeaterCreate(12, HorariosComponent_For_13_Template, 2, 4, "option", 7, _forTrack0);
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementEnd();
            i0.ɵɵconditionalCreate(14, HorariosComponent_Conditional_14_Template, 2, 1, "div", 8);
            i0.ɵɵelementStart(15, "table", 9)(16, "thead")(17, "tr")(18, "th");
            i0.ɵɵtext(19, "ID");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(20, "th");
            i0.ɵɵtext(21, "D\u00EDa");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(22, "th");
            i0.ɵɵtext(23, "Inicio");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(24, "th");
            i0.ɵɵtext(25, "Fin");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(26, "th");
            i0.ɵɵtext(27, "Intervalo");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(28, "th");
            i0.ɵɵtext(29, "Estado");
            i0.ɵɵelementEnd();
            i0.ɵɵelement(30, "th");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(31, "tbody");
            i0.ɵɵrepeaterCreate(32, HorariosComponent_For_33_Template, 19, 7, "tr", null, _forTrack0, false, HorariosComponent_ForEmpty_34_Template, 3, 1, "tr");
            i0.ɵɵelementEnd()()();
            i0.ɵɵconditionalCreate(35, HorariosComponent_Conditional_35_Template, 28, 5, "div", 10);
        } if (rf & 2) {
            i0.ɵɵadvance(4);
            i0.ɵɵproperty("disabled", !ctx.odontologoSeleccionado);
            i0.ɵɵadvance(5);
            i0.ɵɵtwoWayProperty("ngModel", ctx.odontologoSeleccionado);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(3);
            i0.ɵɵrepeater(ctx.odontologos);
            i0.ɵɵadvance(2);
            i0.ɵɵconditional(ctx.error ? 14 : -1);
            i0.ɵɵadvance(18);
            i0.ɵɵrepeater(ctx.items);
            i0.ɵɵadvance(3);
            i0.ɵɵconditional(ctx.showForm ? 35 : -1);
        } }, dependencies: [CommonModule, i2.NgClass, FormsModule, i3.NgSelectOption, i3.ɵNgSelectMultipleOption, i3.DefaultValueAccessor, i3.NumberValueAccessor, i3.SelectControlValueAccessor, i3.NgControlStatus, i3.RequiredValidator, i3.MinValidator, i3.NgModel], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(HorariosComponent, [{
        type: Component,
        args: [{
                selector: 'app-horarios',
                template: `
    <div class="card">
      <div class="between">
        <h2>Horarios de atención</h2>
        <button class="btn primary" [disabled]="!odontologoSeleccionado" (click)="nuevo()">+ Nuevo horario</button>
      </div>
      <div class="toolbar">
        <label class="label" style="margin:0 6px 0 0">Odontólogo</label>
        <select class="input" style="width:280px" [(ngModel)]="odontologoSeleccionado" (change)="cargar()">
          <option value="" disabled>Selecciona un odontólogo</option>
          @for (o of odontologos; track o.id) {
            <option [value]="o.id">{{ o.nombres }} {{ o.apellidos }} — {{ o.especialidad || 'General' }}</option>
          }
        </select>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead>
          <tr><th>ID</th><th>Día</th><th>Inicio</th><th>Fin</th><th>Intervalo</th><th>Estado</th><th></th></tr>
        </thead>
        <tbody>
          @for (h of items; track h.id) {
            <tr>
              <td>{{ h.id }}</td>
              <td>{{ DIAS[h.diaSemana - 1] ?? h.diaSemana }}</td>
              <td>{{ h.horaInicio }}</td>
              <td>{{ h.horaFin }}</td>
              <td>{{ h.intervaloMinutos }} min</td>
              <td><span class="badge" [ngClass]="h.estado === 'ACTIVO' ? 'ok' : 'bad'">{{ h.estado }}</span></td>
              <td class="flex">
                <button class="btn small" (click)="editar(h)">Editar</button>
                <button class="btn small danger" (click)="eliminar(h)">Eliminar</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="empty">{{ odontologoSeleccionado ? 'Sin horarios configurados.' : 'Selecciona un odontólogo para ver sus horarios.' }}</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (showForm) {
      <div class="modal-backdrop" (click)="close($event)">
        <div class="modal" (click)="stop($event)">
          <h2>{{ form.id ? 'Editar horario' : 'Nuevo horario' }}</h2>
          <div class="form-row">
            <div class="full">
              <label class="label">Día de la semana *</label>
              <select class="input" [(ngModel)]="form.diaSemana">
                @for (d of DIAS; track $index) {
                  <option [value]="$index + 1">{{ d }}</option>
                }
              </select>
            </div>
            <div><label class="label">Hora inicio *</label><input class="input" type="time" [(ngModel)]="form.horaInicio" required /></div>
            <div><label class="label">Hora fin *</label><input class="input" type="time" [(ngModel)]="form.horaFin" required /></div>
            <div class="full"><label class="label">Intervalo (min) *</label><input class="input" type="number" min="5" [(ngModel)]="form.intervaloMinutos" /></div>
          </div>
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
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(HorariosComponent, { className: "HorariosComponent", filePath: "src/app/horarios/horarios.ts", lineNumber: 79 }); })();
