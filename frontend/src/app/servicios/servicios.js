import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
import * as i3 from "@angular/forms";
const _forTrack0 = ($index, $item) => $item.id;
function ServiciosComponent_Conditional_17_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 10);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function ServiciosComponent_Conditional_35_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 16);
    i0.ɵɵelement(2, "span", 17);
    i0.ɵɵtext(3, " Cargando servicios\u2026");
    i0.ɵɵelementEnd()();
} }
function ServiciosComponent_For_37_Template(rf, ctx) { if (rf & 1) {
    const _r2 = i0.ɵɵgetCurrentView();
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
    i0.ɵɵelementStart(11, "td")(12, "span", 18);
    i0.ɵɵtext(13);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(14, "td", 19)(15, "button", 20);
    i0.ɵɵlistener("click", function ServiciosComponent_For_37_Template_button_click_15_listener() { const s_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.editar(s_r3)); });
    i0.ɵɵtext(16, "Editar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(17, "button", 21);
    i0.ɵɵlistener("click", function ServiciosComponent_For_37_Template_button_click_17_listener() { const s_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.desactivar(s_r3)); });
    i0.ɵɵtext(18, "Desactivar");
    i0.ɵɵelementEnd()()();
} if (rf & 2) {
    const s_r3 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(s_r3.id);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(s_r3.nombre);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(s_r3.descripcion || "-");
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate1("", s_r3.duracionMinutos, " min");
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate1("$ ", s_r3.precio.toFixed(2));
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", s_r3.estado === "ACTIVO" ? "ok" : "bad");
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(s_r3.estado);
    i0.ɵɵadvance(4);
    i0.ɵɵproperty("disabled", s_r3.estado !== "ACTIVO");
} }
function ServiciosComponent_ForEmpty_38_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 16);
    i0.ɵɵtext(2, "Sin servicios registrados.");
    i0.ɵɵelementEnd()();
} }
function ServiciosComponent_Conditional_48_Template(rf, ctx) { if (rf & 1) {
    const _r4 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 22);
    i0.ɵɵlistener("click", function ServiciosComponent_Conditional_48_Template_div_click_0_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.close($event)); });
    i0.ɵɵelementStart(1, "div", 23);
    i0.ɵɵlistener("click", function ServiciosComponent_Conditional_48_Template_div_click_1_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.stop($event)); });
    i0.ɵɵelementStart(2, "h2");
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "div", 24)(5, "div", 25)(6, "label", 26);
    i0.ɵɵtext(7, "Nombre *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(8, "input", 27);
    i0.ɵɵtwoWayListener("ngModelChange", function ServiciosComponent_Conditional_48_Template_input_ngModelChange_8_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.nombre, $event) || (ctx_r0.form.nombre = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(9, "div", 25)(10, "label", 26);
    i0.ɵɵtext(11, "Descripci\u00F3n");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(12, "textarea", 28);
    i0.ɵɵtwoWayListener("ngModelChange", function ServiciosComponent_Conditional_48_Template_textarea_ngModelChange_12_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.descripcion, $event) || (ctx_r0.form.descripcion = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(13, "div")(14, "label", 26);
    i0.ɵɵtext(15, "Duraci\u00F3n (min) *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(16, "input", 29);
    i0.ɵɵtwoWayListener("ngModelChange", function ServiciosComponent_Conditional_48_Template_input_ngModelChange_16_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.duracionMinutos, $event) || (ctx_r0.form.duracionMinutos = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(17, "div")(18, "label", 26);
    i0.ɵɵtext(19, "Precio *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(20, "input", 30);
    i0.ɵɵtwoWayListener("ngModelChange", function ServiciosComponent_Conditional_48_Template_input_ngModelChange_20_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.precio, $event) || (ctx_r0.form.precio = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(21, "div", 31)(22, "button", 9);
    i0.ɵɵlistener("click", function ServiciosComponent_Conditional_48_Template_button_click_22_listener() { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.showForm = false); });
    i0.ɵɵtext(23, "Cancelar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(24, "button", 2);
    i0.ɵɵlistener("click", function ServiciosComponent_Conditional_48_Template_button_click_24_listener() { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.guardar()); });
    i0.ɵɵtext(25, "Guardar");
    i0.ɵɵelementEnd()()()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(ctx_r0.form.id ? "Editar servicio" : "Nuevo servicio");
    i0.ɵɵadvance(5);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.nombre);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.descripcion);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.duracionMinutos);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.precio);
    i0.ɵɵcontrol();
} }
export class ServiciosComponent {
    api;
    items = [];
    q = '';
    estado = 'ACTIVO';
    page = 0;
    size = 15;
    totalPages = 1;
    error = '';
    cargando = false;
    showForm = false;
    form = {};
    constructor(api) {
        this.api = api;
        this.cargar(0);
    }
    cargar(p) {
        this.error = '';
        this.cargando = true;
        const params = new URLSearchParams({ page: String(p), size: String(this.size) });
        if (this.q.trim())
            params.set('q', this.q.trim());
        if (this.estado)
            params.set('estado', this.estado);
        this.api.get(`/servicios?${params.toString()}`).subscribe({
            next: (r) => {
                this.cargando = false;
                this.items = r.content;
                this.totalPages = Math.max(r.totalPages ?? 1, 1);
                this.page = Math.min(p, this.totalPages - 1);
            },
            error: (e) => {
                this.cargando = false;
                this.error = this.msg(e);
            },
        });
    }
    nuevo() {
        this.form = { duracionMinutos: 30, precio: 0, estado: 'ACTIVO' };
        this.showForm = true;
    }
    editar(s) {
        this.form = { ...s };
        this.showForm = true;
    }
    guardar() {
        if (!this.form.nombre || !this.form.duracionMinutos || this.form.precio == null)
            return;
        const body = { ...this.form, estado: this.form.estado || 'ACTIVO' };
        const req = this.form.id
            ? this.api.put(`/servicios/${this.form.id}`, body)
            : this.api.post('/servicios', body);
        req.subscribe({
            next: () => {
                this.showForm = false;
                this.cargar(this.page);
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    desactivar(s) {
        this.api.del(`/servicios/${s.id}`).subscribe({
            next: () => this.cargar(this.page),
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
    static ɵfac = function ServiciosComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || ServiciosComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: ServiciosComponent, selectors: [["app-servicios"]], decls: 49, vars: 11, consts: [[1, "card"], [1, "between"], [1, "btn", "primary", 3, "click"], [1, "toolbar"], ["placeholder", "Buscar por nombre", 1, "input", 2, "width", "260px", 3, "ngModelChange", "keyup.enter", "ngModel"], [1, "input", 2, "width", "150px", 3, "ngModelChange", "change", "ngModel"], ["value", ""], ["value", "ACTIVO"], ["value", "INACTIVO"], [1, "btn", 3, "click"], [1, "msg", "error"], [1, "tbl"], [1, "between", 2, "margin-top", "10px"], [1, "btn", "small", 3, "click", "disabled"], [1, "muted"], [1, "modal-backdrop"], ["colspan", "7", 1, "empty"], [1, "spinner"], [1, "badge", 3, "ngClass"], [1, "flex"], [1, "btn", "small", 3, "click"], [1, "btn", "small", "danger", 3, "click", "disabled"], [1, "modal-backdrop", 3, "click"], [1, "modal", 3, "click"], [1, "form-row"], [1, "full"], [1, "label"], ["required", "", 1, "input", 3, "ngModelChange", "ngModel"], [1, "input", 3, "ngModelChange", "ngModel"], ["type", "number", "min", "1", 1, "input", 3, "ngModelChange", "ngModel"], ["type", "number", "min", "0", "step", "0.01", 1, "input", 3, "ngModelChange", "ngModel"], [1, "modal-actions"]], template: function ServiciosComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Servicios");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "button", 2);
            i0.ɵɵlistener("click", function ServiciosComponent_Template_button_click_4_listener() { return ctx.nuevo(); });
            i0.ɵɵtext(5, "+ Nuevo servicio");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(6, "div", 3)(7, "input", 4);
            i0.ɵɵtwoWayListener("ngModelChange", function ServiciosComponent_Template_input_ngModelChange_7_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.q, $event) || (ctx.q = $event); return $event; });
            i0.ɵɵlistener("keyup.enter", function ServiciosComponent_Template_input_keyup_enter_7_listener() { return ctx.cargar(0); });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(8, "select", 5);
            i0.ɵɵtwoWayListener("ngModelChange", function ServiciosComponent_Template_select_ngModelChange_8_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.estado, $event) || (ctx.estado = $event); return $event; });
            i0.ɵɵlistener("change", function ServiciosComponent_Template_select_change_8_listener() { return ctx.cargar(0); });
            i0.ɵɵelementStart(9, "option", 6);
            i0.ɵɵtext(10, "Todos los estados");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(11, "option", 7);
            i0.ɵɵtext(12, "Activos");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(13, "option", 8);
            i0.ɵɵtext(14, "Inactivos");
            i0.ɵɵelementEnd()();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(15, "button", 9);
            i0.ɵɵlistener("click", function ServiciosComponent_Template_button_click_15_listener() { return ctx.cargar(0); });
            i0.ɵɵtext(16, "Buscar");
            i0.ɵɵelementEnd()();
            i0.ɵɵconditionalCreate(17, ServiciosComponent_Conditional_17_Template, 2, 1, "div", 10);
            i0.ɵɵelementStart(18, "table", 11)(19, "thead")(20, "tr")(21, "th");
            i0.ɵɵtext(22, "ID");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(23, "th");
            i0.ɵɵtext(24, "Nombre");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(25, "th");
            i0.ɵɵtext(26, "Descripci\u00F3n");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(27, "th");
            i0.ɵɵtext(28, "Duraci\u00F3n");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(29, "th");
            i0.ɵɵtext(30, "Precio");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(31, "th");
            i0.ɵɵtext(32, "Estado");
            i0.ɵɵelementEnd();
            i0.ɵɵelement(33, "th");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(34, "tbody");
            i0.ɵɵconditionalCreate(35, ServiciosComponent_Conditional_35_Template, 4, 0, "tr");
            i0.ɵɵrepeaterCreate(36, ServiciosComponent_For_37_Template, 19, 8, "tr", null, _forTrack0, false, ServiciosComponent_ForEmpty_38_Template, 3, 0, "tr");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(39, "div", 12)(40, "button", 13);
            i0.ɵɵlistener("click", function ServiciosComponent_Template_button_click_40_listener() { return ctx.cargar(ctx.page - 1); });
            i0.ɵɵtext(41, "Anterior");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(42, "span", 14);
            i0.ɵɵtext(43);
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(44, "button", 13);
            i0.ɵɵlistener("click", function ServiciosComponent_Template_button_click_44_listener() { return ctx.cargar(ctx.page); });
            i0.ɵɵtext(45, "Recargar");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(46, "button", 13);
            i0.ɵɵlistener("click", function ServiciosComponent_Template_button_click_46_listener() { return ctx.cargar(ctx.page + 1); });
            i0.ɵɵtext(47, "Siguiente");
            i0.ɵɵelementEnd()()();
            i0.ɵɵconditionalCreate(48, ServiciosComponent_Conditional_48_Template, 26, 5, "div", 15);
        } if (rf & 2) {
            i0.ɵɵadvance(7);
            i0.ɵɵtwoWayProperty("ngModel", ctx.q);
            i0.ɵɵcontrol();
            i0.ɵɵadvance();
            i0.ɵɵtwoWayProperty("ngModel", ctx.estado);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(9);
            i0.ɵɵconditional(ctx.error ? 17 : -1);
            i0.ɵɵadvance(18);
            i0.ɵɵconditional(ctx.cargando ? 35 : -1);
            i0.ɵɵadvance();
            i0.ɵɵrepeater(ctx.items);
            i0.ɵɵadvance(4);
            i0.ɵɵproperty("disabled", ctx.page <= 0);
            i0.ɵɵadvance(3);
            i0.ɵɵtextInterpolate2("P\u00E1gina ", ctx.page + 1, " de ", ctx.totalPages);
            i0.ɵɵadvance();
            i0.ɵɵproperty("disabled", !ctx.cargando);
            i0.ɵɵadvance(2);
            i0.ɵɵproperty("disabled", ctx.page + 1 >= ctx.totalPages);
            i0.ɵɵadvance(2);
            i0.ɵɵconditional(ctx.showForm ? 48 : -1);
        } }, dependencies: [CommonModule, i2.NgClass, FormsModule, i3.NgSelectOption, i3.ɵNgSelectMultipleOption, i3.DefaultValueAccessor, i3.NumberValueAccessor, i3.SelectControlValueAccessor, i3.NgControlStatus, i3.RequiredValidator, i3.MinValidator, i3.NgModel], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(ServiciosComponent, [{
        type: Component,
        args: [{
                selector: 'app-servicios',
                template: `
    <div class="card">
      <div class="between">
        <h2>Servicios</h2>
        <button class="btn primary" (click)="nuevo()">+ Nuevo servicio</button>
      </div>
      <div class="toolbar">
        <input class="input" style="width:260px" placeholder="Buscar por nombre" [(ngModel)]="q" (keyup.enter)="cargar(0)" />
        <select class="input" style="width:150px" [(ngModel)]="estado" (change)="cargar(0)">
          <option value="">Todos los estados</option>
          <option value="ACTIVO">Activos</option>
          <option value="INACTIVO">Inactivos</option>
        </select>
        <button class="btn" (click)="cargar(0)">Buscar</button>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead>
          <tr><th>ID</th><th>Nombre</th><th>Descripción</th><th>Duración</th><th>Precio</th><th>Estado</th><th></th></tr>
        </thead>
        <tbody>
          @if (cargando) {
            <tr><td colspan="7" class="empty"><span class="spinner"></span> Cargando servicios…</td></tr>
          }
          @for (s of items; track s.id) {
            <tr>
              <td>{{ s.id }}</td>
              <td>{{ s.nombre }}</td>
              <td>{{ s.descripcion || '-' }}</td>
              <td>{{ s.duracionMinutos }} min</td>
              <td>$ {{ s.precio.toFixed(2) }}</td>
              <td><span class="badge" [ngClass]="s.estado === 'ACTIVO' ? 'ok' : 'bad'">{{ s.estado }}</span></td>
              <td class="flex">
                <button class="btn small" (click)="editar(s)">Editar</button>
                <button class="btn small danger" (click)="desactivar(s)" [disabled]="s.estado !== 'ACTIVO'">Desactivar</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="empty">Sin servicios registrados.</td></tr>
          }
        </tbody>
      </table>
      <div class="between" style="margin-top:10px">
        <button class="btn small" (click)="cargar(page - 1)" [disabled]="page <= 0">Anterior</button>
        <span class="muted">Página {{ page + 1 }} de {{ totalPages }}</span>
        <button class="btn small" (click)="cargar(page)" [disabled]="!cargando">Recargar</button>
        <button class="btn small" (click)="cargar(page + 1)" [disabled]="page + 1 >= totalPages">Siguiente</button>
      </div>
    </div>

    @if (showForm) {
      <div class="modal-backdrop" (click)="close($event)">
        <div class="modal" (click)="stop($event)">
          <h2>{{ form.id ? 'Editar servicio' : 'Nuevo servicio' }}</h2>
          <div class="form-row">
            <div class="full"><label class="label">Nombre *</label><input class="input" [(ngModel)]="form.nombre" required /></div>
            <div class="full"><label class="label">Descripción</label><textarea class="input" [(ngModel)]="form.descripcion"></textarea></div>
            <div><label class="label">Duración (min) *</label><input class="input" type="number" min="1" [(ngModel)]="form.duracionMinutos" /></div>
            <div><label class="label">Precio *</label><input class="input" type="number" min="0" step="0.01" [(ngModel)]="form.precio" /></div>
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
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(ServiciosComponent, { className: "ServiciosComponent", filePath: "src/app/servicios/servicios.ts", lineNumber: 81 }); })();
