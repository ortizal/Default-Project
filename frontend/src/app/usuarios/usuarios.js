import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
import * as i3 from "@angular/forms";
const _forTrack0 = ($index, $item) => $item.id;
function UsuariosComponent_Conditional_10_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 6);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function UsuariosComponent_For_31_For_13_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "span", 12);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const r_r3 = ctx.$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(r_r3);
} }
function UsuariosComponent_For_31_Template(rf, ctx) { if (rf & 1) {
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
    i0.ɵɵelementStart(11, "td");
    i0.ɵɵrepeaterCreate(12, UsuariosComponent_For_31_For_13_Template, 2, 1, "span", 12, i0.ɵɵrepeaterTrackByIdentity);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(14, "td")(15, "span", 13);
    i0.ɵɵtext(16);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(17, "td", 14)(18, "button", 15);
    i0.ɵɵlistener("click", function UsuariosComponent_For_31_Template_button_click_18_listener() { const u_r4 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.editar(u_r4)); });
    i0.ɵɵtext(19, "Editar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(20, "button", 16);
    i0.ɵɵlistener("click", function UsuariosComponent_For_31_Template_button_click_20_listener() { const u_r4 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.cambiarEstado(u_r4)); });
    i0.ɵɵtext(21);
    i0.ɵɵelementEnd()()();
} if (rf & 2) {
    const u_r4 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(u_r4.id);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(u_r4.username);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate2("", u_r4.nombres, " ", u_r4.apellidos);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(u_r4.email);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(u_r4.telefono || "-");
    i0.ɵɵadvance(2);
    i0.ɵɵrepeater(u_r4.roles);
    i0.ɵɵadvance(3);
    i0.ɵɵproperty("ngClass", u_r4.estado === "ACTIVO" ? "ok" : "bad");
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(u_r4.estado);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(u_r4.estado === "ACTIVO" ? "Desactivar" : "Activar");
} }
function UsuariosComponent_ForEmpty_32_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 17);
    i0.ɵɵtext(2, "Sin usuarios.");
    i0.ɵɵelementEnd()();
} }
function UsuariosComponent_Conditional_40_Conditional_21_Template(rf, ctx) { if (rf & 1) {
    const _r6 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 25)(1, "label", 21);
    i0.ɵɵtext(2, "Contrase\u00F1a *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "input", 30);
    i0.ɵɵtwoWayListener("ngModelChange", function UsuariosComponent_Conditional_40_Conditional_21_Template_input_ngModelChange_3_listener($event) { i0.ɵɵrestoreView(_r6); const ctx_r0 = i0.ɵɵnextContext(2); i0.ɵɵtwoWayBindingSet(ctx_r0.form.password, $event) || (ctx_r0.form.password = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext(2);
    i0.ɵɵadvance(3);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.password);
    i0.ɵɵcontrol();
} }
function UsuariosComponent_Conditional_40_For_31_Template(rf, ctx) { if (rf & 1) {
    const _r7 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "label", 28)(1, "input", 31);
    i0.ɵɵlistener("change", function UsuariosComponent_Conditional_40_For_31_Template_input_change_1_listener() { const r_r8 = i0.ɵɵrestoreView(_r7).$implicit; const ctx_r0 = i0.ɵɵnextContext(2); return i0.ɵɵresetView(ctx_r0.toggleRol(r_r8)); });
    i0.ɵɵelementEnd();
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const r_r8 = ctx.$implicit;
    const ctx_r0 = i0.ɵɵnextContext(2);
    i0.ɵɵadvance();
    i0.ɵɵproperty("checked", ctx_r0.rolMarcado(r_r8));
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate1(" ", r_r8, " ");
} }
function UsuariosComponent_Conditional_40_Template(rf, ctx) { if (rf & 1) {
    const _r5 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 18);
    i0.ɵɵlistener("click", function UsuariosComponent_Conditional_40_Template_div_click_0_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.close($event)); });
    i0.ɵɵelementStart(1, "div", 19);
    i0.ɵɵlistener("click", function UsuariosComponent_Conditional_40_Template_div_click_1_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.stop($event)); });
    i0.ɵɵelementStart(2, "h2");
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "div", 20)(5, "div")(6, "label", 21);
    i0.ɵɵtext(7, "Usuario *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(8, "input", 22);
    i0.ɵɵtwoWayListener("ngModelChange", function UsuariosComponent_Conditional_40_Template_input_ngModelChange_8_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.username, $event) || (ctx_r0.form.username = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(9, "div")(10, "label", 21);
    i0.ɵɵtext(11, "Email *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(12, "input", 23);
    i0.ɵɵtwoWayListener("ngModelChange", function UsuariosComponent_Conditional_40_Template_input_ngModelChange_12_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.email, $event) || (ctx_r0.form.email = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(13, "div")(14, "label", 21);
    i0.ɵɵtext(15, "Nombres *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(16, "input", 24);
    i0.ɵɵtwoWayListener("ngModelChange", function UsuariosComponent_Conditional_40_Template_input_ngModelChange_16_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.nombres, $event) || (ctx_r0.form.nombres = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(17, "div")(18, "label", 21);
    i0.ɵɵtext(19, "Apellidos *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(20, "input", 24);
    i0.ɵɵtwoWayListener("ngModelChange", function UsuariosComponent_Conditional_40_Template_input_ngModelChange_20_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.apellidos, $event) || (ctx_r0.form.apellidos = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵconditionalCreate(21, UsuariosComponent_Conditional_40_Conditional_21_Template, 4, 1, "div", 25);
    i0.ɵɵelementStart(22, "div", 25)(23, "label", 21);
    i0.ɵɵtext(24, "Tel\u00E9fono");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(25, "input", 26);
    i0.ɵɵtwoWayListener("ngModelChange", function UsuariosComponent_Conditional_40_Template_input_ngModelChange_25_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r0.form.telefono, $event) || (ctx_r0.form.telefono = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(26, "div", 25)(27, "label", 21);
    i0.ɵɵtext(28, "Roles *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(29, "div", 27);
    i0.ɵɵrepeaterCreate(30, UsuariosComponent_Conditional_40_For_31_Template, 3, 2, "label", 28, i0.ɵɵrepeaterTrackByIdentity);
    i0.ɵɵelementEnd()()();
    i0.ɵɵelementStart(32, "div", 29)(33, "button", 5);
    i0.ɵɵlistener("click", function UsuariosComponent_Conditional_40_Template_button_click_33_listener() { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.showForm = false); });
    i0.ɵɵtext(34, "Cancelar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(35, "button", 2);
    i0.ɵɵlistener("click", function UsuariosComponent_Conditional_40_Template_button_click_35_listener() { i0.ɵɵrestoreView(_r5); const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.guardar()); });
    i0.ɵɵtext(36, "Guardar");
    i0.ɵɵelementEnd()()()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(ctx_r0.form.id ? "Editar usuario" : "Nuevo usuario");
    i0.ɵɵadvance(5);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.username);
    i0.ɵɵproperty("readonly", !!ctx_r0.form.id);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.email);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.nombres);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.apellidos);
    i0.ɵɵcontrol();
    i0.ɵɵadvance();
    i0.ɵɵconditional(!ctx_r0.form.id ? 21 : -1);
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r0.form.telefono);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(5);
    i0.ɵɵrepeater(ctx_r0.ROLES);
} }
export class UsuariosComponent {
    api;
    ROLES = ['SUPER_ADMIN', 'ADMIN', 'RECEPCION', 'ODONTOLOGO'];
    items = [];
    q = '';
    page = 0;
    size = 15;
    totalPages = 1;
    error = '';
    showForm = false;
    form = {};
    rolesSel = [];
    constructor(api) {
        this.api = api;
        this.cargar(0);
    }
    cargar(p) {
        this.error = '';
        const params = new URLSearchParams({ page: String(p), size: String(this.size) });
        if (this.q.trim())
            params.set('q', this.q.trim());
        this.api.get(`/usuarios?${params.toString()}`).subscribe({
            next: (r) => {
                this.items = r.content;
                this.totalPages = Math.max(r.totalPages ?? 1, 1);
                this.page = Math.min(p, this.totalPages - 1);
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    nuevo() {
        this.form = { username: '', email: '', nombres: '', apellidos: '', telefono: '', password: '' };
        this.rolesSel = [];
        this.showForm = true;
    }
    editar(u) {
        this.form = { ...u };
        this.rolesSel = [...u.roles];
        this.showForm = true;
    }
    rolMarcado(r) {
        return this.rolesSel.includes(r);
    }
    toggleRol(r) {
        this.rolesSel = this.rolesSel.includes(r) ? this.rolesSel.filter((x) => x !== r) : [...this.rolesSel, r];
    }
    guardar() {
        if (!this.form.username || !this.form.email || !this.form.nombres || !this.form.apellidos || this.rolesSel.length === 0)
            return;
        if (!this.form.id && !this.form.password)
            return;
        const base = {
            email: this.form.email,
            telefono: this.form.telefono || null,
            nombres: this.form.nombres,
            apellidos: this.form.apellidos,
            roles: this.rolesSel,
        };
        const req = this.form.id
            ? this.api.put(`/usuarios/${this.form.id}`, base)
            : this.api.post('/usuarios', {
                username: this.form.username,
                password: this.form.password,
                ...base,
            });
        req.subscribe({
            next: () => {
                this.showForm = false;
                this.cargar(this.page);
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    cambiarEstado(u) {
        this.api.post(`/usuarios/${u.id}/estado`, {}).subscribe({
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
        if (a?.status === 403)
            return 'Sin permisos para gestionar usuarios';
        return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
    }
    static ɵfac = function UsuariosComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || UsuariosComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: UsuariosComponent, selectors: [["app-usuarios"]], decls: 41, vars: 8, consts: [[1, "card"], [1, "between"], [1, "btn", "primary", 3, "click"], [1, "toolbar"], ["placeholder", "Buscar por usuario, nombre o email", 1, "input", 2, "width", "260px", 3, "ngModelChange", "keyup.enter", "ngModel"], [1, "btn", 3, "click"], [1, "msg", "error"], [1, "tbl"], [1, "between", 2, "margin-top", "10px"], [1, "btn", "small", 3, "click", "disabled"], [1, "muted"], [1, "modal-backdrop"], [1, "badge", "info", 2, "margin-right", "3px"], [1, "badge", 3, "ngClass"], [1, "flex"], [1, "btn", "small", 3, "click"], [1, "btn", "small", "danger", 3, "click"], ["colspan", "8", 1, "empty"], [1, "modal-backdrop", 3, "click"], [1, "modal", 3, "click"], [1, "form-row"], [1, "label"], ["required", "", 1, "input", 3, "ngModelChange", "ngModel", "readonly"], ["type", "email", "required", "", 1, "input", 3, "ngModelChange", "ngModel"], ["required", "", 1, "input", 3, "ngModelChange", "ngModel"], [1, "full"], [1, "input", 3, "ngModelChange", "ngModel"], [1, "flex", 2, "flex-wrap", "wrap"], [2, "margin-right", "10px", "font-size", "13px"], [1, "modal-actions"], ["type", "password", "required", "", 1, "input", 3, "ngModelChange", "ngModel"], ["type", "checkbox", 3, "change", "checked"]], template: function UsuariosComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Usuarios");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "button", 2);
            i0.ɵɵlistener("click", function UsuariosComponent_Template_button_click_4_listener() { return ctx.nuevo(); });
            i0.ɵɵtext(5, "+ Nuevo usuario");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(6, "div", 3)(7, "input", 4);
            i0.ɵɵtwoWayListener("ngModelChange", function UsuariosComponent_Template_input_ngModelChange_7_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.q, $event) || (ctx.q = $event); return $event; });
            i0.ɵɵlistener("keyup.enter", function UsuariosComponent_Template_input_keyup_enter_7_listener() { return ctx.cargar(0); });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(8, "button", 5);
            i0.ɵɵlistener("click", function UsuariosComponent_Template_button_click_8_listener() { return ctx.cargar(0); });
            i0.ɵɵtext(9, "Buscar");
            i0.ɵɵelementEnd()();
            i0.ɵɵconditionalCreate(10, UsuariosComponent_Conditional_10_Template, 2, 1, "div", 6);
            i0.ɵɵelementStart(11, "table", 7)(12, "thead")(13, "tr")(14, "th");
            i0.ɵɵtext(15, "ID");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(16, "th");
            i0.ɵɵtext(17, "Usuario");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(18, "th");
            i0.ɵɵtext(19, "Nombres");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(20, "th");
            i0.ɵɵtext(21, "Email");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(22, "th");
            i0.ɵɵtext(23, "Tel\u00E9fono");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(24, "th");
            i0.ɵɵtext(25, "Roles");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(26, "th");
            i0.ɵɵtext(27, "Estado");
            i0.ɵɵelementEnd();
            i0.ɵɵelement(28, "th");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(29, "tbody");
            i0.ɵɵrepeaterCreate(30, UsuariosComponent_For_31_Template, 22, 9, "tr", null, _forTrack0, false, UsuariosComponent_ForEmpty_32_Template, 3, 0, "tr");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(33, "div", 8)(34, "button", 9);
            i0.ɵɵlistener("click", function UsuariosComponent_Template_button_click_34_listener() { return ctx.cargar(ctx.page - 1); });
            i0.ɵɵtext(35, "Anterior");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(36, "span", 10);
            i0.ɵɵtext(37);
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(38, "button", 9);
            i0.ɵɵlistener("click", function UsuariosComponent_Template_button_click_38_listener() { return ctx.cargar(ctx.page + 1); });
            i0.ɵɵtext(39, "Siguiente");
            i0.ɵɵelementEnd()()();
            i0.ɵɵconditionalCreate(40, UsuariosComponent_Conditional_40_Template, 37, 8, "div", 11);
        } if (rf & 2) {
            i0.ɵɵadvance(7);
            i0.ɵɵtwoWayProperty("ngModel", ctx.q);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(3);
            i0.ɵɵconditional(ctx.error ? 10 : -1);
            i0.ɵɵadvance(20);
            i0.ɵɵrepeater(ctx.items);
            i0.ɵɵadvance(4);
            i0.ɵɵproperty("disabled", ctx.page <= 0);
            i0.ɵɵadvance(3);
            i0.ɵɵtextInterpolate2("P\u00E1gina ", ctx.page + 1, " de ", ctx.totalPages);
            i0.ɵɵadvance();
            i0.ɵɵproperty("disabled", ctx.page + 1 >= ctx.totalPages);
            i0.ɵɵadvance(2);
            i0.ɵɵconditional(ctx.showForm ? 40 : -1);
        } }, dependencies: [CommonModule, i2.NgClass, FormsModule, i3.DefaultValueAccessor, i3.NgControlStatus, i3.RequiredValidator, i3.NgModel], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(UsuariosComponent, [{
        type: Component,
        args: [{
                selector: 'app-usuarios',
                template: `
    <div class="card">
      <div class="between">
        <h2>Usuarios</h2>
        <button class="btn primary" (click)="nuevo()">+ Nuevo usuario</button>
      </div>
      <div class="toolbar">
        <input class="input" style="width:260px" placeholder="Buscar por usuario, nombre o email" [(ngModel)]="q" (keyup.enter)="cargar(0)" />
        <button class="btn" (click)="cargar(0)">Buscar</button>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead><tr><th>ID</th><th>Usuario</th><th>Nombres</th><th>Email</th><th>Teléfono</th><th>Roles</th><th>Estado</th><th></th></tr></thead>
        <tbody>
          @for (u of items; track u.id) {
            <tr>
              <td>{{ u.id }}</td>
              <td>{{ u.username }}</td>
              <td>{{ u.nombres }} {{ u.apellidos }}</td>
              <td>{{ u.email }}</td>
              <td>{{ u.telefono || '-' }}</td>
              <td>
                @for (r of u.roles; track r) {
                  <span class="badge info" style="margin-right:3px">{{ r }}</span>
                }
              </td>
              <td><span class="badge" [ngClass]="u.estado === 'ACTIVO' ? 'ok' : 'bad'">{{ u.estado }}</span></td>
              <td class="flex">
                <button class="btn small" (click)="editar(u)">Editar</button>
                <button class="btn small danger" (click)="cambiarEstado(u)">{{ u.estado === 'ACTIVO' ? 'Desactivar' : 'Activar' }}</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="8" class="empty">Sin usuarios.</td></tr>
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
          <h2>{{ form.id ? 'Editar usuario' : 'Nuevo usuario' }}</h2>
          <div class="form-row">
            <div><label class="label">Usuario *</label><input class="input" [(ngModel)]="form.username" required [readonly]="!!form.id" /></div>
            <div><label class="label">Email *</label><input class="input" type="email" [(ngModel)]="form.email" required /></div>
            <div><label class="label">Nombres *</label><input class="input" [(ngModel)]="form.nombres" required /></div>
            <div><label class="label">Apellidos *</label><input class="input" [(ngModel)]="form.apellidos" required /></div>
            @if (!form.id) {
              <div class="full"><label class="label">Contraseña *</label><input class="input" type="password" [(ngModel)]="form.password" required /></div>
            }
            <div class="full"><label class="label">Teléfono</label><input class="input" [(ngModel)]="form.telefono" /></div>
            <div class="full">
              <label class="label">Roles *</label>
              <div class="flex" style="flex-wrap:wrap">
                @for (r of ROLES; track r) {
                  <label style="margin-right:10px; font-size:13px">
                    <input type="checkbox" [checked]="rolMarcado(r)" (change)="toggleRol(r)" /> {{ r }}
                  </label>
                }
              </div>
            </div>
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
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(UsuariosComponent, { className: "UsuariosComponent", filePath: "src/app/usuarios/usuarios.ts", lineNumber: 89 }); })();
