import { Component } from '@angular/core';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
const _forTrack0 = ($index, $item) => $item.nombre;
const _forTrack1 = ($index, $item) => $item.fecha;
function ReportesComponent_Conditional_16_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 6);
    i0.ɵɵtext(1);
    i0.ɵɵdomElementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function ReportesComponent_Conditional_21_For_3_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(3, "td", 13)(4, "strong");
    i0.ɵɵtext(5);
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(6, "td", 14)(7, "div", 15);
    i0.ɵɵdomElement(8, "div", 16);
    i0.ɵɵdomElementEnd()()();
} if (rf & 2) {
    const r_r2 = ctx.$implicit;
    const ctx_r0 = i0.ɵɵnextContext(2);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(r_r2.nombre);
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(r_r2.total);
    i0.ɵɵadvance(3);
    i0.ɵɵstyleProp("width", ctx_r0.pct(r_r2.total, ctx_r0.maxOd), "%");
} }
function ReportesComponent_Conditional_21_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "table", 9)(1, "tbody");
    i0.ɵɵrepeaterCreate(2, ReportesComponent_Conditional_21_For_3_Template, 9, 4, "tr", null, _forTrack0);
    i0.ɵɵdomElementEnd()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(2);
    i0.ɵɵrepeater(ctx_r0.e.citasPorOdontologo);
} }
function ReportesComponent_Conditional_22_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 10);
    i0.ɵɵtext(1, "Sin datos.");
    i0.ɵɵdomElementEnd();
} }
function ReportesComponent_Conditional_26_For_3_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(3, "td", 13)(4, "strong");
    i0.ɵɵtext(5);
    i0.ɵɵdomElementEnd()()();
} if (rf & 2) {
    const r_r3 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(r_r3.nombre);
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(r_r3.total);
} }
function ReportesComponent_Conditional_26_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "table", 9)(1, "tbody");
    i0.ɵɵrepeaterCreate(2, ReportesComponent_Conditional_26_For_3_Template, 6, 2, "tr", null, _forTrack0);
    i0.ɵɵdomElementEnd()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(2);
    i0.ɵɵrepeater(ctx_r0.e.serviciosMasSolicitados);
} }
function ReportesComponent_Conditional_27_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 10);
    i0.ɵɵtext(1, "Sin datos.");
    i0.ɵɵdomElementEnd();
} }
function ReportesComponent_Conditional_31_For_3_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(3, "td", 13)(4, "strong");
    i0.ɵɵtext(5);
    i0.ɵɵdomElementEnd()()();
} if (rf & 2) {
    const r_r4 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(r_r4.nombre);
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(r_r4.total);
} }
function ReportesComponent_Conditional_31_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "table", 9)(1, "tbody");
    i0.ɵɵrepeaterCreate(2, ReportesComponent_Conditional_31_For_3_Template, 6, 2, "tr", null, _forTrack0);
    i0.ɵɵdomElementEnd()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(2);
    i0.ɵɵrepeater(ctx_r0.e.porEstado);
} }
function ReportesComponent_Conditional_32_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 10);
    i0.ɵɵtext(1, "Sin datos.");
    i0.ɵɵdomElementEnd();
} }
function ReportesComponent_Conditional_36_For_3_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(3, "td", 13)(4, "strong");
    i0.ɵɵtext(5);
    i0.ɵɵdomElementEnd()()();
} if (rf & 2) {
    const r_r5 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(r_r5.nombre);
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(r_r5.total);
} }
function ReportesComponent_Conditional_36_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "table", 9)(1, "tbody");
    i0.ɵɵrepeaterCreate(2, ReportesComponent_Conditional_36_For_3_Template, 6, 2, "tr", null, _forTrack0);
    i0.ɵɵdomElementEnd()();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(2);
    i0.ɵɵrepeater(ctx_r0.e.confirmacionesPorFuente);
} }
function ReportesComponent_Conditional_37_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 10);
    i0.ɵɵtext(1, "Sin datos.");
    i0.ɵɵdomElementEnd();
} }
function ReportesComponent_Conditional_41_For_2_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 17)(1, "div", 18);
    i0.ɵɵtext(2);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElement(3, "div", 19);
    i0.ɵɵdomElementStart(4, "div", 20);
    i0.ɵɵtext(5);
    i0.ɵɵdomElementEnd()();
} if (rf & 2) {
    const s_r6 = ctx.$implicit;
    const ctx_r0 = i0.ɵɵnextContext(2);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(s_r6.total);
    i0.ɵɵadvance();
    i0.ɵɵstyleProp("height", ctx_r0.bar(s_r6.total, ctx_r0.maxDia), "px");
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(s_r6.fecha);
} }
function ReportesComponent_Conditional_41_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 12);
    i0.ɵɵrepeaterCreate(1, ReportesComponent_Conditional_41_For_2_Template, 6, 4, "div", 17, _forTrack1);
    i0.ɵɵdomElementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵrepeater(ctx_r0.e.citasPorDia);
} }
function ReportesComponent_Conditional_42_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 10);
    i0.ɵɵtext(1, "Sin datos.");
    i0.ɵɵdomElementEnd();
} }
export class ReportesComponent {
    api;
    e = null;
    error = '';
    constructor(api) {
        this.api = api;
        this.api.get('/reportes/estadisticas').subscribe({
            next: (r) => (this.e = r),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    get maxOd() {
        return Math.max(1, ...(this.e?.citasPorOdontologo?.map((x) => x.total) ?? [1]));
    }
    get maxDia() {
        return Math.max(1, ...(this.e?.citasPorDia?.map((x) => x.total) ?? [1]));
    }
    pct(v, max) {
        return Math.round((Number(v) / max) * 100);
    }
    tot(serie) {
        return (serie ?? []).reduce((a, s) => a + (Number(s.total) || 0), 0);
    }
    bar(v, max) {
        return Math.round((Number(v) / max) * 90) + 2;
    }
    msg(e) {
        const a = e;
        return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
    }
    static ɵfac = function ReportesComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || ReportesComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: ReportesComponent, selectors: [["app-reportes"]], decls: 43, vars: 9, consts: [[1, "grid", "cols-3"], [1, "card", "stat", 2, "margin", "0"], [1, "num"], [1, "lbl"], [1, "num", 2, "color", "var(--danger)"], [1, "num", 2, "color", "var(--warning)"], [1, "msg", "error"], [1, "grid", "cols-2", 2, "margin-top", "16px"], [1, "card", 2, "margin", "0"], [1, "tbl"], [1, "empty"], [1, "card", 2, "margin-top", "16px"], [1, "flex", 2, "align-items", "flex-end", "gap", "2px", "min-height", "120px"], [2, "text-align", "right"], [2, "width", "180px"], [2, "height", "8px", "background", "var(--primary-light)", "border-radius", "4px"], [2, "height", "8px", "background", "var(--primary)", "border-radius", "4px"], [2, "flex", "1", "text-align", "center"], [1, "muted", 2, "font-size", "11px"], [2, "background", "var(--primary)", "border-radius", "3px 3px 0 0"], [1, "muted", 2, "font-size", "10px"]], template: function ReportesComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵdomElementStart(0, "div", 0)(1, "div", 1)(2, "div", 2);
            i0.ɵɵtext(3);
            i0.ɵɵdomElementEnd();
            i0.ɵɵdomElementStart(4, "div", 3);
            i0.ɵɵtext(5, "Confirmaciones (mes)");
            i0.ɵɵdomElementEnd()();
            i0.ɵɵdomElementStart(6, "div", 1)(7, "div", 4);
            i0.ɵɵtext(8);
            i0.ɵɵdomElementEnd();
            i0.ɵɵdomElementStart(9, "div", 3);
            i0.ɵɵtext(10, "Cancelaciones (mes)");
            i0.ɵɵdomElementEnd()();
            i0.ɵɵdomElementStart(11, "div", 1)(12, "div", 5);
            i0.ɵɵtext(13);
            i0.ɵɵdomElementEnd();
            i0.ɵɵdomElementStart(14, "div", 3);
            i0.ɵɵtext(15, "No asistencia (mes)");
            i0.ɵɵdomElementEnd()()();
            i0.ɵɵconditionalCreate(16, ReportesComponent_Conditional_16_Template, 2, 1, "div", 6);
            i0.ɵɵdomElementStart(17, "div", 7)(18, "div", 8)(19, "h2");
            i0.ɵɵtext(20, "Citas por odont\u00F3logo");
            i0.ɵɵdomElementEnd();
            i0.ɵɵconditionalCreate(21, ReportesComponent_Conditional_21_Template, 4, 0, "table", 9)(22, ReportesComponent_Conditional_22_Template, 2, 0, "div", 10);
            i0.ɵɵdomElementEnd();
            i0.ɵɵdomElementStart(23, "div", 8)(24, "h2");
            i0.ɵɵtext(25, "Servicios m\u00E1s solicitados");
            i0.ɵɵdomElementEnd();
            i0.ɵɵconditionalCreate(26, ReportesComponent_Conditional_26_Template, 4, 0, "table", 9)(27, ReportesComponent_Conditional_27_Template, 2, 0, "div", 10);
            i0.ɵɵdomElementEnd();
            i0.ɵɵdomElementStart(28, "div", 8)(29, "h2");
            i0.ɵɵtext(30, "Citas por estado");
            i0.ɵɵdomElementEnd();
            i0.ɵɵconditionalCreate(31, ReportesComponent_Conditional_31_Template, 4, 0, "table", 9)(32, ReportesComponent_Conditional_32_Template, 2, 0, "div", 10);
            i0.ɵɵdomElementEnd();
            i0.ɵɵdomElementStart(33, "div", 8)(34, "h2");
            i0.ɵɵtext(35, "Confirmaciones por fuente");
            i0.ɵɵdomElementEnd();
            i0.ɵɵconditionalCreate(36, ReportesComponent_Conditional_36_Template, 4, 0, "table", 9)(37, ReportesComponent_Conditional_37_Template, 2, 0, "div", 10);
            i0.ɵɵdomElementEnd()();
            i0.ɵɵdomElementStart(38, "div", 11)(39, "h2");
            i0.ɵɵtext(40, "Citas por d\u00EDa (\u00FAltimo mes)");
            i0.ɵɵdomElementEnd();
            i0.ɵɵconditionalCreate(41, ReportesComponent_Conditional_41_Template, 3, 0, "div", 12)(42, ReportesComponent_Conditional_42_Template, 2, 0, "div", 10);
            i0.ɵɵdomElementEnd();
        } if (rf & 2) {
            i0.ɵɵadvance(3);
            i0.ɵɵtextInterpolate(ctx.tot(ctx.e?.confirmaciones));
            i0.ɵɵadvance(5);
            i0.ɵɵtextInterpolate(ctx.tot(ctx.e?.cancelaciones));
            i0.ɵɵadvance(5);
            i0.ɵɵtextInterpolate(ctx.tot(ctx.e?.noAsistencia));
            i0.ɵɵadvance(3);
            i0.ɵɵconditional(ctx.error ? 16 : -1);
            i0.ɵɵadvance(5);
            i0.ɵɵconditional(ctx.e?.citasPorOdontologo ? 21 : 22);
            i0.ɵɵadvance(5);
            i0.ɵɵconditional(ctx.e?.serviciosMasSolicitados ? 26 : 27);
            i0.ɵɵadvance(5);
            i0.ɵɵconditional(ctx.e?.porEstado ? 31 : 32);
            i0.ɵɵadvance(5);
            i0.ɵɵconditional(ctx.e?.confirmacionesPorFuente ? 36 : 37);
            i0.ɵɵadvance(5);
            i0.ɵɵconditional(ctx.e?.citasPorDia && ctx.e.citasPorDia.length ? 41 : 42);
        } }, encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(ReportesComponent, [{
        type: Component,
        args: [{
                selector: 'app-reportes',
                template: `
    <div class="grid cols-3">
      <div class="card stat" style="margin:0"><div class="num">{{ tot(e?.confirmaciones) }}</div><div class="lbl">Confirmaciones (mes)</div></div>
      <div class="card stat" style="margin:0"><div class="num" style="color:var(--danger)">{{ tot(e?.cancelaciones) }}</div><div class="lbl">Cancelaciones (mes)</div></div>
      <div class="card stat" style="margin:0"><div class="num" style="color:var(--warning)">{{ tot(e?.noAsistencia) }}</div><div class="lbl">No asistencia (mes)</div></div>
    </div>
    @if (error) {
      <div class="msg error">{{ error }}</div>
    }
    <div class="grid cols-2" style="margin-top:16px">
      <div class="card" style="margin:0">
        <h2>Citas por odontólogo</h2>
        @if (e?.citasPorOdontologo) {
          <table class="tbl">
            <tbody>
              @for (r of e!.citasPorOdontologo; track r.nombre) {
                <tr>
                  <td>{{ r.nombre }}</td>
                  <td style="text-align:right"><strong>{{ r.total }}</strong></td>
                  <td style="width:180px"><div style="height:8px; background:var(--primary-light); border-radius:4px"><div [style.width.%]="pct(r.total, maxOd)" style="height:8px; background:var(--primary); border-radius:4px"></div></div></td>
                </tr>
              }
            </tbody>
          </table>
        } @else {
          <div class="empty">Sin datos.</div>
        }
      </div>
      <div class="card" style="margin:0">
        <h2>Servicios más solicitados</h2>
        @if (e?.serviciosMasSolicitados) {
          <table class="tbl">
            <tbody>
              @for (r of e!.serviciosMasSolicitados; track r.nombre) {
                <tr>
                  <td>{{ r.nombre }}</td>
                  <td style="text-align:right"><strong>{{ r.total }}</strong></td>
                </tr>
              }
            </tbody>
          </table>
        } @else {
          <div class="empty">Sin datos.</div>
        }
      </div>
      <div class="card" style="margin:0">
        <h2>Citas por estado</h2>
        @if (e?.porEstado) {
          <table class="tbl">
            <tbody>
              @for (r of e!.porEstado; track r.nombre) {
                <tr><td>{{ r.nombre }}</td><td style="text-align:right"><strong>{{ r.total }}</strong></td></tr>
              }
            </tbody>
          </table>
        } @else {
          <div class="empty">Sin datos.</div>
        }
      </div>
      <div class="card" style="margin:0">
        <h2>Confirmaciones por fuente</h2>
        @if (e?.confirmacionesPorFuente) {
          <table class="tbl">
            <tbody>
              @for (r of e!.confirmacionesPorFuente; track r.nombre) {
                <tr><td>{{ r.nombre }}</td><td style="text-align:right"><strong>{{ r.total }}</strong></td></tr>
              }
            </tbody>
          </table>
        } @else {
          <div class="empty">Sin datos.</div>
        }
      </div>
    </div>
    <div class="card" style="margin-top:16px">
      <h2>Citas por día (último mes)</h2>
      @if (e?.citasPorDia && e!.citasPorDia.length) {
        <div class="flex" style="align-items:flex-end; gap:2px; min-height:120px">
          @for (s of e!.citasPorDia; track s.fecha) {
            <div style="flex:1; text-align:center">
              <div style="font-size:11px" class="muted">{{ s.total }}</div>
              <div [style.height.px]="bar(s.total, maxDia)" style="background:var(--primary); border-radius:3px 3px 0 0"></div>
              <div style="font-size:10px" class="muted">{{ s.fecha }}</div>
            </div>
          }
        </div>
      } @else {
        <div class="empty">Sin datos.</div>
      }
    </div>
  `,
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(ReportesComponent, { className: "ReportesComponent", filePath: "src/app/reportes/reportes.ts", lineNumber: 99 }); })();
