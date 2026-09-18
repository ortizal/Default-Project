import { Injectable } from '@angular/core';
import * as i0 from "@angular/core";
import * as i1 from "@angular/common/http";
const DEFAULT = { apiUrl: '/api/v1' };
export class Api {
    http;
    cfg = { ...DEFAULT };
    constructor(http) {
        this.http = http;
    }
    async init() {
        try {
            const r = await fetch('/assets/config.json', { cache: 'no-cache' });
            if (r.ok) {
                const json = (await r.json());
                if (json.apiUrl)
                    this.cfg = { ...DEFAULT, ...json };
            }
        }
        catch {
            this.cfg = { ...DEFAULT };
        }
    }
    get apiUrl() {
        return this.cfg.apiUrl;
    }
    url(path) {
        return this.cfg.apiUrl + path;
    }
    get(path, params) {
        return this.http.get(this.url(path), { params });
    }
    post(path, body) {
        return this.http.post(this.url(path), body);
    }
    put(path, body) {
        return this.http.put(this.url(path), body);
    }
    patch(path, body) {
        return this.http.patch(this.url(path), body);
    }
    del(path) {
        return this.http.delete(this.url(path));
    }
    static ɵfac = function Api_Factory(__ngFactoryType__) { /* @ts-ignore */
    return new (__ngFactoryType__ || Api)(i0.ɵɵinject(i1.HttpClient)); };
    static ɵprov = /*@__PURE__*/ i0.ɵɵdefineInjectable({ token: Api, factory: Api.ɵfac, providedIn: 'root' });
}
(() => { (typeof ngDevMode === "undefined" || ngDevMode) && i0.ɵsetClassMetadata(Api, [{
        type: Injectable,
        args: [{ providedIn: 'root' }]
    }], () => [{ type: i1.HttpClient }], null); })();
