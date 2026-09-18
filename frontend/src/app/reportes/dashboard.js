import { Component } from '@angular/core';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
function DashboardComponent_Conditional_3_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElement(0, "span", 1);
} }
function DashboardComponent_Conditional_4_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 2)(1, "div", 4)(2, "div", 5);
    i0.ɵɵtext(3);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(4, "div", 6);
    i0.ɵɵtext(5, "Citas hoy");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(6, "div", 4)(7, "div", 7);
    i0.ɵɵtext(8);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(9, "div", 6);
    i0.ɵɵtext(10, "Pendientes hoy");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(11, "div", 4)(12, "div", 8);
    i0.ɵɵtext(13);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(14, "div", 6);
    i0.ɵɵtext(15, "Confirmadas hoy");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(16, "div", 4)(17, "div", 9);
    i0.ɵɵtext(18);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(19, "div", 6);
    i0.ɵɵtext(20, "Canceladas hoy");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(21, "div", 4)(22, "div", 9);
    i0.ɵɵtext(23);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(24, "div", 6);
    i0.ɵɵtext(25, "No asistieron");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(26, "div", 4)(27, "div", 5);
    i0.ɵɵtext(28);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(29, "div", 6);
    i0.ɵɵtext(30, "Pacientes nuevos (mes)");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(31, "div", 4)(32, "div", 5);
    i0.ɵɵtext(33);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(34, "div", 6);
    i0.ɵɵtext(35, "Conversaciones abiertas");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(36, "div", 4)(37, "div", 8);
    i0.ɵɵtext(38);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(39, "div", 6);
    i0.ɵɵtext(40, "Mensajes enviados");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(41, "div", 4)(42, "div", 9);
    i0.ɵɵtext(43);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(44, "div", 6);
    i0.ɵɵtext(45, "Mensajes con error");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(46, "div", 4)(47, "div", 8);
    i0.ɵɵtext(48);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(49, "div", 6);
    i0.ɵɵtext(50, "Notificaciones enviadas");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(51, "div", 4)(52, "div", 9);
    i0.ɵɵtext(53);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(54, "div", 6);
    i0.ɵɵtext(55, "Notificaciones con error");
    i0.ɵɵdomElementEnd()()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(ctx_r0.d.citasDeHoy);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r0.d.citasPendientesHoy);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r0.d.citasConfirmadasHoy);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r0.d.citasCanceladasHoy);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r0.d.noAsistieronHoy);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r0.d.pacientesNuevosMes);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r0.d.conversacionesAbiertas);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r0.d.mensajesEnviados);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r0.d.mensajesConError);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r0.d.notificacionesEnviadas);
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r0.d.notificacionesConError);
} }
function DashboardComponent_Conditional_5_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 3);
    i0.ɵɵtext(1, "No se pudo cargar el resumen.");
    i0.ɵɵdomElementEnd();
} }
export class DashboardComponent {
    api;
    d = null;
    cargando = true;
    constructor(api) {
        this.api = api;
        this.api.get('/reportes/dashboard').subscribe({
            next: (r) => {
                this.d = r;
                this.cargando = false;
            },
            error: () => {
                this.cargando = false;
            },
        });
    }
    static ɵfac = function DashboardComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || DashboardComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: DashboardComponent, selectors: [["app-dashboard"]], decls: 6, vars: 1, consts: [[1, "card"], [1, "spin"], [1, "grid", "cols-3"], [1, "empty"], [1, "stat", "card"], [1, "num"], [1, "lbl"], [1, "num", 2, "color", "var(--warning)"], [1, "num", 2, "color", "var(--success)"], [1, "num", 2, "color", "var(--danger)"]], template: function DashboardComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵdomElementStart(0, "div", 0)(1, "h2");
            i0.ɵɵtext(2, "Resumen del d\u00EDa");
            i0.ɵɵdomElementEnd();
            i0.ɵɵconditionalCreate(3, DashboardComponent_Conditional_3_Template, 1, 0, "span", 1)(4, DashboardComponent_Conditional_4_Template, 56, 11, "div", 2)(5, DashboardComponent_Conditional_5_Template, 2, 0, "div", 3);
            i0.ɵɵdomElementEnd();
        } if (rf & 2) {
            i0.ɵɵadvance(3);
            i0.ɵɵconditional(ctx.cargando ? 3 : ctx.d ? 4 : 5);
        } }, encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(DashboardComponent, [{
        type: Component,
        args: [{
                selector: 'app-dashboard',
                template: `
    <div class="card">
      <h2>Resumen del día</h2>
      @if (cargando) {
        <span class="spin"></span>
      } @else if (d) {
        <div class="grid cols-3">
          <div class="stat card"><div class="num">{{ d.citasDeHoy }}</div><div class="lbl">Citas hoy</div></div>
          <div class="stat card"><div class="num" style="color:var(--warning)">{{ d.citasPendientesHoy }}</div><div class="lbl">Pendientes hoy</div></div>
          <div class="stat card"><div class="num" style="color:var(--success)">{{ d.citasConfirmadasHoy }}</div><div class="lbl">Confirmadas hoy</div></div>
          <div class="stat card"><div class="num" style="color:var(--danger)">{{ d.citasCanceladasHoy }}</div><div class="lbl">Canceladas hoy</div></div>
          <div class="stat card"><div class="num" style="color:var(--danger)">{{ d.noAsistieronHoy }}</div><div class="lbl">No asistieron</div></div>
          <div class="stat card"><div class="num">{{ d.pacientesNuevosMes }}</div><div class="lbl">Pacientes nuevos (mes)</div></div>
          <div class="stat card"><div class="num">{{ d.conversacionesAbiertas }}</div><div class="lbl">Conversaciones abiertas</div></div>
          <div class="stat card"><div class="num" style="color:var(--success)">{{ d.mensajesEnviados }}</div><div class="lbl">Mensajes enviados</div></div>
          <div class="stat card"><div class="num" style="color:var(--danger)">{{ d.mensajesConError }}</div><div class="lbl">Mensajes con error</div></div>
          <div class="stat card"><div class="num" style="color:var(--success)">{{ d.notificacionesEnviadas }}</div><div class="lbl">Notificaciones enviadas</div></div>
          <div class="stat card"><div class="num" style="color:var(--danger)">{{ d.notificacionesConError }}</div><div class="lbl">Notificaciones con error</div></div>
        </div>
      } @else {
        <div class="empty">No se pudo cargar el resumen.</div>
      }
    </div>
  `,
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(DashboardComponent, { className: "DashboardComponent", filePath: "src/app/reportes/dashboard.ts", lineNumber: 32 }); })();
