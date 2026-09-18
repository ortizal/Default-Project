import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
import * as i3 from "@angular/forms";
const _forTrack0 = ($index, $item) => $item.id;
function PlantillasComponent_Conditional_6_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 3);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function PlantillasComponent_For_21_Template(rf, ctx) { if (rf & 1) {
    const _r2 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "td");
    i0.ɵɵtext(4);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(5, "td", 6)(6, "div", 7);
    i0.ɵɵtext(7);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(8, "td")(9, "span", 8);
    i0.ɵɵtext(10);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(11, "td", 9)(12, "button", 10);
    i0.ɵɵlistener("click", function PlantillasComponent_For_21_Template_button_click_12_listener() { const p_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.editar(p_r3)); });
    i0.ɵɵtext(13, "Editar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(14, "button", 11);
    i0.ɵɵlistener("click", function PlantillasComponent_For_21_Template_button_click_14_listener() { const p_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.eliminar(p_r3)); });
    i0.ɵɵtext(15, "Eliminar");
    i0.ɵɵelementEnd()()();
} if (rf & 2) {
    const p_r3 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(p_r3.id);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(p_r3.nombre);
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(p_r3.contenido);
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", p_r3.activa ? "ok" : "dim");
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(p_r3.activa ? "S\u00ED" : "No");
} }
function PlantillasComponent_ForEmpty_22_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 12);
    i0.ɵɵtext(2, "Sin plantillas.");
    i0.ɵɵelementEnd()();
} }
function PlantillasComponent_Conditional_23_Template(rf, ctx) { if (rf & 1) {
    const _r4 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 13);
    i0.ɵɵlistener("click", function PlantillasComponent_Conditional_23_Template_div_click_0_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.close($event)); });
    i0.ɵɵelementStart(1, "div", 14);
    i0.ɵɵlistener("click", function PlantillasComponent_Conditional_23_Template_div_click_1_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.stop($event)); });
    i0.ɵɵelementStart(2, "h2");
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "label", 15);
    i0.ɵɵtext(5, "Nombre *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(6, "input", 16);
    i0.ɵɵtwoWayListener("ngModelChange", function PlantillasComponent_Conditional_23_Template_input_ngModelChange_6_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.nombre, $event) || (ctx_r0.form.nombre = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementStart(7, "label", 15);
    i0.ɵɵtext(8, "Contenido *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(9, "textarea", 17);
    i0.ɵɵtwoWayListener("ngModelChange", function PlantillasComponent_Conditional_23_Template_textarea_ngModelChange_9_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.contenido, $event) || (ctx_r0.form.contenido = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementStart(10, "div", 18);
    i0.ɵɵtext(11, " Variables: ");
    i0.ɵɵelementStart(12, "code");
    i0.ɵɵtext(13, "{nombres}");
    i0.ɵɵelementEnd();
    i0.ɵɵtext(14, ", ");
    i0.ɵɵelementStart(15, "code");
    i0.ɵɵtext(16, "{fecha}");
    i0.ɵɵelementEnd();
    i0.ɵɵtext(17, ", ");
    i0.ɵɵelementStart(18, "code");
    i0.ɵɵtext(19, "{hora}");
    i0.ɵɵelementEnd();
    i0.ɵɵtext(20, ", ");
    i0.ɵɵelementStart(21, "code");
    i0.ɵɵtext(22, "{servicio}");
    i0.ɵɵelementEnd();
    i0.ɵɵtext(23, ", ");
    i0.ɵɵelementStart(24, "code");
    i0.ɵɵtext(25, "{doctor}");
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(26, "label", 19)(27, "input", 20);
    i0.ɵɵtwoWayListener("ngModelChange", function PlantillasComponent_Conditional_23_Template_input_ngModelChange_27_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.activa, $event) || (ctx_r0.form.activa = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵtext(28, " Activa ");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(29, "div", 21)(30, "button", 22);
    i0.ɵɵlistener("click", function PlantillasComponent_Conditional_23_Template_button_click_30_listener() { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.showForm = false); });
    i0.ɵɵtext(31, "Cancelar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(32, "button", 2);
    i0.ɵɵlistener("click", function PlantillasComponent_Conditional_23_Template_button_click_32_listener() { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.guardar()); });
    i0.ɵɵtext(33, "Guardar");
    i0.ɵɵelementEnd()()()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(ctx_r0.form.id ? "Editar plantilla" : "Nueva plantilla");
    i0.ɵɵadvance(3);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.nombre);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(3);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.contenido);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(18);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.activa);
    i0.ɵɵcontrol();
} }
export class PlantillasComponent {
    api;
    items = [];
    error = '';
    showForm = false;
    form = {};
    constructor(api) {
        this.api = api;
        this.cargar();
    }
    cargar() {
        this.api.get('/plantillas').subscribe({
            next: (r) => (this.items = r),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    nueva() {
        this.form = { nombre: '', contenido: '', activa: true };
        this.showForm = true;
    }
    editar(p) {
        this.form = { ...p };
        this.showForm = true;
    }
    guardar() {
        if (!this.form.nombre || !this.form.contenido)
            return;
        const body = { nombre: this.form.nombre, contenido: this.form.contenido, activa: this.form.activa ?? true };
        const req = this.form.id
            ? this.api.put(`/plantillas/${this.form.id}`, body)
            : this.api.post('/plantillas', body);
        req.subscribe({
            next: () => {
                this.showForm = false;
                this.cargar();
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    eliminar(p) {
        if (!confirm('¿Eliminar esta plantilla?'))
            return;
        this.api.del(`/plantillas/${p.id}`).subscribe({
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
    static ɵfac = function PlantillasComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || PlantillasComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: PlantillasComponent, selectors: [["app-plantillas"]], decls: 24, vars: 3, consts: [[1, "card"], [1, "between"], [1, "btn", "primary", 3, "click"], [1, "msg", "error"], [1, "tbl"], [1, "modal-backdrop"], [2, "max-width", "420px"], [2, "overflow", "hidden", "text-overflow", "ellipsis", "white-space", "nowrap"], [1, "badge", 3, "ngClass"], [1, "flex"], [1, "btn", "small", 3, "click"], [1, "btn", "small", "danger", 3, "click"], ["colspan", "5", 1, "empty"], [1, "modal-backdrop", 3, "click"], [1, "modal", 3, "click"], [1, "label"], ["required", "", 1, "input", 3, "ngModelChange", "ngModel"], ["rows", "5", "required", "", 1, "input", 3, "ngModelChange", "ngModel"], [1, "muted", 2, "font-size", "12px", "margin-top", "4px"], [1, "label", 2, "display", "flex", "align-items", "center", "gap", "6px"], ["type", "checkbox", 3, "ngModelChange", "ngModel"], [1, "modal-actions"], [1, "btn", 3, "click"]], template: function PlantillasComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Plantillas de mensajes");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "button", 2);
            i0.ɵɵlistener("click", function PlantillasComponent_Template_button_click_4_listener() { return ctx.nueva(); });
            i0.ɵɵtext(5, "+ Nueva plantilla");
            i0.ɵɵelementEnd()();
            i0.ɵɵconditionalCreate(6, PlantillasComponent_Conditional_6_Template, 2, 1, "div", 3);
            i0.ɵɵelementStart(7, "table", 4)(8, "thead")(9, "tr")(10, "th");
            i0.ɵɵtext(11, "ID");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(12, "th");
            i0.ɵɵtext(13, "Nombre");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(14, "th");
            i0.ɵɵtext(15, "Contenido");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(16, "th");
            i0.ɵɵtext(17, "Activa");
            i0.ɵɵelementEnd();
            i0.ɵɵelement(18, "th");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(19, "tbody");
            i0.ɵɵrepeaterCreate(20, PlantillasComponent_For_21_Template, 16, 5, "tr", null, _forTrack0, false, PlantillasComponent_ForEmpty_22_Template, 3, 0, "tr");
            i0.ɵɵelementEnd()()();
            i0.ɵɵconditionalCreate(23, PlantillasComponent_Conditional_23_Template, 34, 4, "div", 5);
        } if (rf & 2) {
            i0.ɵɵadvance(6);
            i0.ɵɵconditional(ctx.error ? 6 : -1);
            i0.ɵɵadvance(14);
            i0.ɵɵrepeater(ctx.items);
            i0.ɵɵadvance(3);
            i0.ɵɵconditional(ctx.showForm ? 23 : -1);
        } }, dependencies: [CommonModule, i2.NgClass, FormsModule, i3.DefaultValueAccessor, i3.CheckboxControlValueAccessor, i3.NgControlStatus, i3.RequiredValidator, i3.NgModel], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(PlantillasComponent, [{
        type: Component,
        args: [{
                selector: 'app-plantillas',
                template: `
    <div class="card">
      <div class="between">
        <h2>Plantillas de mensajes</h2>
        <button class="btn primary" (click)="nueva()">+ Nueva plantilla</button>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead><tr><th>ID</th><th>Nombre</th><th>Contenido</th><th>Activa</th><th></th></tr></thead>
        <tbody>
          @for (p of items; track p.id) {
            <tr>
              <td>{{ p.id }}</td>
              <td>{{ p.nombre }}</td>
              <td style="max-width:420px"><div style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap">{{ p.contenido }}</div></td>
              <td><span class="badge" [ngClass]="p.activa ? 'ok' : 'dim'">{{ p.activa ? 'Sí' : 'No' }}</span></td>
              <td class="flex">
                <button class="btn small" (click)="editar(p)">Editar</button>
                <button class="btn small danger" (click)="eliminar(p)">Eliminar</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="empty">Sin plantillas.</td></tr>
          }
        </tbody>
      </table>
    </div>

    @if (showForm) {
      <div class="modal-backdrop" (click)="close($event)">
        <div class="modal" (click)="stop($event)">
          <h2>{{ form.id ? 'Editar plantilla' : 'Nueva plantilla' }}</h2>
          <label class="label">Nombre *</label>
          <input class="input" [(ngModel)]="form.nombre" required />
          <label class="label">Contenido *</label>
          <textarea class="input" rows="5" [(ngModel)]="form.contenido" required></textarea>
          <div class="muted" style="font-size:12px; margin-top:4px">
            Variables: <code>&#123;nombres&#125;</code>, <code>&#123;fecha&#125;</code>, <code>&#123;hora&#125;</code>, <code>&#123;servicio&#125;</code>, <code>&#123;doctor&#125;</code>
          </div>
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
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(PlantillasComponent, { className: "PlantillasComponent", filePath: "src/app/automatizacion/plantillas.ts", lineNumber: 63 }); })();
