import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import * as i0 from "@angular/core";
import * as i1 from "../core/auth.service";
import * as i2 from "@angular/forms";
function LoginComponent_Conditional_6_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelementStart(0, "div", 3);
    i0.ɵɵtext(1);
    i0.ɵɵelementEnd();
} if (rf & 2) {
    const ctx_r0 = i0.ɵɵnextContext();
    i0.ɵɵadvance();
    i0.ɵɵtextInterpolate(ctx_r0.error);
} }
function LoginComponent_Conditional_16_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵelement(0, "span", 10);
} }
function LoginComponent_Conditional_17_Template(rf, ctx) { if (rf & 1) {
    i0.ɵɵtext(0, " Entrar ");
} }
export class LoginComponent {
    auth;
    username = '';
    password = '';
    cargando = false;
    error = '';
    constructor(auth) {
        this.auth = auth;
    }
    entrar() {
        this.error = '';
        this.cargando = true;
        this.auth
            .login(this.username, this.password)
            .catch((err) => {
            this.error =
                err?.error?.message ?? err?.error?.mensaje ?? (err?.status === 401 ? 'Credenciales inválidas' : 'Error al iniciar sesión');
        })
            .finally(() => {
            this.cargando = false;
        });
    }
    static ɵfac = function LoginComponent_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || LoginComponent)(i0.ɵɵdirectiveInject(i1.AuthService)); };
    static ɵcmp = /*@__PURE__*/ i0.ɵɵdefineComponent({ type: LoginComponent, selectors: [["app-login"]], decls: 18, vars: 5, consts: [[1, "login-page"], [1, "login-card"], [1, "sub"], [1, "msg", "error"], [3, "ngSubmit"], [1, "label"], ["type", "text", "name", "username", "autocomplete", "username", "required", "", 1, "input", 3, "ngModelChange", "ngModel"], ["type", "password", "name", "password", "autocomplete", "current-password", "required", "", 1, "input", 3, "ngModelChange", "ngModel"], [2, "margin-top", "18px"], [1, "btn", "primary", 2, "width", "100%", 3, "disabled"], [1, "spin"]], template: function LoginComponent_Template(rf, ctx) { if (rf & 1) {
            i0.ɵɵelementStart(0, "div", 0)(1, "div", 1)(2, "h1");
            i0.ɵɵtext(3, "CRM Dental");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(4, "div", 2);
            i0.ɵɵtext(5, "Inicia sesi\u00F3n con tus credenciales");
            i0.ɵɵelementEnd();
            i0.ɵɵconditionalCreate(6, LoginComponent_Conditional_6_Template, 2, 1, "div", 3);
            i0.ɵɵelementStart(7, "form", 4);
            i0.ɵɵlistener("ngSubmit", function LoginComponent_Template_form_ngSubmit_7_listener() { return ctx.entrar(); });
            i0.ɵɵelementStart(8, "label", 5);
            i0.ɵɵtext(9, "Usuario");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(10, "input", 6);
            i0.ɵɵtwoWayListener("ngModelChange", function LoginComponent_Template_input_ngModelChange_10_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.username, $event) || (ctx.username = $event); return $event; });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(11, "label", 5);
            i0.ɵɵtext(12, "Contrase\u00F1a");
            i0.ɵɵelementEnd();
            i0.ɵɵelementStart(13, "input", 7);
            i0.ɵɵtwoWayListener("ngModelChange", function LoginComponent_Template_input_ngModelChange_13_listener($event) { i0.ɵɵtwoWayBindingSet(ctx.password, $event) || (ctx.password = $event); return $event; });
            i0.ɵɵelementEnd();
            i0.ɵɵcontrolCreate();
            i0.ɵɵelementStart(14, "div", 8)(15, "button", 9);
            i0.ɵɵconditionalCreate(16, LoginComponent_Conditional_16_Template, 1, 0, "span", 10)(17, LoginComponent_Conditional_17_Template, 1, 0);
            i0.ɵɵelementEnd()()()()();
        } if (rf & 2) {
            i0.ɵɵadvance(6);
            i0.ɵɵconditional(ctx.error ? 6 : -1);
            i0.ɵɵadvance(4);
            i0.ɵɵtwoWayProperty("ngModel", ctx.username);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(3);
            i0.ɵɵtwoWayProperty("ngModel", ctx.password);
            i0.ɵɵcontrol();
            i0.ɵɵadvance(2);
            i0.ɵɵproperty("disabled", ctx.cargando);
            i0.ɵɵadvance();
            i0.ɵɵconditional(ctx.cargando ? 16 : 17);
        } }, dependencies: [FormsModule, i2.ɵNgNoValidate, i2.DefaultValueAccessor, i2.NgControlStatus, i2.NgControlStatusGroup, i2.RequiredValidator, i2.NgModel, i2.NgForm], encapsulation: 2 });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(LoginComponent, [{
        type: Component,
        args: [{
                selector: 'app-login',
                template: `
    <div class="login-page">
      <div class="login-card">
        <h1>CRM Dental</h1>
        <div class="sub">Inicia sesión con tus credenciales</div>
        @if (error) {
          <div class="msg error">{{ error }}</div>
        }
        <form (ngSubmit)="entrar()">
          <label class="label">Usuario</label>
          <input class="input" type="text" [(ngModel)]="username" name="username" autocomplete="username" required />
          <label class="label">Contraseña</label>
          <input class="input" type="password" [(ngModel)]="password" name="password" autocomplete="current-password" required />
          <div style="margin-top:18px">
            <button class="btn primary" style="width:100%" [disabled]="cargando">
              @if (cargando) {
                <span class="spin"></span>
              } @else {
                Entrar
              }
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
                imports: [FormsModule],
            }]
    }], () => [{ type: i1.AuthService }], null); })();
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassDebugInfo(LoginComponent, { className: "LoginComponent", filePath: "src/app/auth/login.ts", lineNumber: 35 }); })();
