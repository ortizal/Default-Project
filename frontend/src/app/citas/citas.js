import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
import * as i3 from "@angular/forms";
const _forTrack0 = ($index, $item) => $item.id;
function CitasComponent_For_23_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "option", 12);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const o_r1 = ctx.$implicit;
    i0.ɵɵproperty("value", o_r1.id);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate2("", o_r1.nombres, " ", o_r1.apellidos);
} }
function CitasComponent_Conditional_29_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 16);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r1.error);
} }
function CitasComponent_Conditional_48_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 20);
    i0.ɵɵelement(2, "span", 21);
    i0.ɵɵtext(3, " Cargando\u2026");
    i0.ɵɵelementEnd()();
} }
function CitasComponent_For_50_Conditional_14_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "span", 3);
    i0.ɵɵtext(1, "\u2713");
    i0.ɵɵelementEnd();
} }
function CitasComponent_For_50_Conditional_16_Template(rf, ctx) { if (rf & 1) {
    const _r3 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "button", 26);
    i0.ɵɵlistener("click", function CitasComponent_For_50_Conditional_16_Template_button_click_0_listener() { i0.ɵɵrestoreView(_r3); const c_r4 = i0.ɵɵnextContext().$implicit; const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.confirmar(c_r4)); });
    i0.ɵɵtext(1, "Confirmar");
    i0.ɵɵelementEnd();
} }
function CitasComponent_For_50_Conditional_17_Template(rf, ctx) { if (rf & 1) {
    const _r5 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "button", 26);
    i0.ɵɵlistener("click", function CitasComponent_For_50_Conditional_17_Template_button_click_0_listener() { i0.ɵɵrestoreView(_r5); const c_r4 = i0.ɵɵnextContext().$implicit; const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.atender(c_r4)); });
    i0.ɵɵtext(1, "Atender");
    i0.ɵɵelementEnd();
} }
function CitasComponent_For_50_Conditional_18_Template(rf, ctx) { if (rf & 1) {
    const _r6 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "button", 27);
    i0.ɵɵlistener("click", function CitasComponent_For_50_Conditional_18_Template_button_click_0_listener() { i0.ɵɵrestoreView(_r6); const c_r4 = i0.ɵɵnextContext().$implicit; const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.cancelar(c_r4)); });
    i0.ɵɵtext(1, "Cancelar");
    i0.ɵɵelementEnd();
} }
function CitasComponent_For_50_Conditional_19_Template(rf, ctx) { if (rf & 1) {
    const _r7 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "button", 26);
    i0.ɵɵlistener("click", function CitasComponent_For_50_Conditional_19_Template_button_click_0_listener() { i0.ɵɵrestoreView(_r7); const c_r4 = i0.ɵɵnextContext().$implicit; const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.noAsistio(c_r4)); });
    i0.ɵɵtext(1, "No asisti\u00F3");
    i0.ɵɵelementEnd();
} }
function CitasComponent_For_50_Template(rf, ctx) { if (rf & 1) {
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
    i0.ɵɵelementStart(11, "td")(12, "span", 22);
    i0.ɵɵtext(13);
    i0.ɵɵconditionalCreate(14, CitasComponent_For_50_Conditional_14_Template, 2, 0, "span", 3);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(15, "td", 23);
    i0.ɵɵconditionalCreate(16, CitasComponent_For_50_Conditional_16_Template, 2, 0, "button", 24);
    i0.ɵɵconditionalCreate(17, CitasComponent_For_50_Conditional_17_Template, 2, 0, "button", 24);
    i0.ɵɵconditionalCreate(18, CitasComponent_For_50_Conditional_18_Template, 2, 0, "button", 25);
    i0.ɵɵconditionalCreate(19, CitasComponent_For_50_Conditional_19_Template, 2, 0, "button", 24);
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const c_r4 = ctx.$implicit;
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r4.fecha);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate2("", c_r4.horaInicio, "\u2013", c_r4.horaFin);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r4.pacienteNombre);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r4.servicioNombre);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r4.doctorNombre);
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", ctx_r1.badge(c_r4.estado));
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(c_r4.estado);
    i0.ɵɵadvance();
    i0.ɵɵconditional(c_r4.confirmada ? 14 : -1);
    i0.ɵɵadvance(2);
    i0.ɵɵconditional(c_r4.estado === "PENDIENTE" || c_r4.estado === "CONFIRMADA" ? 16 : -1);
    i0.ɵɵadvance();
    i0.ɵɵconditional(c_r4.estado === "CONFIRMADA" ? 17 : -1);
    i0.ɵɵadvance();
    i0.ɵɵconditional(c_r4.estado === "PENDIENTE" || c_r4.estado === "CONFIRMADA" ? 18 : -1);
    i0.ɵɵadvance();
    i0.ɵɵconditional(c_r4.estado === "CONFIRMADA" ? 19 : -1);
} }
function CitasComponent_ForEmpty_51_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 20);
    i0.ɵɵtext(2, "Sin citas que coincidan.");
    i0.ɵɵelementEnd()();
} }
export class CitasComponent {
    api;
    items = [];
    odontologos = [];
    cargando = false;
    desde = new Date().toISOString().slice(0, 10);
    hasta = '';
    estado = '';
    doctorId = '';
    page = 0;
    size = 15;
    totalPages = 1;
    error = '';
    constructor(api) {
        this.api = api;
        this.cargar(0);
        this.api.get('/odontologos/activos').subscribe((r) => (this.odontologos = r));
    }
    cargar(p) {
        this.cargando = true;
        this.error = '';
        const params = new URLSearchParams({ page: String(p), size: String(this.size), desde: this.desde || '', hasta: this.hasta || '' });
        if (this.estado)
            params.set('estado', this.estado);
        if (this.doctorId)
            params.set('doctorId', this.doctorId);
        this.api.get(`/citas?${params.toString()}`).subscribe({
            next: (r) => {
                this.cargando = false;
                this.items = r.content;
                this.totalPages = Math.max(r.totalPages ?? 1, 1);
                this.page = Math.min(p, this.totalPages - 1);
            },
            error: (e) => {
                this.error = this.msg(e);
                this.cargando = false;
            },
        });
    }
    confirmar(c) {
        this.api.post(`/citas/${c.id}/confirmar`, {}).subscribe({
            next: () => this.cargar(this.page),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    atender(c) {
        this.api.post(`/citas/${c.id}/atender`, {}).subscribe({
            next: () => this.cargar(this.page),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    cancelar(c) {
        const motivo = prompt('Motivo de cancelación');
        if (motivo === null)
            return;
        this.api.post(`/citas/${c.id}/cancelar`, { motivo }).subscribe({
            next: () => this.cargar(this.page),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    noAsistio(c) {
        if (!confirm('¿Registrar como no asistió?'))
            return;
        this.api.post(`/citas/${c.id}/no-asistio`, {}).subscribe({
            next: () => this.cargar(this.page),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    badge(estado) {
        switch (estado) {
            case 'CONFIRMADA':
                return 'ok';
            case 'ATENDIDA':
                return 'info';
            case 'CANCELADA':
                return 'bad';
            case 'NO_ASISTIO':
                return 'warn';
            default:
                return 'dim';
        }
    }
    msg(e) {
        const a = e;
        if (a?.status === 409 || a?.status === 400)
            return a.error?.message ?? 'Datos inválidos';
        if (a?.status === 403)
            return 'Sin permisos para esta acción';
        return 'Error de conexión';
    }
    static ɵfac = function CitasComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || CitasComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: CitasComponent, selectors: [["app-citas"]], decls: 59, vars: 11, consts: [[1, "card"], [1, "toolbar"], ["type", "date", 1, "input", 2, "width", "160px", 3, "ngModelChange", "change", "ngModel"], [1, "muted"], [1, "input", 2, "width", "170px", 3, "ngModelChange", "change", "ngModel"], ["value", ""], ["value", "PENDIENTE"], ["value", "CONFIRMADA"], ["value", "ATENDIDA"], ["value", "CANCELADA"], ["value", "NO_ASISTIO"], [1, "input", 2, "width", "220px", 3, "ngModelChange", "change", "ngModel"], [3, "value"], [1, "btn", 3, "click"], ["title", "Refrescar", "aria-label", "Refrescar listado", 1, "btn", "icon-only", 3, "click"], ["aria-hidden", "true"], [1, "msg", "error"], [1, "tbl"], [1, "between", 2, "margin-top", "10px"], [1, "btn", "small", 3, "click", "disabled"], ["colspan", "7", 1, "empty"], [1, "spinner"], [1, "badge", 3, "ngClass"], [1, "flex"], [1, "btn", "small"], [1, "btn", "small", "danger"], [1, "btn", "small", 3, "click"], [1, "btn", "small", "danger", 3, "click"]], template: function CitasComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "input", 2);
            i0.ɵɵtwoWayListener("ngModelChange", function CitasComponent_Template_input_ngModelChange_2_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.desde, $event) || (ctx.desde = $event); return $event; });
            i0.ɵɵlistener("change", function CitasComponent_Template_input_change_2_listener() { return ctx.cargar(0); });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(3, "span", 3);
            i0.ɵɵtext(4, "a");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(5, "input", 2);
            i0.ɵɵtwoWayListener("ngModelChange", function CitasComponent_Template_input_ngModelChange_5_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.hasta, $event) || (ctx.hasta = $event); return $event; });
            i0.ɵɵlistener("change", function CitasComponent_Template_input_change_5_listener() { return ctx.cargar(0); });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(6, "select", 4);
            i0.ɵɵtwoWayListener("ngModelChange", function CitasComponent_Template_select_ngModelChange_6_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.estado, $event) || (ctx.estado = $event); return $event; });
            i0.ɵɵlistener("change", function CitasComponent_Template_select_change_6_listener() { return ctx.cargar(0); });
            i0.ɵɵelementStart(7, "option", 5);
            i0.ɵɵtext(8, "Todos los estados");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(9, "option", 6);
            i0.ɵɵtext(10, "Pendientes");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(11, "option", 7);
            i0.ɵɵtext(12, "Confirmadas");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(13, "option", 8);
            i0.ɵɵtext(14, "Atendidas");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(15, "option", 9);
            i0.ɵɵtext(16, "Canceladas");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(17, "option", 10);
            i0.ɵɵtext(18, "No asistieron");
            i0.ɵɵelementEnd()();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(19, "select", 11);
            i0.ɵɵtwoWayListener("ngModelChange", function CitasComponent_Template_select_ngModelChange_19_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.doctorId, $event) || (ctx.doctorId = $event); return $event; });
            i0.ɵɵlistener("change", function CitasComponent_Template_select_change_19_listener() { return ctx.cargar(0); });
            i0.ɵɵelementStart(20, "option", 5);
            i0.ɵɵtext(21, "Todos los odont\u00F3logos");
            i0.ɵɵelementEnd();
            i0.ɵɵrepeaterCreate(22, CitasComponent_For_23_Template, 2, 3, "option", 12, _forTrack0);
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(24, "button", 13);
            i0.ɵɵlistener("click", function CitasComponent_Template_button_click_24_listener() { return ctx.cargar(0); });
            i0.ɵɵtext(25, "Filtrar");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(26, "button", 14);
            i0.ɵɵlistener("click", function CitasComponent_Template_button_click_26_listener() { return ctx.cargar(ctx.page); });
            i0.ɵɵelementStart(27, "span", 15);
            i0.ɵɵtext(28, "\u21BB");
            i0.ɵɵelementEnd()()();
            i0.ɵɵconditionalCreate(29, CitasComponent_Conditional_29_Template, 2, 1, "div", 16);
            i0.ɵɵelementStart(30, "table", 17)(31, "thead")(32, "tr")(33, "th");
            i0.ɵɵtext(34, "Fecha");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(35, "th");
            i0.ɵɵtext(36, "Hora");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(37, "th");
            i0.ɵɵtext(38, "Paciente");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(39, "th");
            i0.ɵɵtext(40, "Servicio");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(41, "th");
            i0.ɵɵtext(42, "Odont\u00F3logo");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(43, "th");
            i0.ɵɵtext(44, "Estado");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(45, "th");
            i0.ɵɵtext(46, "Acciones");
            i0.ɵɵelementEnd()()();
            i0.ɵɵelementStart(47, "tbody");
            i0.ɵɵconditionalCreate(48, CitasComponent_Conditional_48_Template, 4, 0, "tr");
            i0.ɵɵrepeaterCreate(49, CitasComponent_For_50_Template, 20, 13, "tr", null, _forTrack0, false, CitasComponent_ForEmpty_51_Template, 3, 0, "tr");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(52, "div", 18)(53, "button", 19);
            i0.ɵɵlistener("click", function CitasComponent_Template_button_click_53_listener() { return ctx.cargar(ctx.page - 1); });
            i0.ɵɵtext(54, "Anterior");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(55, "span", 3);
            i0.ɵɵtext(56);
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(57, "button", 19);
            i0.ɵɵlistener("click", function CitasComponent_Template_button_click_57_listener() { return ctx.cargar(ctx.page + 1); });
            i0.ɵɵtext(58, "Siguiente");
            i0.ɵɵelementEnd()()();
        } if (rf & 2) {
            i0.ɵɵadvance(2);
            i0.ɵɵtwoWayProperty("ngModel", ctx.desde);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(3);
            i0.ɵɵtwoWayProperty("ngModel", ctx.hasta);
            i0.ɵɵcontrol();
            i0.ɵɵadvance();
            i0.ɵɵtwoWayProperty("ngModel", ctx.estado);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(13);
            i0.ɵɵtwoWayProperty("ngModel", ctx.doctorId);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(3);
            i0.ɵɵrepeater(ctx.odontologos);
            i0.ɵɵadvance(7);
            i0.ɵɵconditional(ctx.error ? 29 : -1);
            i0.ɵɵadvance(19);
            i0.ɵɵconditional(ctx.cargando ? 48 : -1);
            i0.ɵɵadvance();
            i0.ɵɵrepeater(ctx.items);
            i0.ɵɵadvance(4);
            i0.ɵɵproperty("disabled", ctx.page <= 0);
            i0.ɵɵadvance(3);
            i0.ɵɵtextInterpolate2("P\u00E1gina ", ctx.page + 1, " de ", ctx.totalPages);
            i0.ɵɵadvance();
            i0.ɵɵproperty("disabled", ctx.page + 1 >= ctx.totalPages);
        } }, dependencies: [CommonModule, i2.NgClass, FormsModule, i3.NgSelectOption, i3.ɵNgSelectMultipleOption, i3.DefaultValueAccessor, i3.SelectControlValueAccessor, i3.NgControlStatus, i3.NgModel], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(CitasComponent, [{
        type: Component,
        args: [{
                selector: 'app-citas',
                template: `
    <div class="card">
      <div class="toolbar">
        <input class="input" style="width:160px" type="date" [(ngModel)]="desde" (change)="cargar(0)" />
        <span class="muted">a</span>
        <input class="input" style="width:160px" type="date" [(ngModel)]="hasta" (change)="cargar(0)" />
        <select class="input" style="width:170px" [(ngModel)]="estado" (change)="cargar(0)">
          <option value="">Todos los estados</option>
          <option value="PENDIENTE">Pendientes</option>
          <option value="CONFIRMADA">Confirmadas</option>
          <option value="ATENDIDA">Atendidas</option>
          <option value="CANCELADA">Canceladas</option>
          <option value="NO_ASISTIO">No asistieron</option>
        </select>
        <select class="input" style="width:220px" [(ngModel)]="doctorId" (change)="cargar(0)">
          <option value="">Todos los odontólogos</option>
          @for (o of odontologos; track o.id) {
            <option [value]="o.id">{{ o.nombres }} {{ o.apellidos }}</option>
          }
        </select>
        <button class="btn" (click)="cargar(0)">Filtrar</button>
        <button class="btn icon-only" title="Refrescar" aria-label="Refrescar listado" (click)="cargar(page)">
          <span aria-hidden="true">↻</span>
        </button>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead>
          <tr><th>Fecha</th><th>Hora</th><th>Paciente</th><th>Servicio</th><th>Odontólogo</th><th>Estado</th><th>Acciones</th></tr>
        </thead>
        <tbody>
        @if (cargando) {
          <tr><td colspan="7" class="empty"><span class="spinner"></span> Cargando…</td></tr>
        }
          @for (c of items; track c.id) {
            <tr>
              <td>{{ c.fecha }}</td>
              <td>{{ c.horaInicio }}–{{ c.horaFin }}</td>
              <td>{{ c.pacienteNombre }}</td>
              <td>{{ c.servicioNombre }}</td>
              <td>{{ c.doctorNombre }}</td>
              <td><span class="badge" [ngClass]="badge(c.estado)">{{ c.estado }}@if (c.confirmada) { <span class="muted">✓</span> }</span></td>
              <td class="flex">
                @if (c.estado === 'PENDIENTE' || c.estado === 'CONFIRMADA') {
                  <button class="btn small" (click)="confirmar(c)">Confirmar</button>
                }
                @if (c.estado === 'CONFIRMADA') {
                  <button class="btn small" (click)="atender(c)">Atender</button>
                }
                @if (c.estado === 'PENDIENTE' || c.estado === 'CONFIRMADA') {
                  <button class="btn small danger" (click)="cancelar(c)">Cancelar</button>
                }
                @if (c.estado === 'CONFIRMADA') {
                  <button class="btn small" (click)="noAsistio(c)">No asistió</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="empty">Sin citas que coincidan.</td></tr>
          }
        </tbody>
      </table>
      <div class="between" style="margin-top:10px">
        <button class="btn small" (click)="cargar(page - 1)" [disabled]="page <= 0">Anterior</button>
        <span class="muted">Página {{ page + 1 }} de {{ totalPages }}</span>
        <button class="btn small" (click)="cargar(page + 1)" [disabled]="page + 1 >= totalPages">Siguiente</button>
      </div>
    </div>
  `,
                imports: [CommonModule, FormsModule],
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(CitasComponent, { className: "CitasComponent", filePath: "src/app/citas/citas.ts", lineNumber: 82 }); })();
