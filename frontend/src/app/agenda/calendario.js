import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as i0 from "@angular/core";
import * as i1 from "../core/api";
import * as i2 from "@angular/common";
const _c0 = (a0, a1, a2) => ({ hoy: a0, festivo: a1, bloqueado: a2 });
const _c1 = () => ["Lunes", "Martes", "Mi\u00E9rcoles", "Jueves", "Viernes", "S\u00E1bado", "Domingo"];
const _c2 = (a0, a1, a2, a3) => ({ hoy: a0, otro: a1, festivo: a2, bloqueado: a3 });
const _forTrack0 = ($index, $item) => $item.iso;
const _forTrack1 = ($index, $item) => $item.id;
function CalendarioComponent_Conditional_7_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 2);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function CalendarioComponent_Conditional_8_For_2_Conditional_4_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "span", 6);
    i0.ɵɵtext(1, "sin horario");
    i0.ɵɵelementEnd();
} }
function CalendarioComponent_Conditional_8_For_2_Conditional_5_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "span", 7);
    i0.ɵɵtext(1, "bloqueo");
    i0.ɵɵelementEnd();
} }
function CalendarioComponent_Conditional_8_For_2_For_8_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 9)(1, "span", 11);
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "span", 12);
    i0.ɵɵtext(4);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(5, "span", 13);
    i0.ɵɵtext(6);
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const c_r4 = ctx.$implicit;
    const ctx_r0 = i0.ɵɵnextContext(3);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r4.horaInicio);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r4.pacienteNombre);
    i0.ɵɵadvance();
    i0.ɵɵproperty("ngClass", ctx_r0.badge(c_r4.estado));
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(c_r4.estadoNombre);
} }
function CalendarioComponent_Conditional_8_For_2_Conditional_9_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 10);
    i0.ɵɵtext(1, "\u2014");
    i0.ɵɵelementEnd();
} }
function CalendarioComponent_Conditional_8_For_2_Template(rf, ctx) { if (rf & 1) {
    const _r2 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 4)(1, "div", 5);
    i0.ɵɵlistener("click", function CalendarioComponent_Conditional_8_For_2_Template_div_click_1_listener() { const d_r3 = i0.ɵɵrestoreView(_r2).$implicit; const ctx_r0 = i0.ɵɵnextContext(2); return i0.ɵɵresetView(ctx_r0.irDia.emit(d_r3.iso)); });
    i0.ɵɵelementStart(2, "strong");
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd();
    i0.ɵɵconditionalCreate(4, CalendarioComponent_Conditional_8_For_2_Conditional_4_Template, 2, 0, "span", 6)(5, CalendarioComponent_Conditional_8_For_2_Conditional_5_Template, 2, 0, "span", 7);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(6, "div", 8);
    i0.ɵɵrepeaterCreate(7, CalendarioComponent_Conditional_8_For_2_For_8_Template, 7, 4, "div", 9, _forTrack1);
    i0.ɵɵconditionalCreate(9, CalendarioComponent_Conditional_8_For_2_Conditional_9_Template, 2, 0, "div", 10);
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const d_r3 = ctx.$implicit;
    const ctx_r0 = i0.ɵɵnextContext(2);
    i0.ɵɵproperty("ngClass", i0.ɵɵpureFunction3(4, _c0, d_r3.iso === ctx_r0.hoy, d_r3.inactivo, d_r3.bloqueado));
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(d_r3.iso);
    i0.ɵɵadvance();
    i0.ɵɵconditional(d_r3.inactivo ? 4 : d_r3.bloqueado ? 5 : -1);
    i0.ɵɵadvance(3);
    i0.ɵɵrepeater(d_r3.citas);
    i0.ɵɵadvance(2);
    i0.ɵɵconditional(!d_r3.citas.length && (d_r3.inactivo || d_r3.bloqueado) ? 9 : -1);
} }
function CalendarioComponent_Conditional_8_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 3);
    i0.ɵɵrepeaterCreate(1, CalendarioComponent_Conditional_8_For_2_Template, 10, 8, "div", 4, _forTrack0);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵrepeater(ctx_r0.dias);
} }
function CalendarioComponent_Conditional_9_For_2_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 15);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const d_r5 = ctx.$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(d_r5);
} }
function CalendarioComponent_Conditional_9_For_5_Conditional_4_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "span", 19);
    i0.ɵɵtext(1, "B");
    i0.ɵɵelementEnd();
} }
function CalendarioComponent_Conditional_9_For_5_For_7_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 9)(1, "span", 11);
    i0.ɵɵtext(2);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "span", 12);
    i0.ɵɵtext(4);
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const c_r8 = ctx.$implicit;
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r8.horaInicio);
    i0.ɵɵadvance(2);
    i0.ɵɵtextInterpolate(c_r8.pacienteNombre);
} }
function CalendarioComponent_Conditional_9_For_5_Conditional_8_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 20);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const d_r7 = i0.ɵɵnextContext().$implicit;
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate1("+", d_r7.citas.length - 3, " m\u00E1s");
} }
function CalendarioComponent_Conditional_9_For_5_Template(rf, ctx) { if (rf & 1) {
    const _r6 = i0.ɵɵgetCurrentView();
    i0.ɵɵelementStart(0, "div", 17);
    i0.ɵɵlistener("click", function CalendarioComponent_Conditional_9_For_5_Template_div_click_0_listener() { const d_r7 = i0.ɵɵrestoreView(_r6).$implicit; const ctx_r0 = i0.ɵɵnextContext(2); return i0.ɵɵresetView(ctx_r0.irDia.emit(d_r7.iso)); });
    i0.ɵɵelementStart(1, "div", 18)(2, "strong");
    i0.ɵɵtext(3);
    i0.ɵɵelementEnd();
    i0.ɵɵconditionalCreate(4, CalendarioComponent_Conditional_9_For_5_Conditional_4_Template, 2, 0, "span", 19);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(5, "div", 8);
    i0.ɵɵrepeaterCreate(6, CalendarioComponent_Conditional_9_For_5_For_7_Template, 5, 2, "div", 9, _forTrack1);
    i0.ɵɵconditionalCreate(8, CalendarioComponent_Conditional_9_For_5_Conditional_8_Template, 2, 1, "div", 20);
    i0.ɵɵelementEnd()();
} if (rf & 2) {
    const d_r7 = ctx.$implicit;
    const ctx_r0 = i0.ɵɵnextContext(2);
    i0.ɵɵproperty("ngClass", i0.ɵɵpureFunction4(4, _c2, d_r7.iso === ctx_r0.hoy, d_r7.enOtroMes, d_r7.inactivo, d_r7.bloqueado));
    i0.ɵɵadvance(3);
    i0.ɵɵtextInterpolate(d_r7.numero);
    i0.ɵɵadvance();
    i0.ɵɵconditional(d_r7.bloqueado ? 4 : -1);
    i0.ɵɵadvance(2);
    i0.ɵɵrepeater(d_r7.citas.slice(0, 3));
    i0.ɵɵadvance(2);
    i0.ɵɵconditional(d_r7.citas.length > 3 ? 8 : -1);
} }
function CalendarioComponent_Conditional_9_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 14);
    i0.ɵɵrepeaterCreate(1, CalendarioComponent_Conditional_9_For_2_Template, 2, 1, "div", 15, i0.ɵɵrepeaterTrackByIdentity);
    i0.ɵɵelementEnd();
    i0.ɵɵelementStart(3, "div", 16);
    i0.ɵɵrepeaterCreate(4, CalendarioComponent_Conditional_9_For_5_Template, 9, 9, "div", 4, _forTrack0);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵrepeater(i0.ɵɵpureFunction0(0, _c1));
    i0.ɵɵadvance(3);
    i0.ɵɵrepeater(ctx_r0.dias);
} }
export class CalendarioComponent {
    api;
    modo = 'semana';
    set fecha(v) {
        this.ancla = v;
        this.cargar();
    }
    set odontologoId(v) {
        this.odontologo = v ?? 0;
        this.horariosDia = undefined;
        this.cargar();
    }
    irDia = new EventEmitter();
    dias = [];
    titulo = '';
    hoy = new Date().toISOString().slice(0, 10);
    error = '';
    ancla = '';
    odontologo = 0;
    citas = [];
    bloqueos = [];
    horariosDia;
    constructor(api) {
        this.api = api;
    }
    cargar() {
        if (!this.ancla || !this.odontologo) {
            this.dias = [];
            return;
        }
        if (!this.horariosDia) {
            this.api.get(`/horarios?odontologoId=${this.odontologo}`).subscribe({
                next: (r) => {
                    this.horariosDia = new Set(r.filter((h) => h.estado === 'ACTIVO').map((h) => h.diaSemana));
                    this.construir();
                },
                error: () => this.construir(),
            });
        }
        const rango = this.rango();
        this.api
            .get(`/citas?desde=${rango.desde}&hasta=${rango.hasta}&page=0&size=1000&doctorId=${this.odontologo}`)
            .subscribe({
            next: (r) => {
                this.citas = r.content;
                this.construir();
            },
            error: (e) => (this.error = this.msg(e)),
        });
        this.api.get(`/agenda/bloqueos?odontologoId=${this.odontologo}&desde=${rango.desde}&hasta=${rango.hasta}`).subscribe({
            next: (r) => {
                this.bloqueos = r;
                this.construir();
            },
            error: () => undefined,
        });
    }
    rango() {
        const ancla = new Date(this.ancla + 'T12:00:00');
        if (this.modo === 'semana') {
            const dia = ancla.getDay();
            const diff = dia === 0 ? -6 : 1 - dia;
            const lunes = new Date(ancla);
            lunes.setDate(ancla.getDate() + diff);
            const domingo = new Date(lunes);
            domingo.setDate(lunes.getDate() + 6);
            return { desde: this.iso(lunes), hasta: this.iso(domingo) };
        }
        const primero = new Date(ancla.getFullYear(), ancla.getMonth(), 1);
        const ultimo = new Date(ancla.getFullYear(), ancla.getMonth() + 1, 0);
        return { desde: this.iso(primero), hasta: this.iso(ultimo) };
    }
    construir() {
        if (!this.ancla || !this.odontologo)
            return;
        const porFecha = new Map();
        for (const c of this.citas) {
            const lista = porFecha.get(c.fecha) ?? [];
            lista.push({ ...c, estadoNombre: this.nombreEstado(c.estado) });
            porFecha.set(c.fecha, lista);
        }
        const bloqueadas = new Set(this.bloqueos.map((b) => b.fecha));
        const celdas = [];
        if (this.modo === 'semana') {
            const { desde } = this.rango();
            const lunes = new Date(desde + 'T12:00:00');
            for (let i = 0; i < 7; i++) {
                const fecha = new Date(lunes);
                fecha.setDate(lunes.getDate() + i);
                const isoDate = this.iso(fecha);
                celdas.push(this.celda(isoDate, fecha.getDate(), false, porFecha, bloqueadas));
            }
            const r = this.rango();
            this.titulo = `${this.formato(r.desde)} — ${this.formato(r.hasta)}`;
        }
        else {
            const ancla = new Date(this.ancla + 'T12:00:00');
            const offset = (new Date(ancla.getFullYear(), ancla.getMonth(), 1).getDay() + 6) % 7;
            const total = new Date(ancla.getFullYear(), ancla.getMonth() + 1, 0).getDate();
            let cursor = -offset + 1;
            while (celdas.length < total + offset) {
                const fecha = new Date(ancla.getFullYear(), ancla.getMonth(), cursor);
                const otroMes = cursor < 1 || cursor > total;
                celdas.push(this.celda(this.iso(fecha), fecha.getDate(), otroMes, porFecha, bloqueadas));
                cursor++;
            }
            while (celdas.length % 7 !== 0) {
                const fecha = new Date(ancla.getFullYear(), ancla.getMonth(), cursor);
                celdas.push(this.celda(this.iso(fecha), fecha.getDate(), true, porFecha, bloqueadas));
                cursor++;
            }
            const m = ancla.toLocaleDateString('es-EC', { month: 'long', year: 'numeric' });
            this.titulo = m.charAt(0).toUpperCase() + m.slice(1);
        }
        this.dias = celdas;
    }
    celda(iso, numero, otroMes, porFecha, bloqueadas) {
        const fecha = new Date(iso + 'T12:00:00');
        const dia = ((fecha.getDay() + 6) % 7) + 1;
        const inactivo = this.horariosDia ? !this.horariosDia.has(dia) && !otroMes : false;
        const citas = (porFecha.get(iso) ?? []).sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));
        return { iso, numero, enOtroMes: otroMes, citas, bloqueado: bloqueadas.has(iso), inactivo };
    }
    navegar(dir) {
        const ancla = new Date(this.ancla + 'T12:00:00');
        if (this.modo === 'semana') {
            ancla.setDate(ancla.getDate() + dir * 7);
        }
        else {
            ancla.setMonth(ancla.getMonth() + dir);
        }
        this.ancla = this.iso(ancla);
        this.cargar();
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
    nombreEstado(estado) {
        return (estado || '').toLowerCase().replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
    }
    iso(d) {
        const m = String(d.getMonth() + 1).padStart(2, '0');
        const dd = String(d.getDate()).padStart(2, '0');
        return `${d.getFullYear()}-${m}-${dd}`;
    }
    formato(iso) {
        const d = new Date(iso + 'T12:00:00');
        return d.toLocaleDateString('es-EC', { day: 'numeric', month: 'short' });
    }
    msg(e) {
        const a = e;
        return a?.status === 400 || a?.status === 409 ? (a.error?.message ?? 'Datos inválidos') : 'Error de conexión';
    }
    static ɵfac = function CalendarioComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || CalendarioComponent)(i0.ɵɵdirectiveInject(i1.Api)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: CalendarioComponent, selectors: [["app-calendario"]], inputs: { modo: "modo", fecha: "fecha", odontologoId: "odontologoId" }, outputs: { irDia: "irDia" }, decls: 10, vars: 6, consts: [[1, "cal-head", "between"], [1, "btn", "small", 3, "click"], [1, "msg", "error"], [1, "cal-grid"], [1, "cal-cell", 3, "ngClass"], [1, "cal-day", 3, "click"], [1, "badge", "dim"], [1, "badge", "warn"], [1, "cal-items"], [1, "cal-item"], [1, "empty"], [1, "cal-hora"], [1, "cal-txt"], [1, "badge", 3, "ngClass"], [1, "cal-weekdays"], [1, "cal-wd"], [1, "cal-grid", "mes"], [1, "cal-cell", 3, "click", "ngClass"], [1, "cal-day"], ["title", "D\u00EDa con bloqueo", 1, "badge", "warn"], [1, "cal-more"]], template: function CalendarioComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "button", 1);
            i0.ɵɵlistener("click", function CalendarioComponent_Template_button_click_1_listener() { return ctx.navegar(-1); });
            i0.ɵɵtext(2);
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(3, "h3");
            i0.ɵɵtext(4);
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(5, "button", 1);
            i0.ɵɵlistener("click", function CalendarioComponent_Template_button_click_5_listener() { return ctx.navegar(1); });
            i0.ɵɵtext(6);
            i0.ɵɵelementEnd()();
            i0.ɵɵconditionalCreate(7, CalendarioComponent_Conditional_7_Template, 2, 1, "div", 2);
            i0.ɵɵconditionalCreate(8, CalendarioComponent_Conditional_8_Template, 3, 0, "div", 3);
            i0.ɵɵconditionalCreate(9, CalendarioComponent_Conditional_9_Template, 6, 1);
        } if (rf & 2) {
            i0.ɵɵadvance(2);
            i0.ɵɵtextInterpolate1("\u2190 ", ctx.modo === "semana" ? "Semana anterior" : "Mes anterior");
            i0.ɵɵadvance(2);
            i0.ɵɵtextInterpolate(ctx.titulo);
            i0.ɵɵadvance(2);
            i0.ɵɵtextInterpolate1("", ctx.modo === "semana" ? "Siguiente semana" : "Siguiente mes", " \u2192");
            i0.ɵɵadvance();
            i0.ɵɵconditional(ctx.error ? 7 : -1);
            i0.ɵɵadvance();
            i0.ɵɵconditional(ctx.modo === "semana" ? 8 : -1);
            i0.ɵɵadvance();
            i0.ɵɵconditional(ctx.modo === "mes" ? 9 : -1);
        } }, dependencies: [CommonModule, i2.NgClass], styles: [".cal-head[_ngcontent-%COMP%] { margin-bottom: 8px; }\n      .cal-grid[_ngcontent-%COMP%] { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }\n      .cal-grid.mes[_ngcontent-%COMP%] { grid-template-rows: repeat(auto-fill, minmax(96px, auto)); }\n      .cal-cell[_ngcontent-%COMP%] {\n        border: 1px solid var(--%NS%border); border-radius: 8px; padding: 6px; min-height: 110px;\n        display: flex; flex-direction: column; gap: 4px; background: var(--%NS%card);\n      }\n      .cal-grid.mes[_ngcontent-%COMP%]   .cal-cell[_ngcontent-%COMP%] { min-height: 96px; cursor: pointer; }\n      .cal-grid.mes[_ngcontent-%COMP%]   .cal-cell[_ngcontent-%COMP%]:hover { border-color: var(--%NS%primary); }\n      .cal-cell.hoy[_ngcontent-%COMP%] { border-color: var(--%NS%primary); box-shadow: 0 0 0 1px var(--%NS%primary); }\n      .cal-cell.otro[_ngcontent-%COMP%] { opacity: 0.45; }\n      .cal-cell.festivo[_ngcontent-%COMP%] { background: repeating-linear-gradient(45deg, transparent, transparent 6px, rgba(120,120,120,0.06) 6px, rgba(120,120,120,0.06) 12px); }\n      .cal-cell.bloqueado[_ngcontent-%COMP%] { border-color: var(--%NS%warning); }\n      .cal-weekdays[_ngcontent-%COMP%] { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; margin-bottom: 6px; }\n      .cal-wd[_ngcontent-%COMP%] { font-size: 12px; font-weight: 600; color: var(--%NS%muted); text-align: center; }\n      .cal-day[_ngcontent-%COMP%] { display: flex; justify-content: space-between; align-items: center; gap: 4px; }\n      .cal-day[_ngcontent-%COMP%]   strong[_ngcontent-%COMP%] { font-size: 12px; color: var(--%NS%muted); }\n      .cal-items[_ngcontent-%COMP%] { display: flex; flex-direction: column; gap: 3px; overflow: hidden; }\n      .cal-item[_ngcontent-%COMP%] { display: flex; align-items: center; gap: 5px; font-size: 12px; line-height: 1.2; }\n      .cal-hora[_ngcontent-%COMP%] { color: var(--%NS%primary); font-weight: 600; white-space: nowrap; }\n      .cal-txt[_ngcontent-%COMP%] { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n      .cal-more[_ngcontent-%COMP%] { font-size: 11px; color: var(--%NS%muted); }"] });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(CalendarioComponent, [{
        type: Component,
        args: [{ selector: 'app-calendario', template: `
    <div class="cal-head between">
      <button class="btn small" (click)="navegar(-1)">← {{ modo === 'semana' ? 'Semana anterior' : 'Mes anterior' }}</button>
      <h3>{{ titulo }}</h3>
      <button class="btn small" (click)="navegar(1)">{{ modo === 'semana' ? 'Siguiente semana' : 'Siguiente mes' }} →</button>
    </div>
    @if (error) {
      <div class="msg error">{{ error }}</div>
    }

    @if (modo === 'semana') {
      <div class="cal-grid">
        @for (d of dias; track d.iso) {
          <div class="cal-cell" [ngClass]="{ hoy: d.iso === hoy, festivo: d.inactivo, bloqueado: d.bloqueado }">
            <div class="cal-day" (click)="irDia.emit(d.iso)">
              <strong>{{ d.iso }}</strong>
              @if (d.inactivo) {
                <span class="badge dim">sin horario</span>
              } @else if (d.bloqueado) {
                <span class="badge warn">bloqueo</span>
              }
            </div>
            <div class="cal-items">
              @for (c of d.citas; track c.id) {
                <div class="cal-item">
                  <span class="cal-hora">{{ c.horaInicio }}</span>
                  <span class="cal-txt">{{ c.pacienteNombre }}</span>
                  <span class="badge" [ngClass]="badge(c.estado)">{{ c.estadoNombre }}</span>
                </div>
              }
              @if (!d.citas.length && (d.inactivo || d.bloqueado)) {
                <div class="empty">—</div>
              }
            </div>
          </div>
        }
      </div>
    }

    @if (modo === 'mes') {
      <div class="cal-weekdays">
        @for (d of ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']; track d) {
          <div class="cal-wd">{{ d }}</div>
        }
      </div>
      <div class="cal-grid mes">
        @for (d of dias; track d.iso) {
          <div
            class="cal-cell"
            [ngClass]="{ hoy: d.iso === hoy, otro: d.enOtroMes, festivo: d.inactivo, bloqueado: d.bloqueado }"
            (click)="irDia.emit(d.iso)"
          >
            <div class="cal-day">
              <strong>{{ d.numero }}</strong>
              @if (d.bloqueado) {
                <span class="badge warn" title="Día con bloqueo">B</span>
              }
            </div>
            <div class="cal-items">
              @for (c of d.citas.slice(0, 3); track c.id) {
                <div class="cal-item">
                  <span class="cal-hora">{{ c.horaInicio }}</span>
                  <span class="cal-txt">{{ c.pacienteNombre }}</span>
                </div>
              }
              @if (d.citas.length > 3) {
                <div class="cal-more">+{{ d.citas.length - 3 }} más</div>
              }
            </div>
          </div>
        }
      </div>
    }
  `, imports: [CommonModule], standalone: true, styles: ["\n      .cal-head { margin-bottom: 8px; }\n      .cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; }\n      .cal-grid.mes { grid-template-rows: repeat(auto-fill, minmax(96px, auto)); }\n      .cal-cell {\n        border: 1px solid var(--border); border-radius: 8px; padding: 6px; min-height: 110px;\n        display: flex; flex-direction: column; gap: 4px; background: var(--card);\n      }\n      .cal-grid.mes .cal-cell { min-height: 96px; cursor: pointer; }\n      .cal-grid.mes .cal-cell:hover { border-color: var(--primary); }\n      .cal-cell.hoy { border-color: var(--primary); box-shadow: 0 0 0 1px var(--primary); }\n      .cal-cell.otro { opacity: 0.45; }\n      .cal-cell.festivo { background: repeating-linear-gradient(45deg, transparent, transparent 6px, rgba(120,120,120,0.06) 6px, rgba(120,120,120,0.06) 12px); }\n      .cal-cell.bloqueado { border-color: var(--warning); }\n      .cal-weekdays { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; margin-bottom: 6px; }\n      .cal-wd { font-size: 12px; font-weight: 600; color: var(--muted); text-align: center; }\n      .cal-day { display: flex; justify-content: space-between; align-items: center; gap: 4px; }\n      .cal-day strong { font-size: 12px; color: var(--muted); }\n      .cal-items { display: flex; flex-direction: column; gap: 3px; overflow: hidden; }\n      .cal-item { display: flex; align-items: center; gap: 5px; font-size: 12px; line-height: 1.2; }\n      .cal-hora { color: var(--primary); font-weight: 600; white-space: nowrap; }\n      .cal-txt { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }\n      .cal-more { font-size: 11px; color: var(--muted); }\n    "] }]
    }], () => [{ type: i1.Api }], { modo: [{
            type: Input
        }], fecha: [{
            type: Input
        }], odontologoId: [{
            type: Input
        }], irDia: [{
            type: Output
        }] }); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(CalendarioComponent, { className: "CalendarioComponent", filePath: "src/app/agenda/calendario.ts", lineNumber: 122 }); })();
