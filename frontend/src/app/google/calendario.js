import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
const _forTrack0 = ($index, $item) => $item.id;
function CalendarioComponent_Conditional_4_Template(rf, ctx) { if (rf & 1) {
    const _r1 = i0.ɵɵgetCurrentView();
    i0.ɵɵdomElementStart(0, "div", 2)(1, "button", 5);
    i0.ɵɵdomListener("click", function CalendarioComponent_Conditional_4_Template_button_click_1_listener() { i0.ɵɵrestoreView(_r1); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.sync()); });
    i0.ɵɵtext(2, "Sincronizar citas");
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(3, "button", 6);
    i0.ɵɵdomListener("click", function CalendarioComponent_Conditional_4_Template_button_click_3_listener() { i0.ɵɵrestoreView(_r1); const ctx_r1 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r1.disconnect()); });
    i0.ɵɵtext(4, "Desconectar");
    i0.ɵɵdomElementEnd()();
} }
function CalendarioComponent_Conditional_5_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 3);
    i0.ɵɵtext(1);
    i0.ɵɵdomElementEnd();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r1.error);
} }
function CalendarioComponent_Conditional_6_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 4);
    i0.ɵɵtext(1, " La integraci\u00F3n con Google no est\u00E1 configurada. Agrega las credenciales OAuth (");
    i0.ɵɵdomElementStart(2, "code");
    i0.ɵɵtext(3, "google.client-id");
    i0.ɵɵdomElementEnd();
    i0.ɵɵtext(4, " / ");
    i0.ɵɵdomElementStart(5, "code");
    i0.ɵɵtext(6, "google.client-secret");
    i0.ɵɵdomElementEnd();
    i0.ɵɵtext(7, " / ");
    i0.ɵɵdomElementStart(8, "code");
    i0.ɵɵtext(9, "google.redirect-uri");
    i0.ɵɵdomElementEnd();
    i0.ɵɵtext(10, ") en ");
    i0.ɵɵdomElementStart(11, "code");
    i0.ɵɵtext(12, "application.properties");
    i0.ɵɵdomElementEnd();
    i0.ɵɵtext(13, " y reinicia el backend. ");
    i0.ɵɵdomElementEnd();
} }
function CalendarioComponent_Conditional_7_Conditional_5_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "span", 11);
    i0.ɵɵtext(1, "Conectada");
    i0.ɵɵdomElementEnd();
} }
function CalendarioComponent_Conditional_7_Conditional_6_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "span", 12);
    i0.ɵɵtext(1, "Sin conectar");
    i0.ɵɵdomElementEnd();
} }
function CalendarioComponent_Conditional_7_Conditional_7_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 13);
    i0.ɵɵtext(1);
    i0.ɵɵdomElementEnd();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext(2);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r1.status.email);
} }
function CalendarioComponent_Conditional_7_Conditional_20_Template(rf, ctx) { if (rf & 1) {
    const _r3 = i0.ɵɵgetCurrentView();
    i0.ɵɵdomElementStart(0, "div", 17)(1, "p", 13);
    i0.ɵɵtext(2, "Conecta una cuenta de Google para que las citas se reflejen en tu calendario.");
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(3, "button", 5);
    i0.ɵɵdomListener("click", function CalendarioComponent_Conditional_7_Conditional_20_Template_button_click_3_listener() { i0.ɵɵrestoreView(_r3); const ctx_r1 = i0.ɵɵnextContext(2); return i0.ɵɵresetView(ctx_r1.connect()); });
    i0.ɵɵtext(4, "Conectar con Google");
    i0.ɵɵdomElementEnd()();
} }
function CalendarioComponent_Conditional_7_Conditional_21_For_12_Conditional_3_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "span", 11);
    i0.ɵɵtext(1, "Seleccionado");
    i0.ɵɵdomElementEnd();
} }
function CalendarioComponent_Conditional_7_Conditional_21_For_12_Conditional_7_Template(rf, ctx) { if (rf & 1) {
    const _r4 = i0.ɵɵgetCurrentView();
    i0.ɵɵdomElementStart(0, "button", 22);
    i0.ɵɵdomListener("click", function CalendarioComponent_Conditional_7_Conditional_21_For_12_Conditional_7_Template_button_click_0_listener() { i0.ɵɵrestoreView(_r4); const c_r5 = i0.ɵɵnextContext().$implicit; const ctx_r1 = i0.ɵɵnextContext(3); return i0.ɵɵresetView(ctx_r1.seleccionar(c_r5)); });
    i0.ɵɵtext(1, "Usar");
    i0.ɵɵdomElementEnd();
} }
function CalendarioComponent_Conditional_7_Conditional_21_For_12_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵconditionalCreate(3, CalendarioComponent_Conditional_7_Conditional_21_For_12_Conditional_3_Template, 2, 0, "span", 11);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(4, "td", 13);
    i0.ɵɵtext(5);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(6, "td");
    i0.ɵɵconditionalCreate(7, CalendarioComponent_Conditional_7_Conditional_21_For_12_Conditional_7_Template, 2, 0, "button", 21);
    i0.ɵɵdomElementEnd()();
} if (rf & 2) {
    const c_r5 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate1(" ", c_r5.summary, " ");
    i0.ɵɵadvance();
    i0.ɵɵconditional(c_r5.seleccionado ? 3 : -1);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r5.timeZone);
    i0.ɵɵadvance(2);
    i0.ɵɵconditional(!c_r5.seleccionado ? 7 : -1);
} }
function CalendarioComponent_Conditional_7_Conditional_21_ForEmpty_13_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "tr")(1, "td", 23);
    i0.ɵɵtext(2, "No se encontraron calendarios.");
    i0.ɵɵdomElementEnd()();
} }
function CalendarioComponent_Conditional_7_Conditional_21_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "h3", 19);
    i0.ɵɵtext(1, "Calendarios de la cuenta");
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(2, "table", 20)(3, "thead")(4, "tr")(5, "th");
    i0.ɵɵtext(6, "Calendario");
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(7, "th");
    i0.ɵɵtext(8, "Zona horaria");
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElement(9, "th");
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(10, "tbody");
    i0.ɵɵrepeaterCreate(11, CalendarioComponent_Conditional_7_Conditional_21_For_12_Template, 8, 4, "tr", null, _forTrack0, false, CalendarioComponent_Conditional_7_Conditional_21_ForEmpty_13_Template, 3, 0, "tr");
    i0.ɵɵdomElementEnd()();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext(2);
    i0.ɵɵadvance(11);
    i0.ɵɵrepeater(ctx_r1.status.calendarios);
} }
function CalendarioComponent_Conditional_7_Conditional_22_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 18);
    i0.ɵɵtext(1);
    i0.ɵɵdomElementEnd();
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext(2);
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate5(" Sincronizaci\u00F3n completa: ", ctx_r1.syncResult.creados, " creados, ", ctx_r1.syncResult.actualizados, " actualizados, ", ctx_r1.syncResult.cancelados, " cancelados, ", ctx_r1.syncResult.errores, " errores, ", ctx_r1.syncResult.sincronizadas, " citas sincronizadas. ");
} }
function CalendarioComponent_Conditional_7_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵdomElementStart(0, "div", 7)(1, "div", 8)(2, "div", 9);
    i0.ɵɵtext(3, "Conexi\u00F3n");
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(4, "div", 10);
    i0.ɵɵconditionalCreate(5, CalendarioComponent_Conditional_7_Conditional_5_Template, 2, 0, "span", 11)(6, CalendarioComponent_Conditional_7_Conditional_6_Template, 2, 0, "span", 12);
    i0.ɵɵdomElementEnd();
    i0.ɵɵconditionalCreate(7, CalendarioComponent_Conditional_7_Conditional_7_Template, 2, 1, "div", 13);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(8, "div", 8)(9, "div", 9);
    i0.ɵɵtext(10, "Sincronizaci\u00F3n de citas");
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(11, "div", 2)(12, "span", 14);
    i0.ɵɵtext(13);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(14, "span", 15);
    i0.ɵɵtext(15);
    i0.ɵɵdomElementEnd();
    i0.ɵɵdomElementStart(16, "span", 16);
    i0.ɵɵtext(17);
    i0.ɵɵdomElementEnd()();
    i0.ɵɵdomElementStart(18, "div", 13);
    i0.ɵɵtext(19, "sincronizadas \u00B7 pendientes \u00B7 errores");
    i0.ɵɵdomElementEnd()()();
    i0.ɵɵconditionalCreate(20, CalendarioComponent_Conditional_7_Conditional_20_Template, 5, 0, "div", 17);
    i0.ɵɵconditionalCreate(21, CalendarioComponent_Conditional_7_Conditional_21_Template, 14, 1);
    i0.ɵɵconditionalCreate(22, CalendarioComponent_Conditional_7_Conditional_22_Template, 2, 5, "div", 18);
} if (rf & 2) {
    const ctx_r1 = i0.ɵɵnextContext();
    i0.ɵɵadvance(5);
    i0.ɵɵconditional(ctx_r1.status.conectada ? 5 : 6);
    i0.ɵɵadvance(2);
    i0.ɵɵconditional(ctx_r1.status.email ? 7 : -1);
    i0.ɵɵadvance(6);
    i0.ɵɵtextInterpolate(ctx_r1.status.citasSincronizadas);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(ctx_r1.status.citasPendientes);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(ctx_r1.status.citasConError);
    i0.ɵɵadvance(3);
    i0.ɵɵconditional(!ctx_r1.status.conectada ? 20 : -1);
    i0.ɵɵadvance();
    i0.ɵɵconditional(ctx_r1.status.conectada ? 21 : -1);
    i0.ɵɵadvance();
    i0.ɵɵconditional(ctx_r1.syncResult ? 22 : -1);
} }
export class CalendarioComponent {
    api;
    status;
    syncResult;
    error = '';
    constructor(api) {
        this.api = api;
        this.cargar();
    }
    cargar() {
        this.api.get('/google/status').subscribe({
            next: (r) => {
                this.status = r;
                this.syncResult = undefined;
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    connect() {
        this.api.get('/google/connect').subscribe({
            next: (r) => {
                if (r.authUrl) {
                    window.open(r.authUrl, '_blank');
                }
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    seleccionar(c) {
        this.api.post(`/google/calendars/${c.id}/select`, {}).subscribe({
            next: () => this.cargar(),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    sync() {
        this.api.post('/google/sync', {}).subscribe({
            next: (r) => {
                this.syncResult = r;
                this.cargar();
            },
            error: (e) => (this.error = this.msg(e)),
        });
    }
    disconnect() {
        if (!confirm('¿Desconectar la cuenta de Google y borrar sus tokens?'))
            return;
        this.api.del('/google/disconnect').subscribe({
            next: () => this.cargar(),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    msg(e) {
        const a = e;
        return (a?.status === 400 || a?.status === 409) && a.error?.message ? a.error.message : 'Error de conexión';
    }
    static ɵfac = function CalendarioComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || CalendarioComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: CalendarioComponent, selectors: [["app-calendario"]], decls: 8, vars: 4, consts: [[1, "card"], [1, "between"], [1, "flex", "gap"], [1, "msg", "error"], [1, "msg", "warn"], [1, "btn", "primary", 3, "click"], [1, "btn", "danger", 3, "click"], [1, "grid2"], [1, "stat"], [1, "stat-label"], [1, "stat-value"], [1, "badge", "ok"], [1, "badge", "dim"], [1, "muted"], ["title", "Sincronizadas", 1, "badge", "ok"], ["title", "Pendientes", 1, "badge", "warn"], ["title", "Con error", 1, "badge", "bad"], [1, "between", 2, "margin-top", "16px"], [1, "msg", "ok"], [2, "margin-top", "20px"], [1, "tbl"], [1, "btn", "small"], [1, "btn", "small", 3, "click"], ["colspan", "3", 1, "empty"]], template: function CalendarioComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵdomElementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Google Calendar");
            i0.ɵɵdomElementEnd();
            i0.ɵɵconditionalCreate(4, CalendarioComponent_Conditional_4_Template, 5, 0, "div", 2);
            i0.ɵɵdomElementEnd();
            i0.ɵɵconditionalCreate(5, CalendarioComponent_Conditional_5_Template, 2, 1, "div", 3);
            i0.ɵɵconditionalCreate(6, CalendarioComponent_Conditional_6_Template, 14, 0, "div", 4);
            i0.ɵɵconditionalCreate(7, CalendarioComponent_Conditional_7_Template, 23, 8);
            i0.ɵɵdomElementEnd();
        } if (rf & 2) {
            i0.ɵɵadvance(4);
            i0.ɵɵconditional(ctx.status?.conectada ? 4 : -1);
            i0.ɵɵadvance();
            i0.ɵɵconditional(ctx.error ? 5 : -1);
            i0.ɵɵadvance();
            i0.ɵɵconditional(ctx.status && !ctx.status.configurada ? 6 : -1);
            i0.ɵɵadvance();
            i0.ɵɵconditional(ctx.status && ctx.status.configurada ? 7 : -1);
        } }, dependencies: [CommonModule], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(CalendarioComponent, [{
        type: Component,
        args: [{
                selector: 'app-calendario',
                template: `
    <div class="card">
      <div class="between">
        <h2>Google Calendar</h2>
        @if (status?.conectada) {
          <div class="flex gap">
            <button class="btn primary" (click)="sync()">Sincronizar citas</button>
            <button class="btn danger" (click)="disconnect()">Desconectar</button>
          </div>
        }
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      @if (status && !status.configurada) {
        <div class="msg warn">
          La integración con Google no está configurada. Agrega las credenciales OAuth
          (<code>google.client-id</code> / <code>google.client-secret</code> / <code>google.redirect-uri</code>)
          en <code>application.properties</code> y reinicia el backend.
        </div>
      }

      @if (status && status.configurada) {
        <div class="grid2">
          <div class="stat">
            <div class="stat-label">Conexión</div>
            <div class="stat-value">
              @if (status.conectada) {
                <span class="badge ok">Conectada</span>
              } @else {
                <span class="badge dim">Sin conectar</span>
              }
            </div>
            @if (status.email) {
              <div class="muted">{{ status.email }}</div>
            }
          </div>
          <div class="stat">
            <div class="stat-label">Sincronización de citas</div>
            <div class="flex gap">
              <span class="badge ok" title="Sincronizadas">{{ status.citasSincronizadas }}</span>
              <span class="badge warn" title="Pendientes">{{ status.citasPendientes }}</span>
              <span class="badge bad" title="Con error">{{ status.citasConError }}</span>
            </div>
            <div class="muted">sincronizadas · pendientes · errores</div>
          </div>
        </div>

        @if (!status.conectada) {
          <div class="between" style="margin-top:16px">
            <p class="muted">Conecta una cuenta de Google para que las citas se reflejen en tu calendario.</p>
            <button class="btn primary" (click)="connect()">Conectar con Google</button>
          </div>
        }

        @if (status.conectada) {
          <h3 style="margin-top:20px">Calendarios de la cuenta</h3>
          <table class="tbl">
            <thead><tr><th>Calendario</th><th>Zona horaria</th><th></th></tr></thead>
            <tbody>
              @for (c of status.calendarios; track c.id) {
                <tr>
                  <td>
                    {{ c.summary }}
                    @if (c.seleccionado) {
                      <span class="badge ok">Seleccionado</span>
                    }
                  </td>
                  <td class="muted">{{ c.timeZone }}</td>
                  <td>
                    @if (!c.seleccionado) {
                      <button class="btn small" (click)="seleccionar(c)">Usar</button>
                    }
                  </td>
                </tr>
              } @empty {
                <tr><td colspan="3" class="empty">No se encontraron calendarios.</td></tr>
              }
            </tbody>
          </table>
        }

        @if (syncResult) {
          <div class="msg ok">
            Sincronización completa: {{ syncResult.creados }} creados, {{ syncResult.actualizados }} actualizados,
            {{ syncResult.cancelados }} cancelados, {{ syncResult.errores }} errores,
            {{ syncResult.sincronizadas }} citas sincronizadas.
          </div>
        }
      }
    </div>
  `,
                imports: [CommonModule],
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(CalendarioComponent, { className: "CalendarioComponent", filePath: "src/app/google/calendario.ts", lineNumber: 102 }); })();
