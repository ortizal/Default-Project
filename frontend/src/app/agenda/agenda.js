import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CalendarioComponent } from './calendario';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
import * as i3 from "@angular/forms";
const _c0 = () => ["dia", "semana", "mes"];
const _c1 = a0 => ({ active: a0 });
const _forTrack0 = ($index, $item) => $item.id;
const _forTrack1 = ($index, $item) => $item.horaInicio;
function AgendaComponent_For_8_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "option", 5);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const o_r1 = ctx.$implicit;
    i0.ɵɵproperty("value", o_r1.id);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate2("", o_r1.nombres, " ", o_r1.apellidos);
} }
function AgendaComponent_Conditional_9_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 6);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r1.error);
} }
function AgendaComponent_For_12_Template(rf, ctx) { if (rf & 1) {
    const _r3 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "button", 11);
    i0.ɵɵlistener("click", function AgendaComponent_For_12_Template_button_click_0_listener() { const v_r4 = i0.ɵɵrestoreView(_r3).$implicit; const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.setVista(v_r4)); });
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const v_r4 = ctx.$implicit;
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵproperty("ngClass", i0.ɵɵpureFunction1(2, _c1, ctx_r1.vista === v_r4));
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate1(" ", v_r4 === "dia" ? "D\u00EDa" : v_r4 === "semana" ? "Semana" : "Mes", " ");
} }
function AgendaComponent_Conditional_13_Conditional_4_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 14);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext(2);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate1("Sin horario configurado para hoy (", ctx_r1.diario, ").");
} }
function AgendaComponent_Conditional_13_For_7_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "span", 15);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const h_r6 = ctx.$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate3("", h_r6.horaInicio, "\u2013", h_r6.horaFin, " (", h_r6.intervaloMinutos, " min)");
} }
function AgendaComponent_Conditional_13_For_27_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "td");
    i0.ɵɵtext(4);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(5, "td");
    i0.ɵɵtext(6);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(7, "td")(8, "span", 25);
    i0.ɵɵtext(9);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(10, "td")(11, "a", 26);
    i0.ɵɵtext(12, "Ver");
    i0.ɵɵelementEnd()()();
} if (rf & 2) {
    const c_r7 = ctx.$implicit;
    const ctx_r1 = i0.ɵɵnextContext(2);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate2("", c_r7.horaInicio, "\u2013", c_r7.horaFin);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r7.pacienteNombre);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r7.servicioNombre);
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", ctx_r1.badge(c_r7.estado));
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(c_r7.estado);
} }
function AgendaComponent_Conditional_13_ForEmpty_28_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 27);
    i0.ɵɵtext(2, "Sin citas este d\u00EDa.");
    i0.ɵɵelementEnd()();
} }
function AgendaComponent_Conditional_13_For_33_Template(rf, ctx) { if (rf & 1) {
    const _r8 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 1)(1, "span");
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "button", 28);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_13_For_33_Template_button_click_3_listener() { const b_r9 = i0.ɵɵrestoreView(_r8).$implicit; const ctx_r1 = i0.ɵɵnextContext(2); return i0.ɵɵresetView(ctx_r1.quitarBloqueo(b_r9)); });
    i0.ɵɵtext(4, "Quitar");
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const b_r9 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate3("", b_r9.horaInicio || "Todo el d\u00EDa", "", b_r9.horaInicio && b_r9.horaFin ? "\u2013" + b_r9.horaFin : "", " \u2014 ", b_r9.motivo || "Sin motivo");
} }
function AgendaComponent_Conditional_13_ForEmpty_34_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 14);
    i0.ɵɵtext(1, "Sin bloqueos.");
    i0.ɵɵelementEnd();
} }
function AgendaComponent_Conditional_13_For_44_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "option", 5);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const s_r10 = ctx.$implicit;
    i0.ɵɵproperty("value", s_r10.id);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate2("", s_r10.nombre, " (", s_r10.duracionMinutos, " min)");
} }
function AgendaComponent_Conditional_13_Conditional_47_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 14);
    i0.ɵɵtext(1, "Selecciona un servicio y fecha para ver los horarios libres.");
    i0.ɵɵelementEnd();
} }
function AgendaComponent_Conditional_13_For_50_Template(rf, ctx) { if (rf & 1) {
    const _r11 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "button", 16);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_13_For_50_Template_button_click_0_listener() { const s_r12 = i0.ɵɵrestoreView(_r11).$implicit; const ctx_r1 = i0.ɵɵnextContext(2); return i0.ɵɵresetView(ctx_r1.crearCitaEn(s_r12)); });
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const s_r12 = ctx.$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(s_r12.horaInicio);
} }
function AgendaComponent_Conditional_13_Template(rf, ctx) { if (rf & 1) {
    const _r5 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 12)(1, "div", 13)(2, "h3");
    i0.ɵɵtext(3, "Horarios atendidos hoy");
    i0.ɵɵelementEnd();
    i0.ɵɵconditionalCreate(4, AgendaComponent_Conditional_13_Conditional_4_Template, 2, 1, "div", 14);
    i0.ɵɵelementStart(5, "div", 2);
    i0.ɵɵrepeaterCreate(6, AgendaComponent_Conditional_13_For_7_Template, 2, 3, "span", 15, _forTrack0);
    i0.ɵɵelementStart(8, "button", 16);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_13_Template_button_click_8_listener() { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.crearBloqueo()); });
    i0.ɵɵtext(9, "+ Bloquear hora");
    i0.ɵɵelementEnd()()();
    i0.ɵɵelementStart(10, "div", 17)(11, "h3");
    i0.ɵɵtext(12, "Citas del d\u00EDa");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(13, "table", 18)(14, "thead")(15, "tr")(16, "th");
    i0.ɵɵtext(17, "Hora");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(18, "th");
    i0.ɵɵtext(19, "Paciente");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(20, "th");
    i0.ɵɵtext(21, "Servicio");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(22, "th");
    i0.ɵɵtext(23, "Estado");
    i0.ɵɵelementEnd();
    i0.ɵɵelement(24, "th");
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(25, "tbody");
    i0.ɵɵrepeaterCreate(26, AgendaComponent_Conditional_13_For_27_Template, 13, 6, "tr", null, _forTrack0, false, AgendaComponent_Conditional_13_ForEmpty_28_Template, 3, 0, "tr");
    i0.ɵɵelementEnd()()();
    i0.ɵɵelementStart(29, "div", 17)(30, "h3");
    i0.ɵɵtext(31, "Bloqueos");
    i0.ɵɵelementEnd();
    i0.ɵɵrepeaterCreate(32, AgendaComponent_Conditional_13_For_33_Template, 5, 3, "div", 1, _forTrack0, false, AgendaComponent_Conditional_13_ForEmpty_34_Template, 2, 0, "div", 14);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(35, "div", 19)(36, "div", 1)(37, "h3");
    i0.ɵɵtext(38, "Disponibilidad");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(39, "div", 2)(40, "select", 20);
    i0.ɵɵtwoWayListener("ngModelChange", function AgendaComponent_Conditional_13_Template_select_ngModelChange_40_listener($event) { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.servicioSeleccionado, $event) || (ctx_r1.servicioSeleccionado = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵlistener("change", function AgendaComponent_Conditional_13_Template_select_change_40_listener() { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.cargarSlots()); });
    i0.ɵɵelementStart(41, "option", 21);
    i0.ɵɵtext(42, "Selecciona un servicio");
    i0.ɵɵelementEnd();
    i0.ɵɵrepeaterCreate(43, AgendaComponent_Conditional_13_For_44_Template, 2, 3, "option", 5, _forTrack0);
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementStart(45, "button", 22);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_13_Template_button_click_45_listener() { i0.ɵɵrestoreView(_r5); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.cargarSlots()); });
    i0.ɵɵtext(46, "Buscar horarios libres");
    i0.ɵɵelementEnd()()();
    i0.ɵɵconditionalCreate(47, AgendaComponent_Conditional_13_Conditional_47_Template, 2, 0, "div", 14);
    i0.ɵɵelementStart(48, "div", 23);
    i0.ɵɵrepeaterCreate(49, AgendaComponent_Conditional_13_For_50_Template, 2, 1, "button", 24, _forTrack1);
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance(4);
    i0.ɵɵconditional(ctx_r1.horarios.length === 0 ? 4 : -1);
    i0.ɵɵadvance(2);
    i0.ɵɵrepeater(ctx_r1.horarios);
    i0.ɵɵadvance(20);
    i0.ɵɵrepeater(ctx_r1.citas);
    i0.ɵɵadvance(6);
    i0.ɵɵrepeater(ctx_r1.bloqueos);
    i0.ɵɵadvance(8);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.servicioSeleccionado);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(3);
    i0.ɵɵrepeater(ctx_r1.servicios);
    i0.ɵɵadvance(4);
    i0.ɵɵconditional(ctx_r1.slots.length === 0 ? 47 : -1);
    i0.ɵɵadvance(2);
    i0.ɵɵrepeater(ctx_r1.slots);
} }
function AgendaComponent_Conditional_14_Template(rf, ctx) { if (rf & 1) {
    const _r13 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 9)(1, "app-calendario", 29);
    i0.ɵɵlistener("irDia", function AgendaComponent_Conditional_14_Template_app_calendario_irDia_1_listener($event) { i0.ɵɵrestoreView(_r13); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.visitarDia($event)); });
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵproperty("modo", ctx_r1.vista)("fecha", ctx_r1.fecha)("odontologoId", ctx_r1.odontologoNum);
} }
function AgendaComponent_Conditional_15_Template(rf, ctx) { if (rf & 1) {
    const _r14 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 30);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_15_Template_div_click_0_listener($event) { i0.ɵɵrestoreView(_r14); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.close($event)); });
    i0.ɵɵelementStart(1, "div", 31);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_15_Template_div_click_1_listener($event) { i0.ɵɵrestoreView(_r14); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.stop($event)); });
    i0.ɵɵelementStart(2, "h2");
    i0.ɵɵtext(3, "Bloquear horario");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "div", 32)(5, "div")(6, "label", 33);
    i0.ɵɵtext(7, "Hora inicio");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(8, "input", 34);
    i0.ɵɵtwoWayListener("ngModelChange", function AgendaComponent_Conditional_15_Template_input_ngModelChange_8_listener($event) { i0.ɵɵrestoreView(_r14); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.bloqueo.horaInicio, $event) || (ctx_r1.bloqueo.horaInicio = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(9, "div")(10, "label", 33);
    i0.ɵɵtext(11, "Hora fin");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(12, "input", 34);
    i0.ɵɵtwoWayListener("ngModelChange", function AgendaComponent_Conditional_15_Template_input_ngModelChange_12_listener($event) { i0.ɵɵrestoreView(_r14); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.bloqueo.horaFin, $event) || (ctx_r1.bloqueo.horaFin = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(13, "div", 35)(14, "label", 33);
    i0.ɵɵtext(15, "Motivo");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(16, "input", 36);
    i0.ɵɵtwoWayListener("ngModelChange", function AgendaComponent_Conditional_15_Template_input_ngModelChange_16_listener($event) { i0.ɵɵrestoreView(_r14); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.bloqueo.motivo, $event) || (ctx_r1.bloqueo.motivo = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(17, "div", 37)(18, "button", 22);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_15_Template_button_click_18_listener() { i0.ɵɵrestoreView(_r14); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.showBloqueo = false); });
    i0.ɵɵtext(19, "Cancelar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(20, "button", 38);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_15_Template_button_click_20_listener() { i0.ɵɵrestoreView(_r14); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.guardarBloqueo()); });
    i0.ɵɵtext(21, "Guardar");
    i0.ɵɵelementEnd()()()();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance(8);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.bloqueo.horaInicio);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.bloqueo.horaFin);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.bloqueo.motivo);
    i0.ɵɵcontrol();
} }
function AgendaComponent_Conditional_16_For_14_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "option", 5);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const p_r16 = ctx.$implicit;
    i0.ɵɵproperty("value", p_r16.id);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate2("", p_r16.nombres, " ", p_r16.apellidos);
} }
function AgendaComponent_Conditional_16_For_22_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "option", 5);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const s_r17 = ctx.$implicit;
    i0.ɵɵproperty("value", s_r17.id);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(s_r17.nombre);
} }
function AgendaComponent_Conditional_16_Template(rf, ctx) { if (rf & 1) {
    const _r15 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 30);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_16_Template_div_click_0_listener($event) { i0.ɵɵrestoreView(_r15); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.close($event)); });
    i0.ɵɵelementStart(1, "div", 31);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_16_Template_div_click_1_listener($event) { i0.ɵɵrestoreView(_r15); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.stop($event)); });
    i0.ɵɵelementStart(2, "h2");
    i0.ɵɵtext(3, "Nueva cita");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "div", 39);
    i0.ɵɵtext(5);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(6, "div", 32)(7, "div", 35)(8, "label", 33);
    i0.ɵɵtext(9, "Paciente *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(10, "select", 36);
    i0.ɵɵtwoWayListener("ngModelChange", function AgendaComponent_Conditional_16_Template_select_ngModelChange_10_listener($event) { i0.ɵɵrestoreView(_r15); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.cita.pacienteId, $event) || (ctx_r1.cita.pacienteId = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementStart(11, "option", 21);
    i0.ɵɵtext(12, "Selecciona un paciente");
    i0.ɵɵelementEnd();
    i0.ɵɵrepeaterCreate(13, AgendaComponent_Conditional_16_For_14_Template, 2, 3, "option", 5, _forTrack0);
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(15, "div")(16, "label", 33);
    i0.ɵɵtext(17, "Servicio *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(18, "select", 40);
    i0.ɵɵtwoWayListener("ngModelChange", function AgendaComponent_Conditional_16_Template_select_ngModelChange_18_listener($event) { i0.ɵɵrestoreView(_r15); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.cita.servicioId, $event) || (ctx_r1.cita.servicioId = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵlistener("change", function AgendaComponent_Conditional_16_Template_select_change_18_listener() { i0.ɵɵrestoreView(_r15); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.onServicio()); });
    i0.ɵɵelementStart(19, "option", 21);
    i0.ɵɵtext(20, "\u2014");
    i0.ɵɵelementEnd();
    i0.ɵɵrepeaterCreate(21, AgendaComponent_Conditional_16_For_22_Template, 2, 2, "option", 5, _forTrack0);
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(23, "div")(24, "label", 33);
    i0.ɵɵtext(25, "Hora *");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(26, "input", 41);
    i0.ɵɵtwoWayListener("ngModelChange", function AgendaComponent_Conditional_16_Template_input_ngModelChange_26_listener($event) { i0.ɵɵrestoreView(_r15); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.cita.horaInicio, $event) || (ctx_r1.cita.horaInicio = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(27, "div", 35)(28, "label", 33);
    i0.ɵɵtext(29, "Observaciones");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(30, "input", 36);
    i0.ɵɵtwoWayListener("ngModelChange", function AgendaComponent_Conditional_16_Template_input_ngModelChange_30_listener($event) { i0.ɵɵrestoreView(_r15); const ctx_r1 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r1.cita.observaciones, $event) || (ctx_r1.cita.observaciones = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(31, "div", 37)(32, "button", 22);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_16_Template_button_click_32_listener() { i0.ɵɵrestoreView(_r15); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.showCita = false); });
    i0.ɵɵtext(33, "Cancelar");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(34, "button", 38);
    i0.ɵɵlistener("click", function AgendaComponent_Conditional_16_Template_button_click_34_listener() { i0.ɵɵrestoreView(_r15); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.guardarCita()); });
    i0.ɵɵtext(35, "Agendar");
    i0.ɵɵelementEnd()()()();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate2("", ctx_r1.fecha, " \u2014 ", ctx_r1.horaSugerida);
    i0.ɵɵadvance(5);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.cita.pacienteId);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(3);
    i0.ɵɵrepeater(ctx_r1.pacientes);
    i0.ɵɵadvance(5);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.cita.servicioId);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(3);
    i0.ɵɵrepeater(ctx_r1.servicios);
    i0.ɵɵadvance(5);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.cita.horaInicio);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r1.cita.observaciones);
    i0.ɵɵcontrol();
} }
export class AgendaComponent {
    api;
    vista = 'dia';
    odontologos = [];
    odontologoSeleccionado = '';
    servicios = [];
    servicioSeleccionado = '';
    pacientes = [];
    citas = [];
    bloqueos = [];
    horarios = [];
    slots = [];
    fecha = new Date().toISOString().slice(0, 10);
    error = '';
    showBloqueo = false;
    showCita = false;
    horaSugerida = '';
    bloqueo = {};
    cita = {};
    constructor(api) {
        this.api = api;
        this.api.get('/odontologos/activos').subscribe({
            next: (r) => {
                this.odontologos = r;
                if (r.length) {
                    this.odontologoSeleccionado = String(r[0].id);
                    this.cargarTodo();
                }
            },
            error: (e) => (this.error = this.msg(e)),
        });
        this.api.get('/servicios/activos').subscribe((r) => (this.servicios = r));
    }
    get diario() {
        const d = new Date(this.fecha + 'T00:00:00');
        return d.toLocaleDateString('es-EC', { weekday: 'long' });
    }
    get odontologoNum() {
        const n = Number(this.odontologoSeleccionado);
        return Number.isFinite(n) && n > 0 ? n : undefined;
    }
    setVista(v) {
        this.vista = v;
    }
    visitarDia(d) {
        this.fecha = d;
        this.setVista('dia');
        this.cargarTodo();
    }
    cargarTodo() {
        if (!this.odontologoSeleccionado)
            return;
        const o = this.odontologoSeleccionado;
        const f = this.fecha;
        this.api.get(`/agenda?odontologoId=${o}&fecha=${f}`).subscribe({
            next: (r) => {
                this.citas = [...r].sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
            },
            error: (e) => (this.error = this.msg(e)),
        });
        this.api.get(`/agenda/bloqueos?odontologoId=${o}&fecha=${f}`).subscribe((r) => (this.bloqueos = r));
        this.api.get(`/horarios?odontologoId=${o}`).subscribe((r) => {
            const d = new Date(f + 'T00:00:00');
            const dia = ((d.getDay() + 6) % 7) + 1;
            this.horarios = r.filter((h) => h.diaSemana === dia && h.estado === 'ACTIVO');
        });
        this.cargarSlots();
    }
    cargarSlots() {
        if (!this.odontologoSeleccionado || !this.servicioSeleccionado) {
            this.slots = [];
            return;
        }
        const svc = this.servicios.find((s) => s.id === Number(this.servicioSeleccionado));
        this.api
            .get(`/agenda/disponibilidad?odontologoId=${this.odontologoSeleccionado}&fecha=${this.fecha}&servicioId=${this.servicioSeleccionado}&duracion=${svc?.duracionMinutos ?? ''}`)
            .subscribe({
            next: (r) => (this.slots = r),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    crearBloqueo() {
        this.bloqueo = { horaInicio: '08:00', horaFin: '12:00', motivo: '' };
        this.showBloqueo = true;
    }
    guardarBloqueo() {
        this.api
            .post('/agenda/bloqueos', {
            odontologoId: Number(this.odontologoSeleccionado),
            fecha: this.fecha,
            horaInicio: this.bloqueo.horaInicio || null,
            horaFin: this.bloqueo.horaFin || null,
            motivo: this.bloqueo.motivo ?? null,
        })
            .subscribe({
            next: () => {
                this.showBloqueo = false;
                this.cargarTodo();
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    quitarBloqueo(b) {
        if (!confirm('¿Quitar este bloqueo?'))
            return;
        this.api.del(`/agenda/bloqueos/${b.id}`).subscribe({
            next: () => this.cargarTodo(),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    crearCitaEn(s) {
        if (this.pacientes.length === 0) {
            this.api.get('/pacientes?estado=ACTIVO&page=0&size=500').subscribe((r) => (this.pacientes = r.content));
        }
        const svc = this.servicios.find((s2) => s2.id === Number(this.servicioSeleccionado));
        this.cita = {
            pacienteId: '',
            servicioId: svc ? svc.id : undefined,
            horaInicio: s.horaInicio,
            observaciones: '',
        };
        this.horaSugerida = s.horaInicio;
        this.showCita = true;
    }
    onServicio() {
        if (this.cita.horaInicio)
            return;
        const svc = this.servicios.find((s) => s.id === Number(this.cita.servicioId));
        if (svc) {
            const now = new Date();
            now.setHours(8, 0, 0, 0);
            this.cita.horaInicio = now.toTimeString().slice(0, 5);
        }
    }
    guardarCita() {
        if (!this.cita.pacienteId || !this.cita.servicioId || !this.cita.horaInicio)
            return;
        this.api
            .post('/citas', {
            pacienteId: Number(this.cita.pacienteId),
            doctorId: Number(this.odontologoSeleccionado),
            servicioId: Number(this.cita.servicioId),
            fecha: this.fecha,
            horaInicio: this.cita.horaInicio,
            observaciones: this.cita.observaciones ?? null,
        })
            .subscribe({
            next: () => {
                this.showCita = false;
                this.cargarTodo();
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    badge(estado) {
        switch (estado) {
            case 'CONFIRMADA':
                return 'ok';
            case 'REALIZADA':
                return 'info';
            case 'CANCELADA':
                return 'bad';
            case 'NO_ASISTIO':
                return 'warn';
            default:
                return 'dim';
        }
    }
    close(e) {
        if (e.target === e.currentTarget) {
            this.showBloqueo = false;
            this.showCita = false;
        }
    }
    stop(e) {
        e.stopPropagation();
    }
    msg(e) {
        const a = e;
        if (a?.status === 409 || a?.status === 400)
            return a.error?.message ?? 'Datos inválidos';
        return 'Error de conexión';
    }
    static ɵfac = function AgendaComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || AgendaComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: AgendaComponent, selectors: [["app-agenda"]], decls: 17, vars: 7, consts: [[1, "card"], [1, "between"], [1, "flex"], ["type", "date", 1, "input", 2, "width", "160px", 3, "ngModelChange", "change", "ngModel"], [1, "input", 2, "width", "260px", 3, "ngModelChange", "change", "ngModel"], [3, "value"], [1, "msg", "error"], [1, "seg", 2, "margin-top", "10px"], [1, "seg-btn", 3, "ngClass"], [1, "card", 2, "margin-top", "6px"], [1, "modal-backdrop"], [1, "seg-btn", 3, "click", "ngClass"], [1, "grid", "cols-2", 2, "margin-top", "6px"], [2, "grid-column", "1 / -1"], [1, "empty"], [1, "badge", "info"], [1, "btn", "small", 3, "click"], [1, "card", 2, "margin", "0"], [1, "tbl"], [1, "card", 2, "margin", "16px 0 0"], [1, "input", 2, "width", "240px", 3, "ngModelChange", "change", "ngModel"], ["value", ""], [1, "btn", 3, "click"], [1, "flex", 2, "flex-wrap", "wrap", "margin-top", "8px"], [1, "btn", "small"], [1, "badge", 3, "ngClass"], ["routerLink", "/citas", 1, "btn", "small"], ["colspan", "5", 1, "empty"], [1, "btn", "small", "danger", 3, "click"], [3, "irDia", "modo", "fecha", "odontologoId"], [1, "modal-backdrop", 3, "click"], [1, "modal", 3, "click"], [1, "form-row"], [1, "label"], ["type", "time", 1, "input", 3, "ngModelChange", "ngModel"], [1, "full"], [1, "input", 3, "ngModelChange", "ngModel"], [1, "modal-actions"], [1, "btn", "primary", 3, "click"], [1, "sub"], [1, "input", 3, "ngModelChange", "change", "ngModel"], ["type", "time", "required", "", 1, "input", 3, "ngModelChange", "ngModel"]], template: function AgendaComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Agenda del d\u00EDa");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "div", 2)(5, "input", 3);
            i0.ɵɵtwoWayListener("ngModelChange", function AgendaComponent_Template_input_ngModelChange_5_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.fecha, $event) || (ctx.fecha = $event); return $event; });
            i0.ɵɵlistener("change", function AgendaComponent_Template_input_change_5_listener() { return ctx.cargarTodo(); });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(6, "select", 4);
            i0.ɵɵtwoWayListener("ngModelChange", function AgendaComponent_Template_select_ngModelChange_6_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.odontologoSeleccionado, $event) || (ctx.odontologoSeleccionado = $event); return $event; });
            i0.ɵɵlistener("change", function AgendaComponent_Template_select_change_6_listener() { return ctx.cargarTodo(); });
            i0.ɵɵrepeaterCreate(7, AgendaComponent_For_8_Template, 2, 3, "option", 5, _forTrack0);
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementEnd()();
            i0.ɵɵconditionalCreate(9, AgendaComponent_Conditional_9_Template, 2, 1, "div", 6);
            i0.ɵɵelementStart(10, "div", 7);
            i0.ɵɵrepeaterCreate(11, AgendaComponent_For_12_Template, 2, 4, "button", 8, i0.ɵɵrepeaterTrackByIdentity);
            i0.ɵɵelementEnd();
            i0.ɵɵconditionalCreate(13, AgendaComponent_Conditional_13_Template, 51, 5)(14, AgendaComponent_Conditional_14_Template, 2, 3, "div", 9);
            i0.ɵɵelementEnd();
            i0.ɵɵconditionalCreate(15, AgendaComponent_Conditional_15_Template, 22, 3, "div", 10);
            i0.ɵɵconditionalCreate(16, AgendaComponent_Conditional_16_Template, 36, 6, "div", 10);
        } if (rf & 2) {
            i0.ɵɵadvance(5);
            i0.ɵɵtwoWayProperty("ngModel", ctx.fecha);
            i0.ɵɵcontrol();
            i0.ɵɵadvance();
            i0.ɵɵtwoWayProperty("ngModel", ctx.odontologoSeleccionado);
            i0.ɵɵcontrol();
            i0.ɵɵadvance();
            i0.ɵɵrepeater(ctx.odontologos);
            i0.ɵɵadvance(2);
            i0.ɵɵconditional(ctx.error ? 9 : -1);
            i0.ɵɵadvance(2);
            i0.ɵɵrepeater(i0.ɵɵpureFunction0(6, _c0));
            i0.ɵɵadvance(2);
            i0.ɵɵconditional(ctx.vista === "dia" ? 13 : 14);
            i0.ɵɵadvance(2);
            i0.ɵɵconditional(ctx.showBloqueo ? 15 : -1);
            i0.ɵɵadvance();
            i0.ɵɵconditional(ctx.showCita ? 16 : -1);
        } }, dependencies: [CommonModule, i2.NgClass, FormsModule, i3.NgSelectOption, i3.ɵNgSelectMultipleOption, i3.DefaultValueAccessor, i3.SelectControlValueAccessor, i3.NgControlStatus, i3.RequiredValidator, i3.NgModel, RouterLink, CalendarioComponent], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(AgendaComponent, [{
        type: Component,
        args: [{
                selector: 'app-agenda',
                template: `
    <div class="card">
      <div class="between">
        <h2>Agenda del día</h2>
        <div class="flex">
          <input class="input" style="width:160px" type="date" [(ngModel)]="fecha" (change)="cargarTodo()" />
          <select class="input" style="width:260px" [(ngModel)]="odontologoSeleccionado" (change)="cargarTodo()">
            @for (o of odontologos; track o.id) {
              <option [value]="o.id">{{ o.nombres }} {{ o.apellidos }}</option>
            }
          </select>
        </div>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }

      <div class="seg" style="margin-top:10px">
        @for (v of ['dia', 'semana', 'mes']; track v) {
          <button class="seg-btn" [ngClass]="{ active: vista === v }" (click)="setVista(v)">
            {{ v === 'dia' ? 'Día' : v === 'semana' ? 'Semana' : 'Mes' }}
          </button>
        }
      </div>

      @if (vista === 'dia') {
      <div class="grid cols-2" style="margin-top:6px">
        <div style="grid-column: 1 / -1">
          <h3>Horarios atendidos hoy</h3>
          @if (horarios.length === 0) {
            <div class="empty">Sin horario configurado para hoy ({{ diario }}).</div>
          }
          <div class="flex">
            @for (h of horarios; track h.id) {
              <span class="badge info">{{ h.horaInicio }}–{{ h.horaFin }} ({{ h.intervaloMinutos }} min)</span>
            }
            <button class="btn small" (click)="crearBloqueo()">+ Bloquear hora</button>
          </div>
        </div>

        <div class="card" style="margin:0">
          <h3>Citas del día</h3>
          <table class="tbl">
            <thead><tr><th>Hora</th><th>Paciente</th><th>Servicio</th><th>Estado</th><th></th></tr></thead>
            <tbody>
              @for (c of citas; track c.id) {
                <tr>
                  <td>{{ c.horaInicio }}–{{ c.horaFin }}</td>
                  <td>{{ c.pacienteNombre }}</td>
                  <td>{{ c.servicioNombre }}</td>
                  <td><span class="badge" [ngClass]="badge(c.estado)">{{ c.estado }}</span></td>
                  <td><a class="btn small" routerLink="/citas">Ver</a></td>
                </tr>
              } @empty {
                <tr><td colspan="5" class="empty">Sin citas este día.</td></tr>
              }
            </tbody>
          </table>
        </div>

        <div class="card" style="margin:0">
          <h3>Bloqueos</h3>
          @for (b of bloqueos; track b.id) {
            <div class="between">
              <span>{{ b.horaInicio || 'Todo el día' }}{{ b.horaInicio && b.horaFin ? '–' + b.horaFin : '' }} — {{ b.motivo || 'Sin motivo' }}</span>
              <button class="btn small danger" (click)="quitarBloqueo(b)">Quitar</button>
            </div>
          } @empty {
            <div class="empty">Sin bloqueos.</div>
          }
        </div>
      </div>

      <div class="card" style="margin:16px 0 0">
        <div class="between">
          <h3>Disponibilidad</h3>
          <div class="flex">
            <select class="input" style="width:240px" [(ngModel)]="servicioSeleccionado" (change)="cargarSlots()">
              <option value="">Selecciona un servicio</option>
              @for (s of servicios; track s.id) {
                <option [value]="s.id">{{ s.nombre }} ({{ s.duracionMinutos }} min)</option>
              }
            </select>
            <button class="btn" (click)="cargarSlots()">Buscar horarios libres</button>
          </div>
        </div>
        @if (slots.length === 0) {
          <div class="empty">Selecciona un servicio y fecha para ver los horarios libres.</div>
        }
        <div class="flex" style="flex-wrap:wrap; margin-top:8px">
          @for (s of slots; track s.horaInicio) {
            <button class="btn small" (click)="crearCitaEn(s)">{{ s.horaInicio }}</button>
          }
        </div>
      </div>
      } @else {
        <div class="card" style="margin-top:6px">
          <app-calendario
            [modo]="vista"
            [fecha]="fecha"
            [odontologoId]="odontologoNum"
            (irDia)="visitarDia($event)"
          />
        </div>
      }
    </div>

    @if (showBloqueo) {
      <div class="modal-backdrop" (click)="close($event)">
        <div class="modal" (click)="stop($event)">
          <h2>Bloquear horario</h2>
          <div class="form-row">
            <div><label class="label">Hora inicio</label><input class="input" type="time" [(ngModel)]="bloqueo.horaInicio" /></div>
            <div><label class="label">Hora fin</label><input class="input" type="time" [(ngModel)]="bloqueo.horaFin" /></div>
            <div class="full"><label class="label">Motivo</label><input class="input" [(ngModel)]="bloqueo.motivo" /></div>
          </div>
          <div class="modal-actions">
            <button class="btn" (click)="showBloqueo = false">Cancelar</button>
            <button class="btn primary" (click)="guardarBloqueo()">Guardar</button>
          </div>
        </div>
      </div>
    }

    @if (showCita) {
      <div class="modal-backdrop" (click)="close($event)">
        <div class="modal" (click)="stop($event)">
          <h2>Nueva cita</h2>
          <div class="sub">{{ fecha }} — {{ horaSugerida }}</div>
          <div class="form-row">
            <div class="full">
              <label class="label">Paciente *</label>
              <select class="input" [(ngModel)]="cita.pacienteId">
                <option value="">Selecciona un paciente</option>
                @for (p of pacientes; track p.id) {
                  <option [value]="p.id">{{ p.nombres }} {{ p.apellidos }}</option>
                }
              </select>
            </div>
            <div>
              <label class="label">Servicio *</label>
              <select class="input" [(ngModel)]="cita.servicioId" (change)="onServicio()">
                <option value="">—</option>
                @for (s of servicios; track s.id) {
                  <option [value]="s.id">{{ s.nombre }}</option>
                }
              </select>
            </div>
            <div><label class="label">Hora *</label><input class="input" type="time" [(ngModel)]="cita.horaInicio" required /></div>
            <div class="full"><label class="label">Observaciones</label><input class="input" [(ngModel)]="cita.observaciones" /></div>
          </div>
          <div class="modal-actions">
            <button class="btn" (click)="showCita = false">Cancelar</button>
            <button class="btn primary" (click)="guardarCita()">Agendar</button>
          </div>
        </div>
      </div>
    }
  `,
                imports: [CommonModule, FormsModule, RouterLink, CalendarioComponent],
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(AgendaComponent, { className: "AgendaComponent", filePath: "src/app/agenda/agenda.ts", lineNumber: 172 }); })();
