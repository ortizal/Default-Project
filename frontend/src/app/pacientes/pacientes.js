import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
import * as i3 from "@angular/forms";
const _forTrack0 = ($index, $item) => $item.id;
function PacientesComponent_Conditional_19_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 11);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function PacientesComponent_Conditional_37_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 17);
    i0.ɵɵelement(2, "span", 18);
    i0.ɵɵtext(3, " Cargando pacientes\u2026");
    i0.ɵɵelementEnd()();
} }
function PacientesComponent_For_39_Template(rf, ctx) { if (rf & 1) {
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
    i0.ɵɵelementStart(11, "td")(12, "span", 19);
    i0.ɵɵtext(13);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(14, "td", 20)(15, "button", 21);
    i0.ɵɵlistener("click", function PacientesComponent_For_39_Template_button_click_15_listener() { const p_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.editar(p_r3)); });
    i0.ɵɵtext(16, "Editar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(17, "button", 22);
    i0.ɵɵlistener("click", function PacientesComponent_For_39_Template_button_click_17_listener() { const p_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.desactivar(p_r3)); });
    i0.ɵɵtext(18, "Desactivar");
    i0.ɵɵelementEnd()()();
} if (rf & 2) {
    const p_r3 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(p_r3.id);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(p_r3.cedula || "-");
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate2("", p_r3.nombres, " ", p_r3.apellidos);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(p_r3.telefono || "-");
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(p_r3.email || "-");
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", p_r3.estado === "ACTIVO" ? "ok" : "bad");
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(p_r3.estado);
    i0.ɵɵadvance(4);
    i0.ɵɵproperty("disabled", p_r3.estado !== "ACTIVO");
} }
function PacientesComponent_ForEmpty_40_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 17);
    i0.ɵɵtext(2, "Sin pacientes registrados.");
    i0.ɵɵelementEnd()();
} }
function PacientesComponent_Conditional_48_Template(rf, ctx) { if (rf & 1) {
    const _r4 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 23);
    i0.ɵɵlistener("click", function PacientesComponent_Conditional_48_Template_div_click_0_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.close($event)); });
    i0.ɵɵelementStart(1, "div", 24);
    i0.ɵɵlistener("click", function PacientesComponent_Conditional_48_Template_div_click_1_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.stop($event)); });
    i0.ɵɵelementStart(2, "h2");
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "div", 25);
    i0.ɵɵtext(5, "Los campos con * son obligatorios.");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(6, "div", 26)(7, "div")(8, "label", 27);
    i0.ɵɵtext(9, "C\u00E9dula");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(10, "input", 28);
    i0.ɵɵtwoWayListener("ngModelChange", function PacientesComponent_Conditional_48_Template_input_ngModelChange_10_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.cedula, $event) || (ctx_r0.form.cedula = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(11, "div")(12, "label", 27);
    i0.ɵɵtext(13, "Fecha nacimiento");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(14, "input", 29);
    i0.ɵɵtwoWayListener("ngModelChange", function PacientesComponent_Conditional_48_Template_input_ngModelChange_14_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.fechaNacimiento, $event) || (ctx_r0.form.fechaNacimiento = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(15, "div")(16, "label", 27);
    i0.ɵɵtext(17, "Nombres *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(18, "input", 30);
    i0.ɵɵtwoWayListener("ngModelChange", function PacientesComponent_Conditional_48_Template_input_ngModelChange_18_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.nombres, $event) || (ctx_r0.form.nombres = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(19, "div")(20, "label", 27);
    i0.ɵɵtext(21, "Apellidos *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(22, "input", 30);
    i0.ɵɵtwoWayListener("ngModelChange", function PacientesComponent_Conditional_48_Template_input_ngModelChange_22_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.apellidos, $event) || (ctx_r0.form.apellidos = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(23, "div")(24, "label", 27);
    i0.ɵɵtext(25, "Tel\u00E9fono");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(26, "input", 31);
    i0.ɵɵtwoWayListener("ngModelChange", function PacientesComponent_Conditional_48_Template_input_ngModelChange_26_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.telefono, $event) || (ctx_r0.form.telefono = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(27, "div")(28, "label", 27);
    i0.ɵɵtext(29, "Email");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(30, "input", 32);
    i0.ɵɵtwoWayListener("ngModelChange", function PacientesComponent_Conditional_48_Template_input_ngModelChange_30_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.email, $event) || (ctx_r0.form.email = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(31, "div", 33)(32, "label", 27);
    i0.ɵɵtext(33, "Direcci\u00F3n");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(34, "input", 34);
    i0.ɵɵtwoWayListener("ngModelChange", function PacientesComponent_Conditional_48_Template_input_ngModelChange_34_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.direccion, $event) || (ctx_r0.form.direccion = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(35, "div", 33)(36, "label", 27);
    i0.ɵɵtext(37, "Observaciones");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(38, "textarea", 34);
    i0.ɵɵtwoWayListener("ngModelChange", function PacientesComponent_Conditional_48_Template_textarea_ngModelChange_38_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.observaciones, $event) || (ctx_r0.form.observaciones = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(39, "div", 35)(40, "button", 9);
    i0.ɵɵlistener("click", function PacientesComponent_Conditional_48_Template_button_click_40_listener() { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.showForm = false); });
    i0.ɵɵtext(41, "Cancelar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(42, "button", 2);
    i0.ɵɵlistener("click", function PacientesComponent_Conditional_48_Template_button_click_42_listener() { i0.ɵɵrestoreView(_r4); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.guardar()); });
    i0.ɵɵtext(43, "Guardar");
    i0.ɵɵelementEnd()()()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(ctx_r0.form.id ? "Editar paciente" : "Nuevo paciente");
    i0.ɵɵadvance(7);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.cedula);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.fechaNacimiento);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.nombres);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.apellidos);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.telefono);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.email);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.direccion);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.observaciones);
    i0.ɵɵcontrol();
} }
export class PacientesComponent {
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
        this.api.get(`/pacientes?${params.toString()}`).subscribe({
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
        this.form = { fechaNacimiento: '', estado: 'ACTIVO' };
        this.showForm = true;
    }
    editar(p) {
        this.form = { ...p, fechaNacimiento: p.fechaNacimiento ? p.fechaNacimiento.slice(0, 10) : '' };
        this.showForm = true;
    }
    guardar() {
        if (!this.form.nombres || !this.form.apellidos)
            return;
        const body = {
            cedula: this.form.cedula || null,
            nombres: this.form.nombres,
            apellidos: this.form.apellidos,
            telefono: this.form.telefono || null,
            email: this.form.email || null,
            fechaNacimiento: this.form.fechaNacimiento || null,
            direccion: this.form.direccion || null,
            observaciones: this.form.observaciones || null,
            estado: this.form.estado || 'ACTIVO',
        };
        const req = this.form.id
            ? this.api.put(`/pacientes/${this.form.id}`, body)
            : this.api.post('/pacientes', body);
        req.subscribe({
            next: () => {
                this.showForm = false;
                this.cargar(this.page);
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    desactivar(p) {
        this.api.del(`/pacientes/${p.id}`).subscribe({
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
        if (a?.status === 409 || a?.status === 400)
            return a.error?.message ?? a.error?.mensaje ?? 'Datos inválidos';
        return 'Error de conexión';
    }
    static ɵfac = function PacientesComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || PacientesComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: PacientesComponent, selectors: [["app-pacientes"]], decls: 49, vars: 11, consts: [[1, "card"], [1, "between"], [1, "btn", "primary", 3, "click"], [1, "toolbar"], ["placeholder", "Buscar por nombre, c\u00E9dula o tel\u00E9fono", 1, "input", 2, "width", "260px", 3, "ngModelChange", "keyup.enter", "ngModel"], [1, "input", 2, "width", "150px", 3, "ngModelChange", "change", "ngModel"], ["value", ""], ["value", "ACTIVO"], ["value", "INACTIVO"], [1, "btn", 3, "click"], ["title", "Recargar lista", 1, "btn", 3, "click", "disabled"], [1, "msg", "error"], [1, "tbl"], [1, "between", 2, "margin-top", "10px"], [1, "btn", "small", 3, "click", "disabled"], [1, "muted"], [1, "modal-backdrop"], ["colspan", "7", 1, "empty"], [1, "spinner"], [1, "badge", 3, "ngClass"], [1, "flex"], [1, "btn", "small", 3, "click"], [1, "btn", "small", "danger", 3, "click", "disabled"], [1, "modal-backdrop", 3, "click"], [1, "modal", 3, "click"], [1, "sub"], [1, "form-row"], [1, "label"], ["maxlength", "20", 1, "input", 3, "ngModelChange", "ngModel"], ["type", "date", 1, "input", 3, "ngModelChange", "ngModel"], ["required", "", 1, "input", 3, "ngModelChange", "ngModel"], ["maxlength", "30", 1, "input", 3, "ngModelChange", "ngModel"], ["type", "email", 1, "input", 3, "ngModelChange", "ngModel"], [1, "full"], [1, "input", 3, "ngModelChange", "ngModel"], [1, "modal-actions"]], template: function PacientesComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Pacientes");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "button", 2);
            i0.ɵɵlistener("click", function PacientesComponent_Template_button_click_4_listener() { return ctx.nuevo(); });
            i0.ɵɵtext(5, "+ Nuevo paciente");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(6, "div", 3)(7, "input", 4);
            i0.ɵɵtwoWayListener("ngModelChange", function PacientesComponent_Template_input_ngModelChange_7_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.q, $event) || (ctx.q = $event); return $event; });
            i0.ɵɵlistener("keyup.enter", function PacientesComponent_Template_input_keyup_enter_7_listener() { return ctx.cargar(0); });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(8, "select", 5);
            i0.ɵɵtwoWayListener("ngModelChange", function PacientesComponent_Template_select_ngModelChange_8_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.estado, $event) || (ctx.estado = $event); return $event; });
            i0.ɵɵlistener("change", function PacientesComponent_Template_select_change_8_listener() { return ctx.cargar(0); });
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
            i0.ɵɵlistener("click", function PacientesComponent_Template_button_click_15_listener() { return ctx.cargar(0); });
            i0.ɵɵtext(16, "Buscar");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(17, "button", 10);
            i0.ɵɵlistener("click", function PacientesComponent_Template_button_click_17_listener() { return ctx.cargar(ctx.page); });
            i0.ɵɵtext(18, "\u21BB Recargar");
            i0.ɵɵelementEnd()();
            i0.ɵɵconditionalCreate(19, PacientesComponent_Conditional_19_Template, 2, 1, "div", 11);
            i0.ɵɵelementStart(20, "table", 12)(21, "thead")(22, "tr")(23, "th");
            i0.ɵɵtext(24, "ID");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(25, "th");
            i0.ɵɵtext(26, "C\u00E9dula");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(27, "th");
            i0.ɵɵtext(28, "Nombre");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(29, "th");
            i0.ɵɵtext(30, "Tel\u00E9fono");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(31, "th");
            i0.ɵɵtext(32, "Email");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(33, "th");
            i0.ɵɵtext(34, "Estado");
            i0.ɵɵelementEnd();
            i0.ɵɵelement(35, "th");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(36, "tbody");
            i0.ɵɵconditionalCreate(37, PacientesComponent_Conditional_37_Template, 4, 0, "tr");
            i0.ɵɵrepeaterCreate(38, PacientesComponent_For_39_Template, 19, 9, "tr", null, _forTrack0, false, PacientesComponent_ForEmpty_40_Template, 3, 0, "tr");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(41, "div", 13)(42, "button", 14);
            i0.ɵɵlistener("click", function PacientesComponent_Template_button_click_42_listener() { return ctx.cargar(ctx.page - 1); });
            i0.ɵɵtext(43, "Anterior");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(44, "span", 15);
            i0.ɵɵtext(45);
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(46, "button", 14);
            i0.ɵɵlistener("click", function PacientesComponent_Template_button_click_46_listener() { return ctx.cargar(ctx.page + 1); });
            i0.ɵɵtext(47, "Siguiente");
            i0.ɵɵelementEnd()()();
            i0.ɵɵconditionalCreate(48, PacientesComponent_Conditional_48_Template, 44, 9, "div", 16);
        } if (rf & 2) {
            i0.ɵɵadvance(7);
            i0.ɵɵtwoWayProperty("ngModel", ctx.q);
            i0.ɵɵcontrol();
            i0.ɵɵadvance();
            i0.ɵɵtwoWayProperty("ngModel", ctx.estado);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(9);
            i0.ɵɵproperty("disabled", ctx.cargando);
            i0.ɵɵadvance(2);
            i0.ɵɵconditional(ctx.error ? 19 : -1);
            i0.ɵɵadvance(18);
            i0.ɵɵconditional(ctx.cargando ? 37 : -1);
            i0.ɵɵadvance();
            i0.ɵɵrepeater(ctx.items);
            i0.ɵɵadvance(4);
            i0.ɵɵproperty("disabled", ctx.page <= 0);
            i0.ɵɵadvance(3);
            i0.ɵɵtextInterpolate2("P\u00E1gina ", ctx.page + 1, " de ", ctx.totalPages);
            i0.ɵɵadvance();
            i0.ɵɵproperty("disabled", ctx.page + 1 >= ctx.totalPages);
            i0.ɵɵadvance(2);
            i0.ɵɵconditional(ctx.showForm ? 48 : -1);
        } }, dependencies: [CommonModule, i2.NgClass, FormsModule, i3.NgSelectOption, i3.ɵNgSelectMultipleOption, i3.DefaultValueAccessor, i3.SelectControlValueAccessor, i3.NgControlStatus, i3.RequiredValidator, i3.MaxLengthValidator, i3.NgModel], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(PacientesComponent, [{
        type: Component,
        args: [{
                selector: 'app-pacientes',
                template: `
    <div class="card">
      <div class="between">
        <h2>Pacientes</h2>
        <button class="btn primary" (click)="nuevo()">+ Nuevo paciente</button>
      </div>
      <div class="toolbar">
        <input class="input" style="width:260px" placeholder="Buscar por nombre, cédula o teléfono" [(ngModel)]="q" (keyup.enter)="cargar(0)" />
        <select class="input" style="width:150px" [(ngModel)]="estado" (change)="cargar(0)">
          <option value="">Todos los estados</option>
          <option value="ACTIVO">Activos</option>
          <option value="INACTIVO">Inactivos</option>
        </select>
        <button class="btn" (click)="cargar(0)">Buscar</button>
        <button class="btn" (click)="cargar(page)" [disabled]="cargando" title="Recargar lista">↻ Recargar</button>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead>
          <tr><th>ID</th><th>Cédula</th><th>Nombre</th><th>Teléfono</th><th>Email</th><th>Estado</th><th></th></tr>
        </thead>
        <tbody>
          @if (cargando) {
            <tr><td colspan="7" class="empty"><span class="spinner"></span> Cargando pacientes…</td></tr>
          }
          @for (p of items; track p.id) {
            <tr>
              <td>{{ p.id }}</td>
              <td>{{ p.cedula || '-' }}</td>
              <td>{{ p.nombres }} {{ p.apellidos }}</td>
              <td>{{ p.telefono || '-' }}</td>
              <td>{{ p.email || '-' }}</td>
              <td><span class="badge" [ngClass]="p.estado === 'ACTIVO' ? 'ok' : 'bad'">{{ p.estado }}</span></td>
              <td class="flex">
                <button class="btn small" (click)="editar(p)">Editar</button>
                <button class="btn small danger" (click)="desactivar(p)" [disabled]="p.estado !== 'ACTIVO'">Desactivar</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="empty">Sin pacientes registrados.</td></tr>
          }
        </tbody>
      </table>
      <div class="between" style="margin-top:10px">
        <button class="btn small" (click)="cargar(page - 1)" [disabled]="page <= 0">Anterior</button>
        <span class="muted">Página {{ page + 1 }} de {{ totalPages }}</span>
        <button class="btn small" (click)="cargar(page + 1)" [disabled]="page + 1 >= totalPages">Siguiente</button>
      </div>
    </div>

    @if (showForm) {
      <div class="modal-backdrop" (click)="close($event)">
        <div class="modal" (click)="stop($event)">
          <h2>{{ form.id ? 'Editar paciente' : 'Nuevo paciente' }}</h2>
          <div class="sub">Los campos con * son obligatorios.</div>
          <div class="form-row">
            <div><label class="label">Cédula</label><input class="input" maxlength="20" [(ngModel)]="form.cedula" /></div>
            <div><label class="label">Fecha nacimiento</label><input class="input" type="date" [(ngModel)]="form.fechaNacimiento" /></div>
            <div><label class="label">Nombres *</label><input class="input" [(ngModel)]="form.nombres" required /></div>
            <div><label class="label">Apellidos *</label><input class="input" [(ngModel)]="form.apellidos" required /></div>
            <div><label class="label">Teléfono</label><input class="input" maxlength="30" [(ngModel)]="form.telefono" /></div>
            <div><label class="label">Email</label><input class="input" type="email" [(ngModel)]="form.email" /></div>
            <div class="full"><label class="label">Dirección</label><input class="input" [(ngModel)]="form.direccion" /></div>
            <div class="full"><label class="label">Observaciones</label><textarea class="input" [(ngModel)]="form.observaciones"></textarea></div>
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
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(PacientesComponent, { className: "PacientesComponent", filePath: "src/app/pacientes/pacientes.ts", lineNumber: 86 }); })();
