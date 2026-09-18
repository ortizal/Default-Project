import { Injectable } from '@angular/core';
import * as i0 from "@angular/core";
import * as i1 from "./api";
import * as i2 from "@angular/router";
const KEY = 'dentalcrm.auth';
export class AuthService {
    api;
    router;
    state = null;
    constructor(api, router) {
        this.api = api;
        this.router = router;
        try {
            const raw = localStorage.getItem(KEY);
            if (raw)
                this.state = JSON.parse(raw);
        }
        catch {
            this.state = null;
        }
    }
    get token() {
        return this.state?.token ?? null;
    }
    get username() {
        return this.state?.username ?? '';
    }
    get roles() {
        return this.state?.roles ?? [];
    }
    get logged() {
        return !!this.token;
    }
    hasRole(code) {
        return this.roles.some((r) => r.toUpperCase() === code.toUpperCase());
    }
    login(username, password) {
        return new Promise((resolve, reject) => {
            this.api.post('/auth/login', { username, password }).subscribe({
                next: (l) => {
                    this.state = l;
                    localStorage.setItem(KEY, JSON.stringify(l));
                    this.router.navigate(['/dashboard']);
                    resolve();
                },
                error: (err) => reject(err),
            });
        });
    }
    logout() {
        this.state = null;
        localStorage.removeItem(KEY);
        this.router.navigate(['/login']);
    }
    static ɵfac = function AuthService_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || AuthService)(i0.ɵɵinject(i1.Api), i0.ɵɵinject(i2.Router)); };
    static ɵprov = /*@__PURE__*/ i0.ɵɵdefineInjectable({ token: AuthService, factory: AuthService.ɵfac, providedIn: 'root' });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(AuthService, [{
        type: Injectable,
        args: [{ providedIn: 'root' }]
    }], () => [{ type: i1.Api }, { type: i2.Router }], null); })();
