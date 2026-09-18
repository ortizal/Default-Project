import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/forms";
import * as i3 from "@angular/common";
const _forTrack0 = ($index, $item) => $item.id;
function AgenteComponent_Conditional_10_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 6);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function AgenteComponent_For_29_Template(rf, ctx) { if (rf & 1) {
    const _r2 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "tr")(1, "td");
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "td");
    i0.ɵɵtext(4);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(5, "td")(6, "span", 8);
    i0.ɵɵtext(7);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(8, "td")(9, "button", 9);
    i0.ɵɵlistener("click", function AgenteComponent_For_29_Template_button_click_9_listener() { const c_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.toggleAgente(c_r3)); });
    i0.ɵɵtext(10);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(11, "td")(12, "span", 10);
    i0.ɵɵtext(13);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(14, "td", 11);
    i0.ɵɵtext(15);
    i0.ɵɵelementStart(16, "span", 12);
    i0.ɵɵtext(17);
    i0.ɵɵpipe(18, "date");
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(19, "td")(20, "button", 13);
    i0.ɵɵlistener("click", function AgenteComponent_For_29_Template_button_click_20_listener() { const c_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r0.transferir(c_r3)); });
    i0.ɵɵtext(21, "Transferir a humano");
    i0.ɵɵelementEnd()()();
} if (rf & 2) {
    const c_r3 = ctx.$implicit;
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r3.nombreContacto || c_r3.telefono);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r3.pacienteNombre || "\u2014");
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(c_r3.intencion || "\u2014");
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", c_r3.agenteActivo ? "" : "primary");
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate1(" ", c_r3.agenteActivo ? "On" : "Off", " ");
    i0.ɵɵadvance(2);
    i0.ɵɵproperty("ngClass", ctx_r0.badge(c_r3.estado));
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(c_r3.estado);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate1(" ", c_r3.ultimoMensaje || "\u2014", " ");
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate1("(", i0.ɵɵpipeBind2(18, 9, c_r3.ultimoMensajeAt, "HH:mm"), ")");
} }
function AgenteComponent_ForEmpty_30_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "tr")(1, "td", 14);
    i0.ɵɵtext(2, "Sin conversaciones del agente.");
    i0.ɵɵelementEnd()();
} }
export class AgenteComponent {
    api;
    items = [];
    q = '';
    error = '';
    constructor(api) {
        this.api = api;
        this.cargar();
    }
    cargar() {
        this.api.get(`/agente/conversaciones?q=${encodeURIComponent(this.q)}`).subscribe({
            next: (r) => (this.items = r),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    toggleAgente(c) {
        this.api.post(`/agente/conversaciones/${c.id}/agente`, { activo: !c.agenteActivo }).subscribe({
            next: () => this.cargar(),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    transferir(c) {
        if (!confirm(`¿Transferir la conversación de ${c.nombreContacto || c.telefono} a atención humana?`))
            return;
        this.api.post(`/agente/conversaciones/${c.id}/transferir`, {}).subscribe({
            next: () => this.cargar(),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    badge(estado) {
        switch (estado) {
            case 'BOT':
                return 'info';
            case 'ATENCION_HUMANA':
                return 'warn';
            case 'ATENDIDA':
                return 'ok';
            case 'CERRADA':
                return 'dim';
            default:
                return 'dim';
        }
    }
    msg(e) {
        const a = e;
        if (a?.status === 403)
            return 'Sin permisos para ver el agente';
        return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
    }
    static ɵfac = function AgenteComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || AgenteComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: AgenteComponent, selectors: [["app-agente"]], decls: 31, vars: 3, consts: [[1, "card"], [1, "between"], [1, "flex"], ["placeholder", "Buscar por tel\u00E9fono o nombre", 1, "input", 2, "width", "220px", 3, "ngModelChange", "keyup.enter", "ngModel"], [1, "btn", 3, "click"], [1, "btn", "small", 3, "click"], [1, "msg", "error"], [1, "tbl"], [1, "badge", "info"], [1, "btn", "small", 3, "click", "ngClass"], [1, "badge", 3, "ngClass"], [1, "muted", 2, "max-width", "260px", "overflow", "hidden", "text-overflow", "ellipsis", "white-space", "nowrap"], [2, "font-size", "11px"], [1, "btn", "small", "danger", 3, "click"], ["colspan", "7", 1, "empty"]], template: function AgenteComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h2");
            i0.ɵɵtext(3, "Supervisi\u00F3n del agente IA");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "div", 2)(5, "input", 3);
            i0.ɵɵtwoWayListener("ngModelChange", function AgenteComponent_Template_input_ngModelChange_5_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.q, $event) || (ctx.q = $event); return $event; });
            i0.ɵɵlistener("keyup.enter", function AgenteComponent_Template_input_keyup_enter_5_listener() { return ctx.cargar(); });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(6, "button", 4);
            i0.ɵɵlistener("click", function AgenteComponent_Template_button_click_6_listener() { return ctx.cargar(); });
            i0.ɵɵtext(7, "Buscar");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(8, "button", 5);
            i0.ɵɵlistener("click", function AgenteComponent_Template_button_click_8_listener() { return ctx.cargar(); });
            i0.ɵɵtext(9, "\u27F3");
            i0.ɵɵelementEnd()()();
            i0.ɵɵconditionalCreate(10, AgenteComponent_Conditional_10_Template, 2, 1, "div", 6);
            i0.ɵɵelementStart(11, "table", 7)(12, "thead")(13, "tr")(14, "th");
            i0.ɵɵtext(15, "Contacto");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(16, "th");
            i0.ɵɵtext(17, "Paciente");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(18, "th");
            i0.ɵɵtext(19, "Intenci\u00F3n");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(20, "th");
            i0.ɵɵtext(21, "Agente");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(22, "th");
            i0.ɵɵtext(23, "Estado");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(24, "th");
            i0.ɵɵtext(25, "\u00DAltimo mensaje");
            i0.ɵɵelementEnd();
            i0.ɵɵelement(26, "th");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(27, "tbody");
            i0.ɵɵrepeaterCreate(28, AgenteComponent_For_29_Template, 22, 12, "tr", null, _forTrack0, false, AgenteComponent_ForEmpty_30_Template, 3, 0, "tr");
            i0.ɵɵelementEnd()()();
        } if (rf & 2) {
            i0.ɵɵadvance(5);
            i0.ɵɵtwoWayProperty("ngModel", ctx.q);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(5);
            i0.ɵɵconditional(ctx.error ? 10 : -1);
            i0.ɵɵadvance(18);
            i0.ɵɵrepeater(ctx.items);
        } }, dependencies: [FormsModule, i2.DefaultValueAccessor, i2.NgControlStatus, i2.NgModel, CommonModule, i3.NgClass, i3.DatePipe], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(AgenteComponent, [{
        type: Component,
        args: [{
                selector: 'app-agente',
                template: `
    <div class="card">
      <div class="between">
        <h2>Supervisión del agente IA</h2>
        <div class="flex">
          <input class="input" style="width:220px" [(ngModel)]="q" (keyup.enter)="cargar()" placeholder="Buscar por teléfono o nombre" />
          <button class="btn" (click)="cargar()">Buscar</button>
          <button class="btn small" (click)="cargar()">⟳</button>
        </div>
      </div>
      @if (error) {
        <div class="msg error">{{ error }}</div>
      }
      <table class="tbl">
        <thead><tr><th>Contacto</th><th>Paciente</th><th>Intención</th><th>Agente</th><th>Estado</th><th>Último mensaje</th><th></th></tr></thead>
        <tbody>
          @for (c of items; track c.id) {
            <tr>
              <td>{{ c.nombreContacto || c.telefono }}</td>
              <td>{{ c.pacienteNombre || '—' }}</td>
              <td><span class="badge info">{{ c.intencion || '—' }}</span></td>
              <td>
                <button class="btn small" [ngClass]="c.agenteActivo ? '' : 'primary'" (click)="toggleAgente(c)">
                  {{ c.agenteActivo ? 'On' : 'Off' }}
                </button>
              </td>
              <td><span class="badge" [ngClass]="badge(c.estado)">{{ c.estado }}</span></td>
              <td class="muted" style="max-width:260px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap">
                {{ c.ultimoMensaje || '—' }} <span style="font-size:11px">({{ c.ultimoMensajeAt | date:'HH:mm' }})</span>
              </td>
              <td>
                <button class="btn small danger" (click)="transferir(c)">Transferir a humano</button>
              </td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="empty">Sin conversaciones del agente.</td></tr>
          }
        </tbody>
      </table>
    </div>
  `,
                imports: [FormsModule, CommonModule],
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(AgenteComponent, { className: "AgenteComponent", filePath: "src/app/agente/agente.ts", lineNumber: 52 }); })();
