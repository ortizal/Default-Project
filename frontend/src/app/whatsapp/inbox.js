import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
import * as i3 from "@angular/forms";
const _c0 = a0 => ({ sel: a0 });
const _c1 = (a0, a1) => ({ in: a0, out: a1 });
const _forTrack0 = ($index, $item) => $item.id;
function InboxComponent_For_10_Conditional_8_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵtext(0);
    i0.ɵɵpipe(1, "date");
} if (rf & 2) {
    const c_r2 = i0.ɵɵnextContext().$implicit;
    i0.ɵɵtextInterpolate1(" \u00B7 ", i0.ɵɵpipeBind2(1, 1, c_r2.ultimoMensajeAt, "HH:mm"), " ");
} }
function InboxComponent_For_10_Template(rf, ctx) { if (rf & 1) {
    const _r1 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 9);
    i0.ɵɵlistener("click", function InboxComponent_For_10_Template_div_click_0_listener() { const c_r2 = i0.ɵɵrestoreView(_r1).$implicit; const ctx_r2 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r2.abrir(c_r2)); });
    i0.ɵɵelementStart(1, "div", 2)(2, "strong");
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "span", 10);
    i0.ɵɵtext(5);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(6, "div", 11);
    i0.ɵɵtext(7);
    i0.ɵɵconditionalCreate(8, InboxComponent_For_10_Conditional_8_Template, 2, 4);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(9, "div", 12);
    i0.ɵɵtext(10);
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const c_r2 = ctx.$implicit;
    const ctx_r2 = i0.ɵɵnextContext();
    i0.ɵɵproperty("ngClass", i0.ɵɵpureFunction1(7, _c0, ctx_r2.seleccion?.id === c_r2.id));
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(c_r2.nombreContacto || c_r2.telefono);
    i0.ɵɵadvance();
    i0.ɵɵproperty("ngClass", ctx_r2.badge(c_r2.estado));
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(c_r2.estado);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate1(" ", c_r2.pacienteNombres || c_r2.telefono, " ");
    i0.ɵɵadvance();
    i0.ɵɵconditional(c_r2.ultimoMensajeAt ? 8 : -1);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r2.ultimoMensaje);
} }
function InboxComponent_ForEmpty_11_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 7);
    i0.ɵɵtext(1, "Sin conversaciones.");
    i0.ɵɵelementEnd();
} }
function InboxComponent_Conditional_13_For_20_Conditional_6_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵtext(0);
} if (rf & 2) {
    const m_r5 = i0.ɵɵnextContext().$implicit;
    i0.ɵɵtextInterpolate1(" \u00B7 ", m_r5.estado, " ");
} }
function InboxComponent_Conditional_13_For_20_Conditional_7_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 29);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const m_r5 = i0.ɵɵnextContext().$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(m_r5.error);
} }
function InboxComponent_Conditional_13_For_20_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 24)(1, "div");
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "div", 28);
    i0.ɵɵtext(4);
    i0.ɵɵpipe(5, "date");
    i0.ɵɵconditionalCreate(6, InboxComponent_Conditional_13_For_20_Conditional_6_Template, 1, 1);
    i0.ɵɵelementEnd();
    i0.ɵɵconditionalCreate(7, InboxComponent_Conditional_13_For_20_Conditional_7_Template, 2, 1, "div", 29);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const m_r5 = ctx.$implicit;
    i0.ɵɵproperty("ngClass", i0.ɵɵpureFunction2(8, _c1, m_r5.direccion === "INBOUND", m_r5.direccion === "OUTBOUND"));
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(m_r5.texto);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate1("", i0.ɵɵpipeBind2(5, 5, m_r5.receivedAt, "dd/MM HH:mm"), " ");
    i0.ɵɵadvance(2);
    i0.ɵɵconditional(m_r5.direccion === "OUTBOUND" ? 6 : -1);
    i0.ɵɵadvance();
    i0.ɵɵconditional(m_r5.error ? 7 : -1);
} }
function InboxComponent_Conditional_13_ForEmpty_21_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 7);
    i0.ɵɵtext(1, "Sin mensajes.");
    i0.ɵɵelementEnd();
} }
function InboxComponent_Conditional_13_Template(rf, ctx) { if (rf & 1) {
    const _r4 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 13)(1, "div")(2, "h2", 14);
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(4, "div", 15);
    i0.ɵɵtext(5);
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(6, "div", 16)(7, "select", 17);
    i0.ɵɵtwoWayListener("ngModelChange", function InboxComponent_Conditional_13_Template_select_ngModelChange_7_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r2 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r2.nuevoEstado, $event) || (ctx_r2.nuevoEstado = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵlistener("change", function InboxComponent_Conditional_13_Template_select_change_7_listener() { i0.ɵɵrestoreView(_r4); const ctx_r2 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r2.cambiarEstado()); });
    i0.ɵɵelementStart(8, "option", 18);
    i0.ɵɵtext(9, "Cambiar estado\u2026");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(10, "option", 19);
    i0.ɵɵtext(11, "Bot");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(12, "option", 20);
    i0.ɵɵtext(13, "Atenci\u00F3n humana");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(14, "option", 21);
    i0.ɵɵtext(15, "Atendida");
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(16, "option", 22);
    i0.ɵɵtext(17, "Cerrada");
    i0.ɵɵelementEnd()();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementEnd()();
    i0.ɵɵelementStart(18, "div", 23);
    i0.ɵɵrepeaterCreate(19, InboxComponent_Conditional_13_For_20_Template, 8, 11, "div", 24, _forTrack0, false, InboxComponent_Conditional_13_ForEmpty_21_Template, 2, 0, "div", 7);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(22, "form", 25);
    i0.ɵɵlistener("ngSubmit", function InboxComponent_Conditional_13_Template_form_ngSubmit_22_listener() { i0.ɵɵrestoreView(_r4); const ctx_r2 = i0.ɵɵnextContext(); return i0.ɵɵresetView(ctx_r2.enviar()); });
    i0.ɵɵelementStart(23, "input", 26);
    i0.ɵɵtwoWayListener("ngModelChange", function InboxComponent_Conditional_13_Template_input_ngModelChange_23_listener($event) { i0.ɵɵrestoreView(_r4); const ctx_r2 = i0.ɵɵnextContext(); i0.ɵɵtwoWayBindingSet(ctx_r2.texto, $event) || (ctx_r2.texto = $event); return i0.ɵɵresetView($event); });
    i0.ɵɵelementEnd();
    i0.ɵɵcontrolCreate();
    i0.ɵɵelementStart(24, "button", 27);
    i0.ɵɵtext(25, "Enviar");
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const ctx_r2 = i0.ɵɵnextContext();
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(ctx_r2.detalle.conversacion.nombreContacto || ctx_r2.detalle.conversacion.telefono);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate2(" Sesi\u00F3n ", ctx_r2.detalle.conversacion.sesionId, " \u00B7 Paciente: ", ctx_r2.detalle.conversacion.pacienteNombres || "no asociado", " ");
    i0.ɵɵadvance(2);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r2.nuevoEstado);
    i0.ɵɵcontrol();
    i0.ɵɵadvance(12);
    i0.ɵɵrepeater(ctx_r2.detalle.mensajes);
    i0.ɵɵadvance(4);
    i0.ɵɵtwoWayProperty("ngModel", ctx_r2.texto);
    i0.ɵɵcontrol();
    i0.ɵɵadvance();
    i0.ɵɵproperty("disabled", !ctx_r2.texto.trim() || ctx_r2.enviando);
} }
function InboxComponent_Conditional_14_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 7);
    i0.ɵɵtext(1, "Selecciona una conversaci\u00F3n del panel izquierdo.");
    i0.ɵɵelementEnd();
} }
export class InboxComponent {
    api;
    conversaciones = [];
    seleccion = null;
    detalle = null;
    q = '';
    texto = '';
    nuevoEstado = '';
    enviando = false;
    error = '';
    timer = null;
    constructor(api) {
        this.api = api;
        this.cargarLista();
        this.timer = setInterval(() => this.poll(), 6000);
    }
    ngOnDestroy() {
        if (this.timer)
            clearInterval(this.timer);
    }
    poll() {
        if (!document.hidden)
            this.cargarLista(true);
    }
    cargarLista(silencioso = false) {
        this.api.get(`/whatsapp/conversaciones?q=${encodeURIComponent(this.q)}`).subscribe({
            next: (r) => {
                this.conversaciones = r;
                if (this.seleccion) {
                    const a = r.find((c) => c.id === this.seleccion.id);
                    if (a)
                        this.seleccion = a;
                    this.abrir(this.seleccion);
                }
            },
            error: (e) => {
                if (!silencioso)
                    this.error = this.msg(e);
            },
        });
    }
    abrir(c) {
        this.seleccion = c;
        this.nuevoEstado = '';
        this.api.get(`/whatsapp/conversaciones/${c.id}`).subscribe({
            next: (d) => (this.detalle = d),
            error: (e) => (this.error = this.msg(e)),
        });
    }
    enviar() {
        if (!this.detalle || !this.texto.trim())
            return;
        this.enviando = true;
        this.api
            .post(`/whatsapp/conversaciones/${this.detalle.conversacion.id}/mensajes`, { texto: this.texto.trim() })
            .subscribe({
            next: (m) => {
                this.texto = '';
                this.detalle.mensajes = [...this.detalle.mensajes, m];
                this.enviando = false;
            },
            error: (e) => {
                this.error = this.msg(e);
                this.enviando = false;
            },
        });
    }
    cambiarEstado() {
        if (!this.detalle || !this.nuevoEstado)
            return;
        this.api
            .post(`/whatsapp/conversaciones/${this.detalle.conversacion.id}/estado`, { estado: this.nuevoEstado })
            .subscribe({
            next: (c) => {
                this.seleccion = c;
                this.cargarLista();
                this.nuevoEstado = '';
            },
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
        return a?.status === 409 || a?.status === 400 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
    }
    static ɵfac = function InboxComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || InboxComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: InboxComponent, selectors: [["app-inbox"]], decls: 15, vars: 3, consts: [[1, "grid", "cols-2", 2, "grid-template-columns", "340px 1fr"], [1, "card", 2, "margin", "0"], [1, "between"], [1, "btn", "small", 3, "click"], ["placeholder", "Buscar por tel\u00E9fono o nombre", 1, "input", 3, "ngModelChange", "keyup.enter", "ngModel"], [2, "margin-top", "10px", "max-height", "62vh", "overflow", "auto"], [1, "conv", "clickable", 3, "ngClass"], [1, "empty"], [1, "card", 2, "margin", "0", "display", "flex", "flex-direction", "column"], [1, "conv", "clickable", 3, "click", "ngClass"], [1, "badge", 3, "ngClass"], [1, "muted", 2, "font-size", "12px", "margin-top", "2px"], [1, "muted", 2, "font-size", "12px", "white-space", "nowrap", "overflow", "hidden", "text-overflow", "ellipsis"], [1, "between", 2, "border-bottom", "1px solid var(--border)", "padding-bottom", "10px"], [2, "margin", "0"], [1, "muted"], [1, "flex"], [1, "input", 2, "width", "200px", 3, "ngModelChange", "change", "ngModel"], ["value", "", "disabled", ""], ["value", "BOT"], ["value", "ATENCION_HUMANA"], ["value", "ATENDIDA"], ["value", "CERRADA"], [1, "chat", 2, "flex", "1", "max-height", "52vh", "overflow", "auto", "padding", "12px 0"], [1, "bubble", 3, "ngClass"], [1, "flex", 3, "ngSubmit"], ["name", "texto", "placeholder", "Escribe un mensaje\u2026", "required", "", 1, "input", 3, "ngModelChange", "ngModel"], [1, "btn", "primary", 3, "disabled"], [1, "muted", 2, "font-size", "11px", "text-align", "right"], [1, "msg", "error", 2, "margin", "4px 0 0"]], template: function InboxComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "div", 2)(3, "h2");
            i0.ɵɵtext(4, "Conversaciones");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(5, "button", 3);
            i0.ɵɵlistener("click", function InboxComponent_Template_button_click_5_listener() { return ctx.cargarLista(); });
            i0.ɵɵtext(6, "\u27F3");
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(7, "input", 4);
            i0.ɵɵtwoWayListener("ngModelChange", function InboxComponent_Template_input_ngModelChange_7_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.q, $event) || (ctx.q = $event); return $event; });
            i0.ɵɵlistener("keyup.enter", function InboxComponent_Template_input_keyup_enter_7_listener() { return ctx.cargarLista(); });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(8, "div", 5);
            i0.ɵɵrepeaterCreate(9, InboxComponent_For_10_Template, 11, 9, "div", 6, _forTrack0, false, InboxComponent_ForEmpty_11_Template, 2, 0, "div", 7);
            i0.ɵɵelementEnd()();
            i0.ɵɵelementStart(12, "div", 8);
            i0.ɵɵconditionalCreate(13, InboxComponent_Conditional_13_Template, 26, 7)(14, InboxComponent_Conditional_14_Template, 2, 0, "div", 7);
            i0.ɵɵelementEnd()();
        } if (rf & 2) {
            i0.ɵɵadvance(7);
            i0.ɵɵtwoWayProperty("ngModel", ctx.q);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(2);
            i0.ɵɵrepeater(ctx.conversaciones);
            i0.ɵɵadvance(4);
            i0.ɵɵconditional(ctx.detalle && ctx.seleccion ? 13 : 14);
        } }, dependencies: [CommonModule, i2.NgClass, FormsModule, i3.ɵNgNoValidate, i3.NgSelectOption, i3.ɵNgSelectMultipleOption, i3.DefaultValueAccessor, i3.SelectControlValueAccessor, i3.NgControlStatus, i3.NgControlStatusGroup, i3.RequiredValidator, i3.NgModel, i3.NgForm, i2.DatePipe], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(InboxComponent, [{
        type: Component,
        args: [{
                selector: 'app-inbox',
                template: `
    <div class="grid cols-2" style="grid-template-columns:340px 1fr">
      <div class="card" style="margin:0">
        <div class="between">
          <h2>Conversaciones</h2>
          <button class="btn small" (click)="cargarLista()">⟳</button>
        </div>
        <input class="input" placeholder="Buscar por teléfono o nombre" [(ngModel)]="q" (keyup.enter)="cargarLista()" />
        <div style="margin-top:10px; max-height:62vh; overflow:auto">
          @for (c of conversaciones; track c.id) {
            <div class="conv clickable" [ngClass]="{ sel: seleccion?.id === c.id }" (click)="abrir(c)">
              <div class="between">
                <strong>{{ c.nombreContacto || c.telefono }}</strong>
                <span class="badge" [ngClass]="badge(c.estado)">{{ c.estado }}</span>
              </div>
              <div class="muted" style="font-size:12px; margin-top:2px">
                {{ c.pacienteNombres || c.telefono }}
                @if (c.ultimoMensajeAt) {
                  · {{ c.ultimoMensajeAt | date:'HH:mm' }}
                }
              </div>
              <div class="muted" style="font-size:12px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis">{{ c.ultimoMensaje }}</div>
            </div>
          } @empty {
            <div class="empty">Sin conversaciones.</div>
          }
        </div>
      </div>

      <div class="card" style="margin:0; display:flex; flex-direction:column">
        @if (detalle && seleccion) {
          <div class="between" style="border-bottom:1px solid var(--border); padding-bottom:10px">
            <div>
              <h2 style="margin:0">{{ detalle.conversacion.nombreContacto || detalle.conversacion.telefono }}</h2>
              <div class="muted">
                Sesión {{ detalle.conversacion.sesionId }} · Paciente: {{ detalle.conversacion.pacienteNombres || 'no asociado' }}
              </div>
            </div>
            <div class="flex">
              <select class="input" style="width:200px" [(ngModel)]="nuevoEstado" (change)="cambiarEstado()">
                <option value="" disabled>Cambiar estado…</option>
                <option value="BOT">Bot</option>
                <option value="ATENCION_HUMANA">Atención humana</option>
                <option value="ATENDIDA">Atendida</option>
                <option value="CERRADA">Cerrada</option>
              </select>
            </div>
          </div>
          <div class="chat" style="flex:1; max-height:52vh; overflow:auto; padding:12px 0">
            @for (m of detalle.mensajes; track m.id) {
              <div class="bubble" [ngClass]="{ in: m.direccion === 'INBOUND', out: m.direccion === 'OUTBOUND' }">
                <div>{{ m.texto }}</div>
                <div class="muted" style="font-size:11px; text-align:right">{{ m.receivedAt | date:'dd/MM HH:mm' }} @if (m.direccion === 'OUTBOUND') { · {{ m.estado }} }</div>
                @if (m.error) {
                  <div class="msg error" style="margin:4px 0 0">{{ m.error }}</div>
                }
              </div>
            } @empty {
              <div class="empty">Sin mensajes.</div>
            }
          </div>
          <form class="flex" (ngSubmit)="enviar()">
            <input class="input" [(ngModel)]="texto" name="texto" placeholder="Escribe un mensaje…" required />
            <button class="btn primary" [disabled]="!texto.trim() || enviando">Enviar</button>
          </form>
        } @else {
          <div class="empty">Selecciona una conversación del panel izquierdo.</div>
        }
      </div>
    </div>
  `,
                imports: [CommonModule, FormsModule],
            }]
    }], () => [{ type: i1.Api }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(InboxComponent, { className: "InboxComponent", filePath: "src/app/whatsapp/inbox.ts", lineNumber: 82 }); })();
