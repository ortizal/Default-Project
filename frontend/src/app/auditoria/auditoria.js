import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/forms";
const _forTrack0 = ($index, $item) => $item.id;
function AuditoriaComponent_For_10_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "option", 6);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const m_r1 = ctx.$implicit;
    i0.ɵɵproperty("value", m_r1);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(m_r1);
} }
function AuditoriaComponent_Conditional_13_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 8);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r1.error);
} }
function AuditoriaComponent_For_32_Conditional_16_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "span", 11);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const a_r4 = i0.ɵɵnextContext().$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate1("#", a_r4.entidadId);
} }
function AuditoriaComponent_For_32_Conditional_19_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr");
    i0.ɵɵelement(1, "td");
    i0.ɵɵelementStart(2, "td", 16)(3, "div", 17)(4, "div")(5, "div", 18);
    i0.ɵɵtext(6, "Datos anteriores");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(7, "pre", 19);
    i0.ɵɵtext(8);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(9, "div")(10, "div", 18);
    i0.ɵɵtext(11, "Datos nuevos");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(12, "pre", 19);
    i0.ɵɵtext(13);
    i0.ɵɵelementEnd()()()()();
} if (rf & 2) {
    const a_r4 = i0.ɵɵnextContext().$implicit;
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance(8);
    i0.ɵɵtextInterpolate(ctx_r1.pretty(a_r4.datosAnteriores));
    i0.ɵɵadvance(5);
    i0.ɵɵtextInterpolate(ctx_r1.pretty(a_r4.datosNuevos));
} }
function AuditoriaComponent_For_32_Template(rf, ctx) { if (rf & 1) {
    const _r3 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "tr")(1, "td")(2, "button", 14);
    i0.ɵɵlistener("click", function AuditoriaComponent_For_32_Template_button_click_2_listener() { const a_r4 = i0.ɵɵrestoreView(_r3).$implicit; const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.toggle(a_r4.id)); });
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(4, "td");
    i0.ɵɵtext(5);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(6, "td");
    i0.ɵɵtext(7);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(8, "td")(9, "strong");
    i0.ɵɵtext(10);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(11, "td")(12, "span", 15);
    i0.ɵɵtext(13);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(14, "td");
    i0.ɵɵtext(15);
    i0.ɵɵconditionalCreate(16, AuditoriaComponent_For_32_Conditional_16_Template, 2, 1, "span", 11);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(17, "td", 11);
    i0.ɵɵtext(18);
    i0.ɵɵelementEnd()();
    i0.ɵɵconditionalCreate(19, AuditoriaComponent_For_32_Conditional_19_Template, 14, 2, "tr");
} if (rf & 2) {
    const a_r4 = ctx.$implicit;
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(ctx_r1.abierto(a_r4.id) ? "\u2212" : "+");
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(a_r4.createdAt);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(a_r4.usuarioId ?? "-");
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(a_r4.accion);
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(a_r4.modulo);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(a_r4.entidad);
    i0.ɵɵadvance();
    i0.ɵɵconditional(a_r4.entidadId ? 16 : -1);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(a_r4.ip || "-");
    i0.ɵɵadvance();
    i0.ɵɵconditional(ctx_r1.abierto(a_r4.id) ? 19 : -1);
} }
function AuditoriaComponent_ForEmpty_33_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 20);
    i0.ɵɵtext(2, "Sin registros de auditor\u00EDa.");
    i0.ɵɵelementEnd()();
} }
const MODULOS = [
    'AGENDA',
    'AGENTE_IA',
    'AUTOMATIZACIONES',
    'CITAS',
    'GOOGLE_CALENDAR',
    'HORARIOS',
    'ODONTOLOGOS',
    'PACIENTES',
    'SERVICIOS',
    'USUARIOS',
    'WHATSAPP',
];
export class AuditoriaComponent {
    api;
    modulos = MODULOS;
    rows = [];
    q = '';
    modulo = '';
    pagina = 0;
    totalPag = 1;
    error = '';
    abiertos = new Set();
    constructor(api) {
        this.api = api;
        this.cargar();
    }
    buscar() {
        this.pagina = 0;
        this.cargar();
    }
    cargar() {
        const params = [];
        if (this.q.trim())
            params.push(`q=${encodeURIComponent(this.q.trim())}`);
        if (this.modulo)
            params.push(`modulo=${this.modulo}`);
        params.push(`page=${this.pagina}`, 'size=20');
        this.api.get(`/auditoria?${params.join('&')}`).subscribe({
            next: (r) => {
                this.rows = r.content;
                this.totalPag = Math.max(r.totalPages ?? 1, 1);
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    ir(p) {
        if (p < 0 || p >= this.totalPag)
            return;
        this.pagina = p;
        this.cargar();
    }
    toggle(id) {
        if (this.abiertos.has(id)) {
            this.abiertos.delete(id);
        }
        else {
            this.abiertos.add(id);
        }
    }
    abierto(id) {
        return this.abiertos.has(id);
    }
    pretty(v) {
        if (v == null)
            return '—';
        try {
            const obj = typeof v === 'string' ? JSON.parse(v) : v;
            return JSON.stringify(obj, null, 2);
        }
        catch {
            return String(v);
        }
    }
    msg(e) {
        const a = e;
        return a?.status === 403 ? 'No tienes permiso para ver la auditoría.' : 'Error de conexión';
    }
    static ɵfac = function AuditoriaComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || AuditoriaComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: AuditoriaComponent, selectors: [["app-auditoria"]], decls: 42, vars: 8, consts: [[1, "card"], [1, "between"], [1, "flex", "gap", 3, "ngSubmit"], ["name", "q", "placeholder", "Buscar acci\u00F3n, m\u00F3dulo o entidad", 1, "input", 3, "ngModelChange", "ngModel"], ["name", "modulo", 1, "input", 3, "ngModelChange", "ngModel"], ["value", ""], [3, "value"], ["type", "submit", 1, "btn", "primary"], [1, "msg", "error"], [1, "tbl"], [1, "between", 2, "margin-top", "12px"], [1, "muted"], [1, "flex", "gap"], [1, "btn", "small", 3, "click", "disabled"], [1, "btn", "small", 3, "click"], [1, "badge", "info"], ["colspan", "6"], [1, "grid2", 2, "margin-top", "4px"], [1, "label"], [1, "dump"], ["colspan", "7", 1, "empty"]], template: function AuditoriaComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Auditor\u00EDa");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "form", 2);
            i0.ɵɵlistener("ngSubmit", function AuditoriaComponent_Template_form_ngSubmit_4_listener() { return ctx.buscar(); });
            i0.ɵɵelementStart(5, "input", 3);
            i0.ɵɵtwoWayListener("ngModelChange", function AuditoriaComponent_Template_input_ngModelChange_5_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.q, $event) || (ctx.q = $event); return $event; });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(6, "select", 4);
            i0.ɵɵtwoWayListener("ngModelChange", function AuditoriaComponent_Template_select_ngModelChange_6_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.modulo, $event) || (ctx.modulo = $event); return $event; });
            i0.ɵɵlistener("ngModelChange", function AuditoriaComponent_Template_select_ngModelChange_6_listener() { return ctx.buscar(); });
            i0.ɵɵelementStart(7, "option", 5);
            i0.ɵɵtext(8, "Todos los m\u00F3dulos");
            i0.ɵɵelementEnd();
            i0.ɵɵrepeaterCreate(9, AuditoriaComponent_For_10_Template, 2, 2, "option", 6, i0.ɵɵrepeaterTrackByIdentity);
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(11, "button", 7);
            i0.ɵɵtext(12, "Buscar");
            i0.ɵɵelementEnd()()();
            i0.ɵɵconditionalCreate(13, AuditoriaComponent_Conditional_13_Template, 2, 1, "div", 8);
            i0.ɵɵelementStart(14, "table", 9)(15, "thead")(16, "tr");
            i0.ɵɵelement(17, "th");
            i0.ɵɵelementStart(18, "th");
            i0.ɵɵtext(19, "Fecha");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(20, "th");
            i0.ɵɵtext(21, "Usuario");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(22, "th");
            i0.ɵɵtext(23, "Acci\u00F3n");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(24, "th");
            i0.ɵɵtext(25, "M\u00F3dulo");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(26, "th");
            i0.ɵɵtext(27, "Entidad");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(28, "th");
            i0.ɵɵtext(29, "IP");
            i0.ɵɵelementEnd()()();
            i0.ɵɵelementStart(30, "tbody");
            i0.ɵɵrepeaterCreate(31, AuditoriaComponent_For_32_Template, 20, 9, null, null, _forTrack0, false, AuditoriaComponent_ForEmpty_33_Template, 3, 0, "tr");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(34, "div", 10)(35, "span", 11);
            i0.ɵɵtext(36);
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(37, "div", 12)(38, "button", 13);
            i0.ɵɵlistener("click", function AuditoriaComponent_Template_button_click_38_listener() { return ctx.ir(ctx.pagina - 1); });
            i0.ɵɵtext(39, "\u2190 Anterior");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(40, "button", 13);
            i0.ɵɵlistener("click", function AuditoriaComponent_Template_button_click_40_listener() { return ctx.ir(ctx.pagina + 1); });
            i0.ɵɵtext(41, "Siguiente \u2192");
            i0.ɵɵelementEnd()()()();
        } if (rf & 2) {
            i0.ɵɵadvance(5);
            i0.ɵɵtwoWayProperty("ngModel", ctx.q);
            i0.ɵɵcontrol();
            i0.ɵɵadvance();
            i0.ɵɵtwoWayProperty("ngModel", ctx.modulo);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(3);
            i0.ɵɵrepeater(ctx.modulos);
            i0.ɵɵadvance(4);
            i0.ɵɵconditional(ctx.error ? 13 : -1);
            i0.ɵɵadvance(18);
            i0.ɵɵrepeater(ctx.rows);
            i0.ɵɵadvance(5);
            i0.ɵɵtextInterpolate2("P\u00E1gina ", ctx.pagina + 1, " de ", ctx.totalPag);
            i0.ɵɵadvance(2);
            i0.ɵɵproperty("disabled", ctx.pagina === 0);
            i0.ɵɵadvance(2);
            i0.ɵɵproperty("disabled", ctx.pagina + 1 >= ctx.totalPag);
        } }, dependencies: [CommonModule, FormsModule, i2.ɵNgNoValidate, i2.NgSelectOption, i2.ɵNgSelectMultipleOption, i2.DefaultValueAccessor, i2.SelectControlValueAccessor, i2.NgControlStatus, i2.NgControlStatusGroup, i2.NgModel, i2.NgForm], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(AuditoriaComponent, [{
        type: Component,
        args: [{
                selector: 'app-auditoria',
                template: `
    <div class="card">
      <div class="between">
        <h2>Auditoría</h2>
        <form class="flex gap" (ngSubmit)="buscar()">
          <input class="input" [(ngModel)]="q" name="q" placeholder="Buscar acción, módulo o entidad" />
          <select class="input" [(ngModel)]="modulo" name="modulo" (ngModelChange)="buscar()">
            <option value="">Todos los módulos</option>
            @for (m of modulos; track m) {
              <option [value]="m">{{ m }}</option>
            }
          </select>
          <button class="btn primary" type="submit">Buscar</button>
        </form>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead><tr><th></th><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Módulo</th><th>Entidad</th><th>IP</th></tr></thead>
        <tbody>
          @for (a of rows; track a.id) {
            <tr>
              <td><button class="btn small" (click)="toggle(a.id)">{{ abierto(a.id) ? '−' : '+' }}</button></td>
              <td>{{ a.createdAt }}</td>
              <td>{{ a.usuarioId ?? '-' }}</td>
              <td><strong>{{ a.accion }}</strong></td>
              <td><span class="badge info">{{ a.modulo }}</span></td>
              <td>{{ a.entidad }}@if (a.entidadId) { <span class="muted">#{{ a.entidadId }}</span> }</td>
              <td class="muted">{{ a.ip || '-' }}</td>
            </tr>
            @if (abierto(a.id)) {
              <tr>
                <td></td>
                <td colspan="6">
                  <div class="grid2" style="margin-top:4px">
                    <div>
                      <div class="label">Datos anteriores</div>
                      <pre class="dump">{{ pretty(a.datosAnteriores) }}</pre>
                    </div>
                    <div>
                      <div class="label">Datos nuevos</div>
                      <pre class="dump">{{ pretty(a.datosNuevos) }}</pre>
                    </div>
                  </div>
                </td>
              </tr>
            }
          } @empty {
            <tr><td colspan="7" class="empty">Sin registros de auditoría.</td></tr>
          }
        </tbody>
      </table>
      <div class="between" style="margin-top:12px">
        <span class="muted">Página {{ pagina + 1 }} de {{ totalPag }}</span>
        <div class="flex gap">
          <button class="btn small" [disabled]="pagina === 0" (click)="ir(pagina - 1)">← Anterior</button>
          <button class="btn small" [disabled]="pagina + 1 >= totalPag" (click)="ir(pagina + 1)">Siguiente →</button>
        </div>
      </div>
    </div>
  `,
                imports: [CommonModule, FormsModule],
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(AuditoriaComponent, { className: "AuditoriaComponent", filePath: "src/app/auditoria/auditoria.ts", lineNumber: 87 }); })();
