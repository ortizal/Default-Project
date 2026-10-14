import{$ as Mh,Bn as nd,Cn as ih,Dn as jL,Dr as zL,En as jI,Er as zI,Et as Te$1,Fn as lr,H as Iu,Hn as nw,I as Hr,In as me$1,It as VI,Jn as pm,Ln as mh,Mn as kv,Mt as Ui,N as HI,Ot as UI,Qn as rI,Qt as ch,Rt as Vs,T as Eu,Tr as z,V as Im,Wn as oe,Wt as _I,Yn as pw,_ as Dm,_r as wI,ar as th,at as O,bn as hn,f as C,gn as g$1,gr as wD,ir as td,jn as ke$1,kt as UL,ln as es,lt as PL,mn as fi,mr as vu,n as $r,nr as sh,nt as Ne$1,ot as OI,p as CI,qn as ph,qt as ah,v as Dr,xn as hu,xt as SI,y as Du,yr as wh,zt as Vw}from"./chunk--JbN2_6S.js";import"./chunk-BRQbujOQ.js";import{a as ot,c as Io,d as le$1,f as lr$1,i as Wt,l as Nt,o as rt,p as q$1,r as Ot$1,s as u}from"./main-4F4JFESK.js";import{n as l,t as R}from"./chunk-SULupQ6-.js";var ut=[{section:`Operación`,items:[{path:`/dashboard`,label:`Inicio`},{path:`/pacientes`,label:`Pacientes`,permiso:`PACIENTES_READ`},{path:`/odontologos`,label:`Odontólogos`,permiso:`ODONTOLOGOS_READ`},{path:`/servicios`,label:`Servicios`,permiso:`SERVICIOS_READ`},{path:`/horarios`,label:`Horarios`,permiso:`AGENDA_READ`},{path:`/agenda`,label:`Agenda`,permiso:`AGENDA_READ`},{path:`/citas`,label:`Citas`,permiso:`CITAS_READ`}]},{section:`Comunicación`,items:[{path:`/whatsapp/inbox`,label:`WhatsApp Inbox`,permiso:`WHATSAPP_READ`},{path:`/whatsapp/campana`,label:`Publicidad WhatsApp`,permiso:`WHATSAPP_WRITE`},{path:`/redes-sociales`,label:`Redes sociales`,permiso:`REDES_SOCIALES`},{path:`/whatsapp/sesiones`,label:`Sesiones WhatsApp`,permiso:`WHATSAPP_WRITE`},{path:`/plantillas`,label:`Plantillas`,permiso:`AUTOMATIZACIONES`},{path:`/notificaciones`,label:`Notificaciones`,permiso:`AUTOMATIZACIONES`}]},{section:`Automatización`,items:[{path:`/automatizaciones`,label:`Automatizaciones`,permiso:`AUTOMATIZACIONES`},{path:`/agente`,label:`Agente IA`,permiso:`AGENTE_IA`}]},{section:`Calendario`,items:[{path:`/google`,label:`Google Calendar`,permiso:`INTEGRACIONES`}]},{section:`Gestión`,items:[{path:`/reportes`,label:`Reportes`,permiso:`REPORTES`},{path:`/auditoria`,label:`Auditoría`,permiso:`AUDITORIA_READ`},{path:`/usuarios`,label:`Usuarios`,permiso:`USUARIOS_READ`},{path:`/configuracion/consultorio`,label:`Datos del consultorio`,permiso:`CONFIGURACION`},{path:`/configuracion/integraciones`,label:`Configuración de integraciones`,permiso:`CONFIGURACION`}]}];var Re=(a,n)=>n.section;var Fe=(a,n)=>n.path;function Oe(a,n){if(a&1){let t=OI();Ui(0,`a`,9),ph(`click`,function(){td(t);return nd(jI(2).navegar.emit())}),Ui(1,`mat-icon`),pw(2),vu(),Ui(3,`span`,10),pw(4),vu()()}if(a&2){let t=n.$implicit,e=jI(2);sh(`routerLink`,t.path)(`title`,e.colapsado()?t.label:null),wD(2),Mh(e.ICONOS[t.path]??`chevron_right`),wD(2),Mh(t.label)}}function Le(a,n){if(a&1&&(Ui(0,`div`,7),pw(1),vu(),_I(2,Oe,5,4,`a`,8,Fe)),a&2){let t=n.$implicit;wD(),Mh(t.section),wD(),SI(t.items)}}var Pe={"/dashboard":`home`,"/pacientes":`people`,"/odontologos":`medical_services`,"/servicios":`build`,"/horarios":`schedule`,"/agenda":`calendar_month`,"/citas":`event_available`,"/whatsapp/inbox":`chat`,"/whatsapp/campana":`campaign`,"/whatsapp/sesiones":`smart_toy`,"/redes-sociales":`share`,"/plantillas":`description`,"/notificaciones":`notifications`,"/automatizaciones":`bolt`,"/agente":`support_agent`,"/google":`event`,"/reportes":`insert_chart`,"/auditoria":`verified_user`,"/usuarios":`account_circle`,"/configuracion/consultorio":`location_on`,"/configuracion/integraciones":`settings`};var bt=class a{colapsado=jL(!1);abierto=jL(!1);salir=PL();navegar=PL();ICONOS=Pe;auth=g$1(u);get secciones(){return ut.map(n=>({section:n.section,items:n.items.filter(t=>this.auth.puede(t.permiso))})).filter(n=>n.items.length>0)}get usuario(){return this.auth.username}static ɵfac=function(t){return new(t||a)};static ɵcmp=rI({type:a,selectors:[[`app-sidebar`]],inputs:{colapsado:[1,`colapsado`],abierto:[1,`abierto`]},outputs:{salir:`salir`,navegar:`navegar`},decls:17,vars:5,consts:[[1,`sidebar`],[1,`brand`],[1,`brand-mark`],[1,`brand-name`],[1,`sidebar-nav`],[1,`sidebar-footer`],[1,`btn`,`sm`,`block`,`mt-2`,3,`click`],[1,`section`],[`routerLinkActive`,`active`,3,`routerLink`,`title`],[`routerLinkActive`,`active`,3,`click`,`routerLink`,`title`],[1,`txt`]],template:function(t,e){t&1&&(Ui(0,`aside`,0)(1,`div`,1)(2,`span`,2)(3,`mat-icon`),pw(4,`medical_services`),vu()(),Ui(5,`span`,3),pw(6,`Little Smile CRM`),vu()(),Ui(7,`nav`,4),_I(8,Le,4,1,null,null,Re),vu(),Ui(10,`div`,5)(11,`div`),pw(12,`Usuario: `),Ui(13,`strong`),pw(14),vu()(),Ui(15,`button`,6),ph(`click`,function(){return e.salir.emit()}),pw(16,` Cerrar sesión `),vu()()()),t&2&&(wh(`collapsed`,e.colapsado())(`open`,e.abierto()),wD(8),SI(e.secciones),wD(6),Mh(e.usuario))},dependencies:[Nt,Io,Wt,Ot$1],encapsulation:2})};var At;function me(){if(At==null){let a=typeof document<`u`?document.head:null;At=!!(a&&(a.createShadowRoot||a.attachShadow))}return At}function Rt(a){if(me()){let n=a.getRootNode?a.getRootNode():null;if(typeof ShadowRoot<`u`&&ShadowRoot&&n instanceof ShadowRoot)return n}return null}function g(a){if(a.composedPath)try{return a.composedPath()[0]}catch(n){}return a.target}var q;function le(){if(q==null&&typeof window<`u`)try{window.addEventListener(`test`,null,Object.defineProperty({},"passive",{get:()=>q=!0}))}finally{q=q||!1}return q}function j(a){return le()?a:!!a.capture}var ze=[`*`,[[`mat-toolbar-row`]]];var Be=[`*`,`mat-toolbar-row`];var je=(()=>{class a{static ɵfac=function(e){return new(e||a)};static ɵdir=hu({type:a,selectors:[[`mat-toolbar-row`]],hostAttrs:[1,`mat-toolbar-row`],exportAs:[`matToolbarRow`]})}return a})();var pt=(()=>{class a{_elementRef=g$1(hn);_platform=g$1(l);_document=g$1(Te$1);color;_toolbarRows;ngAfterViewInit(){this._platform.isBrowser&&(this._checkToolbarMixedModes(),this._toolbarRows.changes.subscribe(()=>this._checkToolbarMixedModes()))}_checkToolbarMixedModes(){this._toolbarRows.length}static ɵfac=function(e){return new(e||a)};static ɵcmp=rI({type:a,selectors:[[`mat-toolbar`]],contentQueries:function(e,o,i){if(e&1&&mh(i,je,5),e&2){let l;UI(l=zI())&&(o._toolbarRows=l)}},hostAttrs:[1,`mat-toolbar`],hostVars:6,hostBindings:function(e,o){e&2&&(nw(o.color?`mat-`+o.color:``),wh(`mat-toolbar-multiple-rows`,o._toolbarRows.length>0)(`mat-toolbar-single-row`,o._toolbarRows.length===0))},inputs:{color:`color`},exportAs:[`matToolbar`],ngContentSelectors:Be,decls:2,vars:0,template:function(e,o){e&1&&(VI(ze),HI(0),HI(1,1))},styles:[`.mat-toolbar {
  background: var(--%NS%mat-toolbar-container-background-color, var(--%NS%mat-sys-surface));
  color: var(--%NS%mat-toolbar-container-text-color, var(--%NS%mat-sys-on-surface));
}
.mat-toolbar, .mat-toolbar h1, .mat-toolbar h2, .mat-toolbar h3, .mat-toolbar h4, .mat-toolbar h5, .mat-toolbar h6 {
  font-family: var(--%NS%mat-toolbar-title-text-font, var(--%NS%mat-sys-title-large-font));
  font-size: var(--%NS%mat-toolbar-title-text-size, var(--%NS%mat-sys-title-large-size));
  line-height: var(--%NS%mat-toolbar-title-text-line-height, var(--%NS%mat-sys-title-large-line-height));
  font-weight: var(--%NS%mat-toolbar-title-text-weight, var(--%NS%mat-sys-title-large-weight));
  letter-spacing: var(--%NS%mat-toolbar-title-text-tracking, var(--%NS%mat-sys-title-large-tracking));
  margin: 0;
}
@media (forced-colors: active) {
  .mat-toolbar {
    outline: solid 1px;
  }
}
.mat-toolbar .mat-form-field-underline,
.mat-toolbar .mat-form-field-ripple,
.mat-toolbar .mat-focused .mat-form-field-ripple {
  background-color: currentColor;
}
.mat-toolbar .mat-form-field-label,
.mat-toolbar .mat-focused .mat-form-field-label,
.mat-toolbar .mat-select-value,
.mat-toolbar .mat-select-arrow,
.mat-toolbar .mat-form-field.mat-focused .mat-select-arrow {
  color: inherit;
}
.mat-toolbar .mat-input-element {
  caret-color: currentColor;
}
.mat-toolbar .mat-mdc-button-base.mat-mdc-button-base.mat-unthemed {
  --%NS%mat-button-text-label-text-color: var(--%NS%mat-toolbar-container-text-color, var(--%NS%mat-sys-on-surface));
  --%NS%mat-button-outlined-label-text-color: var(--%NS%mat-toolbar-container-text-color, var(--%NS%mat-sys-on-surface));
}

.mat-toolbar-row, .mat-toolbar-single-row {
  display: flex;
  box-sizing: border-box;
  padding: 0 16px;
  width: 100%;
  flex-direction: row;
  align-items: center;
  white-space: nowrap;
  height: var(--%NS%mat-toolbar-standard-height, 64px);
}
@media (max-width: 599px) {
  .mat-toolbar-row, .mat-toolbar-single-row {
    height: var(--%NS%mat-toolbar-mobile-height, 56px);
  }
}

.mat-toolbar-multiple-rows {
  display: flex;
  box-sizing: border-box;
  flex-direction: column;
  width: 100%;
  min-height: var(--%NS%mat-toolbar-standard-height, 64px);
}
@media (max-width: 599px) {
  .mat-toolbar-multiple-rows {
    min-height: var(--%NS%mat-toolbar-mobile-height, 56px);
  }
}
`],encapsulation:2})}return a})();var ft=(()=>{class a{static ɵfac=function(e){return new(e||a)};static ɵmod=es({type:a});static ɵinj=Dr({imports:[ot]})}return a})();function W(a){return a.buttons===0||a.detail===0}function X(a){let n=a.touches&&a.touches[0]||a.changedTouches&&a.changedTouches[0];return!!n&&n.identifier===-1&&(n.radiusX==null||n.radiusX===1)&&(n.radiusY==null||n.radiusY===1)}function M(a){return a instanceof hn?a.nativeElement:a}var ue=new C(`cdk-input-modality-detector-options`);var be={ignoreKeys:[18,17,224,91,16]};var pe=650;var Ft={passive:!0,capture:!0};var fe=(()=>{class a{_platform=g$1(l);_listenerCleanups;modalityDetected;modalityChanged;get mostRecentModality(){return this._modality.value}_mostRecentTarget=null;_modality=new lr(null);_options;_lastTouchMs=0;_onKeydown=t=>{this._options?.ignoreKeys?.some(e=>e===t.keyCode)||(this._modality.next(`keyboard`),this._mostRecentTarget=g(t))};_onMousedown=t=>{Date.now()-this._lastTouchMs<pe||(this._modality.next(W(t)?`keyboard`:`mouse`),this._mostRecentTarget=g(t))};_onTouchstart=t=>{if(X(t)){this._modality.next(`keyboard`);return}this._lastTouchMs=Date.now(),this._modality.next(`touch`),this._mostRecentTarget=g(t)};constructor(){let t=g$1(z),e=g$1(Te$1),o=g$1(ue,{optional:!0});if(this._options=O(O({},be),o),this.modalityDetected=this._modality.pipe(Dm(1)),this.modalityChanged=this.modalityDetected.pipe(pm()),this._platform.isBrowser){let i=g$1(Hr).createRenderer(null,null);this._listenerCleanups=t.runOutsideAngular(()=>[i.listen(e,`keydown`,this._onKeydown,Ft),i.listen(e,`mousedown`,this._onMousedown,Ft),i.listen(e,`touchstart`,this._onTouchstart,Ft)])}}ngOnDestroy(){this._modality.complete(),this._listenerCleanups?.forEach(t=>t())}static ɵfac=function(e){return new(e||a)};static ɵprov=ke$1({token:a,factory:a.ɵfac})}return a})();var Q=(function(a){return a[a.IMMEDIATE=0]=`IMMEDIATE`,a[a.EVENTUAL=1]=`EVENTUAL`,a})(Q||{});var he=new C(`cdk-focus-monitor-default-options`);var ht=j({passive:!0,capture:!0});var Ot=(()=>{class a{_ngZone=g$1(z);_platform=g$1(l);_inputModalityDetector=g$1(fe);_origin=null;_lastFocusOrigin=null;_windowFocused=!1;_windowFocusTimeoutId;_originTimeoutId;_originFromTouchInteraction=!1;_elementInfo=new Map;_monitoredElementCount=0;_rootNodeFocusListenerCount=new Map;_detectionMode;_windowFocusListener=()=>{this._windowFocused=!0,this._windowFocusTimeoutId=setTimeout(()=>this._windowFocused=!1)};_document=g$1(Te$1);_stopInputModalityDetector=new oe;constructor(){let t=g$1(he,{optional:!0});this._detectionMode=t?.detectionMode||Q.IMMEDIATE}_rootNodeFocusAndBlurListener=t=>{let e=g(t);for(let o=e;o;o=o.parentElement)t.type===`focus`?this._onFocus(t,o):this._onBlur(t,o)};monitor(t,e=!1){let o=M(t);if(!this._platform.isBrowser||o.nodeType!==1)return Vs();let i=Rt(o)||this._document,l=this._elementInfo.get(o);if(l)return e&&(l.checkChildren=!0),l.subject;let h={checkChildren:e,subject:new oe,rootNode:i};return this._elementInfo.set(o,h),this._registerGlobalListeners(h),h.subject}stopMonitoring(t){let e=M(t),o=this._elementInfo.get(e);o&&(o.subject.complete(),this._setClasses(e),this._elementInfo.delete(e),this._removeGlobalListeners(o))}focusVia(t,e,o){let i=M(t);i===this._document.activeElement?this._getClosestElementsInfo(i).forEach(([h,R])=>this._originChanged(h,e,R)):(this._setOrigin(e),typeof i.focus==`function`&&i.focus(o))}ngOnDestroy(){this._elementInfo.forEach((t,e)=>this.stopMonitoring(e))}_getWindow(){return this._document.defaultView||window}_getFocusOrigin(t){return this._origin?this._originFromTouchInteraction?this._shouldBeAttributedToTouch(t)?`touch`:`program`:this._origin:this._windowFocused&&this._lastFocusOrigin?this._lastFocusOrigin:t&&this._isLastInteractionFromInputLabel(t)?`mouse`:`program`}_shouldBeAttributedToTouch(t){return this._detectionMode===Q.EVENTUAL||!!t?.contains(this._inputModalityDetector._mostRecentTarget)}_setClasses(t,e){t.classList.toggle(`cdk-focused`,!!e),t.classList.toggle(`cdk-touch-focused`,e===`touch`),t.classList.toggle(`cdk-keyboard-focused`,e===`keyboard`),t.classList.toggle(`cdk-mouse-focused`,e===`mouse`),t.classList.toggle(`cdk-program-focused`,e===`program`)}_setOrigin(t,e=!1){this._ngZone.runOutsideAngular(()=>{if(this._origin=t,this._originFromTouchInteraction=t===`touch`&&e,this._detectionMode===Q.IMMEDIATE){clearTimeout(this._originTimeoutId);let o=this._originFromTouchInteraction?pe:1;this._originTimeoutId=setTimeout(()=>this._origin=null,o)}})}_onFocus(t,e){let o=this._elementInfo.get(e),i=g(t);!o||!o.checkChildren&&e!==i||this._originChanged(e,this._getFocusOrigin(i),o)}_onBlur(t,e){let o=this._elementInfo.get(e);!o||o.checkChildren&&t.relatedTarget instanceof Node&&e.contains(t.relatedTarget)||(this._setClasses(e),this._emitOrigin(o,null))}_emitOrigin(t,e){t.subject.observers.length&&this._ngZone.run(()=>t.subject.next(e))}_registerGlobalListeners(t){if(!this._platform.isBrowser)return;let e=t.rootNode,o=this._rootNodeFocusListenerCount.get(e)||0;o||this._ngZone.runOutsideAngular(()=>{e.addEventListener(`focus`,this._rootNodeFocusAndBlurListener,ht),e.addEventListener(`blur`,this._rootNodeFocusAndBlurListener,ht)}),this._rootNodeFocusListenerCount.set(e,o+1),++this._monitoredElementCount===1&&(this._ngZone.runOutsideAngular(()=>{this._getWindow().addEventListener(`focus`,this._windowFocusListener)}),this._inputModalityDetector.modalityDetected.pipe(Im(this._stopInputModalityDetector)).subscribe(i=>{this._setOrigin(i,!0)}))}_removeGlobalListeners(t){let e=t.rootNode;if(this._rootNodeFocusListenerCount.has(e)){let o=this._rootNodeFocusListenerCount.get(e);o>1?this._rootNodeFocusListenerCount.set(e,o-1):(e.removeEventListener(`focus`,this._rootNodeFocusAndBlurListener,ht),e.removeEventListener(`blur`,this._rootNodeFocusAndBlurListener,ht),this._rootNodeFocusListenerCount.delete(e))}--this._monitoredElementCount||(this._getWindow().removeEventListener(`focus`,this._windowFocusListener),this._stopInputModalityDetector.next(),clearTimeout(this._windowFocusTimeoutId),clearTimeout(this._originTimeoutId))}_originChanged(t,e,o){this._setClasses(t,e),this._emitOrigin(o,e),this._lastFocusOrigin=e}_getClosestElementsInfo(t){let e=[];return this._elementInfo.forEach((o,i)=>{(i===t||o.checkChildren&&i.contains(t))&&e.push([i,o])}),e}_isLastInteractionFromInputLabel(t){let{_mostRecentTarget:e,mostRecentModality:o}=this._inputModalityDetector;if(o!==`mouse`||!e||e===t||t.nodeName!==`INPUT`&&t.nodeName!==`TEXTAREA`||t.disabled)return!1;let i=t.labels;if(i){for(let l=0;l<i.length;l++)if(i[l].contains(e))return!0}return!1}static ɵfac=function(e){return new(e||a)};static ɵprov=ke$1({token:a,factory:a.ɵfac})}return a})();var f=(function(a){return a[a.FADING_IN=0]=`FADING_IN`,a[a.VISIBLE=1]=`VISIBLE`,a[a.FADING_OUT=2]=`FADING_OUT`,a[a.HIDDEN=3]=`HIDDEN`,a})(f||{});var Lt=class{_renderer;element;config;_animationForciblyDisabledThroughCss;state=f.HIDDEN;constructor(n,t,e,o=!1){this._renderer=n,this.element=t,this.config=e,this._animationForciblyDisabledThroughCss=o}fadeOut(){this._renderer.fadeOutRipple(this)}};var ve=j({passive:!0,capture:!0});var Pt=class{_events=new Map;addHandler(n,t,e,o){let i=this._events.get(t);if(i){let l=i.get(e);l?l.add(o):i.set(e,new Set([o]))}else this._events.set(t,new Map([[e,new Set([o])]])),n.runOutsideAngular(()=>{document.addEventListener(t,this._delegateEventHandler,ve)})}removeHandler(n,t,e){let o=this._events.get(n);if(!o)return;let i=o.get(t);i&&(i.delete(e),i.size===0&&o.delete(t),o.size===0&&(this._events.delete(n),document.removeEventListener(n,this._delegateEventHandler,ve)))}_delegateEventHandler=n=>{let t=g(n);t&&this._events.get(n.type)?.forEach((e,o)=>{(o===t||o.contains(t))&&e.forEach(i=>i.handleEvent(n))})}};var J={enterDuration:225,exitDuration:150};var Ue=800;var ge=j({passive:!0,capture:!0});var ye=[`mousedown`,`touchstart`];var _e=[`mouseup`,`mouseleave`,`touchend`,`touchcancel`];var Ve=(()=>{class a{static ɵfac=function(e){return new(e||a)};static ɵcmp=rI({type:a,selectors:[[`ng-component`]],hostAttrs:[`mat-ripple-style-loader`,``],decls:0,vars:0,template:function(e,o){},styles:[`.mat-ripple {
  overflow: hidden;
  position: relative;
}
.mat-ripple:not(:empty) {
  transform: translateZ(0);
}

.mat-ripple.mat-ripple-unbounded {
  overflow: visible;
}

.mat-ripple-element {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
  transition: opacity, transform 0ms cubic-bezier(0, 0, 0.2, 1);
  transform: scale3d(0, 0, 0);
  background-color: var(--%NS%mat-ripple-color, color-mix(in srgb, var(--%NS%mat-sys-on-surface) 10%, transparent));
}
@media (forced-colors: active) {
  .mat-ripple-element {
    display: none;
  }
}
.cdk-drag-preview .mat-ripple-element, .cdk-drag-placeholder .mat-ripple-element {
  display: none;
}
`],encapsulation:2})}return a})();var vt=class a{_target;_ngZone;_platform;_containerElement;_triggerElement=null;_isPointerDown=!1;_activeRipples=new Map;_mostRecentTransientRipple=null;_lastTouchStartEvent;_pointerUpEventsRegistered=!1;_containerRect=null;static _eventManager=new Pt;constructor(n,t,e,o,i){this._target=n,this._ngZone=t,this._platform=o,o.isBrowser&&(this._containerElement=M(e)),i&&i.get(rt).load(Ve)}fadeInRipple(n,t,e={}){let o=this._containerRect=this._containerRect||this._containerElement.getBoundingClientRect(),i=O(O({},J),e.animation);e.centered&&(n=o.left+o.width/2,t=o.top+o.height/2);let l=e.radius||He(n,t,o),h=n-o.left,R=t-o.top,T=i.enterDuration,b=document.createElement(`div`);b.classList.add(`mat-ripple-element`),b.style.left=`${h-l}px`,b.style.top=`${R-l}px`,b.style.height=`${l*2}px`,b.style.width=`${l*2}px`,e.color!=null&&(b.style.backgroundColor=e.color),b.style.transitionDuration=`${T}ms`,this._containerElement.appendChild(b);let Ut=window.getComputedStyle(b),Ce=Ut.transitionProperty,Vt=Ut.transitionDuration,St=Ce===`none`||Vt===`0s`||Vt===`0s, 0s`||o.width===0&&o.height===0,k=new Lt(this,b,e,St);b.style.transform=`scale3d(1, 1, 1)`,k.state=f.FADING_IN,e.persistent||(this._mostRecentTransientRipple=k);let tt=null;return!St&&(T||i.exitDuration)&&this._ngZone.runOutsideAngular(()=>{let Ht=()=>{tt&&(tt.fallbackTimer=null),clearTimeout(Gt),this._finishRippleTransition(k)},Nt=()=>this._destroyRipple(k),Gt=setTimeout(Nt,T+100);b.addEventListener(`transitionend`,Ht),b.addEventListener(`transitioncancel`,Nt),tt={onTransitionEnd:Ht,onTransitionCancel:Nt,fallbackTimer:Gt}}),this._activeRipples.set(k,tt),(St||!T)&&this._finishRippleTransition(k),k}fadeOutRipple(n){if(n.state===f.FADING_OUT||n.state===f.HIDDEN)return;let t=n.element,e=O(O({},J),n.config.animation);t.style.transitionDuration=`${e.exitDuration}ms`,t.style.opacity=`0`,n.state=f.FADING_OUT,(n._animationForciblyDisabledThroughCss||!e.exitDuration)&&this._finishRippleTransition(n)}fadeOutAll(){this._getActiveRipples().forEach(n=>n.fadeOut())}fadeOutAllNonPersistent(){this._getActiveRipples().forEach(n=>{n.config.persistent||n.fadeOut()})}setupTriggerEvents(n){let t=M(n);!this._platform.isBrowser||!t||t===this._triggerElement||(this._removeTriggerEvents(),this._triggerElement=t,ye.forEach(e=>{a._eventManager.addHandler(this._ngZone,e,t,this)}))}handleEvent(n){n.type===`mousedown`?this._onMousedown(n):n.type===`touchstart`?this._onTouchStart(n):this._onPointerUp(),this._pointerUpEventsRegistered||(this._ngZone.runOutsideAngular(()=>{_e.forEach(t=>{this._triggerElement.addEventListener(t,this,ge)})}),this._pointerUpEventsRegistered=!0)}_finishRippleTransition(n){n.state===f.FADING_IN?this._startFadeOutTransition(n):n.state===f.FADING_OUT&&this._destroyRipple(n)}_startFadeOutTransition(n){let t=n===this._mostRecentTransientRipple,{persistent:e}=n.config;n.state=f.VISIBLE,!e&&(!t||!this._isPointerDown)&&n.fadeOut()}_destroyRipple(n){let t=this._activeRipples.get(n)??null;this._activeRipples.delete(n),this._activeRipples.size||(this._containerRect=null),n===this._mostRecentTransientRipple&&(this._mostRecentTransientRipple=null),n.state=f.HIDDEN,t!==null&&(n.element.removeEventListener(`transitionend`,t.onTransitionEnd),n.element.removeEventListener(`transitioncancel`,t.onTransitionCancel),t.fallbackTimer!==null&&clearTimeout(t.fallbackTimer)),n.element.remove()}_onMousedown(n){let t=W(n),e=this._lastTouchStartEvent&&Date.now()<this._lastTouchStartEvent+Ue;!this._target.rippleDisabled&&!t&&!e&&(this._isPointerDown=!0,this.fadeInRipple(n.clientX,n.clientY,this._target.rippleConfig))}_onTouchStart(n){if(!this._target.rippleDisabled&&!X(n)){this._lastTouchStartEvent=Date.now(),this._isPointerDown=!0;let t=n.changedTouches;if(t)for(let e=0;e<t.length;e++)this.fadeInRipple(t[e].clientX,t[e].clientY,this._target.rippleConfig)}}_onPointerUp(){this._isPointerDown&&(this._isPointerDown=!1,this._getActiveRipples().forEach(n=>{let t=n.state===f.VISIBLE||n.config.terminateOnPointerUp&&n.state===f.FADING_IN;!n.config.persistent&&t&&n.fadeOut()}))}_getActiveRipples(){return Array.from(this._activeRipples.keys())}_removeTriggerEvents(){let n=this._triggerElement;n&&(ye.forEach(t=>a._eventManager.removeHandler(t,n,this)),this._pointerUpEventsRegistered&&(_e.forEach(t=>n.removeEventListener(t,this,ge)),this._pointerUpEventsRegistered=!1))}};function He(a,n,t){let e=Math.max(Math.abs(a-t.left),Math.abs(a-t.right)),o=Math.max(Math.abs(n-t.top),Math.abs(n-t.bottom));return Math.sqrt(e*e+o*o)}var Se=new C(`mat-ripple-global-options`);var Ge={capture:!0};var Ze=[`focus`,`mousedown`,`mouseenter`,`touchstart`];var zt=`mat-ripple-loader-uninitialized`;var Bt=`mat-ripple-loader-class-name`;var Ne=`mat-ripple-loader-centered`;var gt=`mat-ripple-loader-disabled`;var xe=(()=>{class a{_document=g$1(Te$1);_animationsDisabled=R();_globalRippleOptions=g$1(Se,{optional:!0});_platform=g$1(l);_ngZone=g$1(z);_injector=g$1(Ne$1);_eventCleanups;_hosts=new Map;constructor(){let t=g$1(Hr).createRenderer(null,null);this._eventCleanups=this._ngZone.runOutsideAngular(()=>Ze.map(e=>t.listen(this._document,e,this._onInteraction,Ge)))}ngOnDestroy(){let t=this._hosts.keys();for(let e of t)this.destroyRipple(e);this._eventCleanups.forEach(e=>e())}configureRipple(t,e){t.setAttribute(zt,this._globalRippleOptions?.namespace??``),(e.className||!t.hasAttribute(Bt))&&t.setAttribute(Bt,e.className||``),e.centered&&t.setAttribute(Ne,``),e.disabled&&t.setAttribute(gt,``)}setDisabled(t,e){let o=this._hosts.get(t);o?(o.target.rippleDisabled=e,!e&&!o.hasSetUpEvents&&(o.hasSetUpEvents=!0,o.renderer.setupTriggerEvents(t))):e?t.setAttribute(gt,``):t.removeAttribute(gt)}_onInteraction=t=>{let e=g(t);if(e instanceof HTMLElement){let o=e.closest(`[${zt}="${this._globalRippleOptions?.namespace??``}"]`);o&&this._createRipple(o)}};_createRipple(t){if(!this._document||this._hosts.has(t))return;t.querySelector(`.mat-ripple`)?.remove();let e=this._document.createElement(`span`);e.classList.add(`mat-ripple`,t.getAttribute(Bt)),t.append(e);let o=this._globalRippleOptions,i=this._animationsDisabled?0:o?.animation?.enterDuration??J.enterDuration,l=this._animationsDisabled?0:o?.animation?.exitDuration??J.exitDuration,h={rippleDisabled:this._animationsDisabled||o?.disabled||t.hasAttribute(gt),rippleConfig:{centered:t.hasAttribute(Ne),terminateOnPointerUp:o?.terminateOnPointerUp,animation:{enterDuration:i,exitDuration:l}}},R=new vt(h,this._ngZone,e,this._platform,this._injector),T=!h.rippleDisabled;T&&R.setupTriggerEvents(t),this._hosts.set(t,{target:h,renderer:R,hasSetUpEvents:T}),t.removeAttribute(zt)}destroyRipple(t){let e=this._hosts.get(t);e&&(e.renderer._removeTriggerEvents(),this._hosts.delete(t))}static ɵfac=function(e){return new(e||a)};static ɵprov=ke$1({token:a,factory:a.ɵfac})}return a})();var we=(()=>{class a{static ɵfac=function(e){return new(e||a)};static ɵcmp=rI({type:a,selectors:[[`structural-styles`]],decls:0,vars:0,template:function(e,o){},styles:[`.mat-focus-indicator {
  position: relative;
}
.mat-focus-indicator::before {
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  position: absolute;
  box-sizing: border-box;
  pointer-events: none;
  display: var(--%NS%mat-focus-indicator-display, none);
  border-width: var(--%NS%mat-focus-indicator-border-width, 3px);
  border-style: var(--%NS%mat-focus-indicator-border-style, solid);
  border-color: var(--%NS%mat-focus-indicator-border-color, transparent);
  border-radius: var(--%NS%mat-focus-indicator-border-radius, 4px);
}
.mat-focus-indicator:focus-visible::before {
  content: "";
}

@media (forced-colors: active) {
  html {
    --%NS%mat-focus-indicator-display: block;
    --%NS%mat-focus-indicator-fallback-border-style: none;
  }
}
`],encapsulation:2})}return a})();var $e=[`*`,[[``,`progressIndicator`,``]]];var Ye=[`*`,`[progressIndicator]`];function Ke(a,n){a&1&&(Du(0,`div`,1),HI(1,1),Eu())}var qe=new C(`MAT_BUTTON_CONFIG`);function Ee(a){return a==null?void 0:zL(a)}var Me=(()=>{class a{_elementRef=g$1(hn);_ngZone=g$1(z);_animationsDisabled=R();_config=g$1(qe,{optional:!0});_focusMonitor=g$1(Ot);_cleanupClick;_renderer=g$1($r);_rippleLoader=g$1(xe);_isAnchor;_isFab=!1;color;get disableRipple(){return this._disableRipple}set disableRipple(t){this._disableRipple=t,this._updateRippleDisabled()}_disableRipple=!1;get disabled(){return this._disabled}set disabled(t){this._disabled=t,this._updateRippleDisabled()}_disabled=!1;ariaDisabled;disabledInteractive;tabIndex;set _tabindex(t){this.tabIndex=t}showProgress=jL(!1,{transform:UL});constructor(){g$1(rt).load(we);let t=this._elementRef.nativeElement;this._isAnchor=t.tagName===`A`,this.disabledInteractive=this._config?.disabledInteractive??!1,this.color=this._config?.color??null,this._rippleLoader?.configureRipple(t,{className:`mat-mdc-button-ripple`})}ngAfterViewInit(){this._focusMonitor.monitor(this._elementRef,!0),this._isAnchor&&this._setupAsAnchor()}ngOnDestroy(){this._cleanupClick?.(),this._focusMonitor.stopMonitoring(this._elementRef),this._rippleLoader?.destroyRipple(this._elementRef.nativeElement)}focus(t=`program`,e){t?this._focusMonitor.focusVia(this._elementRef.nativeElement,t,e):this._elementRef.nativeElement.focus(e)}_getAriaDisabled(){return this.ariaDisabled!=null?this.ariaDisabled:this._isAnchor?this.disabled||null:this.disabled&&this.disabledInteractive?!0:null}_getDisabledAttribute(){return this.disabledInteractive||!this.disabled?null:!0}_updateRippleDisabled(){this._rippleLoader?.setDisabled(this._elementRef.nativeElement,this.disableRipple||this.disabled)}_getTabIndex(){return this._isAnchor?this.disabled&&!this.disabledInteractive?-1:this.tabIndex:this.tabIndex}_setupAsAnchor(){this._cleanupClick=this._ngZone.runOutsideAngular(()=>this._renderer.listen(this._elementRef.nativeElement,`click`,t=>{this.disabled&&(t.preventDefault(),t.stopImmediatePropagation())}))}static ɵfac=function(e){return new(e||a)};static ɵdir=hu({type:a,hostAttrs:[1,`mat-mdc-button-base`],hostVars:15,hostBindings:function(e,o){e&2&&(ih(`disabled`,o._getDisabledAttribute())(`aria-disabled`,o._getAriaDisabled())(`tabindex`,o._getTabIndex()),nw(o.color?`mat-`+o.color:``),wh(`mat-mdc-button-progress-indicator-shown`,o.showProgress())(`mat-mdc-button-disabled`,o.disabled)(`mat-mdc-button-disabled-interactive`,o.disabledInteractive)(`mat-unthemed`,!o.color)(`_mat-animation-noopable`,o._animationsDisabled))},inputs:{color:`color`,disableRipple:[2,`disableRipple`,`disableRipple`,UL],disabled:[2,`disabled`,`disabled`,UL],ariaDisabled:[2,`aria-disabled`,`ariaDisabled`,UL],disabledInteractive:[2,`disabledInteractive`,`disabledInteractive`,UL],tabIndex:[2,`tabIndex`,`tabIndex`,Ee],_tabindex:[2,`tabindex`,`_tabindex`,Ee],showProgress:[1,`showProgress`]}})}return a})();var jt=(()=>{class a extends Me{constructor(){super(),this._rippleLoader.configureRipple(this._elementRef.nativeElement,{centered:!0})}static ɵfac=function(e){return new(e||a)};static ɵcmp=rI({type:a,selectors:[[`button`,`mat-icon-button`,``],[`a`,`mat-icon-button`,``],[`button`,`matIconButton`,``],[`a`,`matIconButton`,``]],hostAttrs:[1,`mdc-icon-button`,`mat-mdc-icon-button`],exportAs:[`matButton`,`matAnchor`],features:[th],ngContentSelectors:Ye,decls:5,vars:1,consts:[[1,`mat-mdc-button-persistent-ripple`,`mdc-icon-button__ripple`],[1,`mat-mdc-button-progress-indicator-container`],[1,`mat-focus-indicator`],[1,`mat-mdc-button-touch-target`]],template:function(e,o){e&1&&(VI($e),ch(0,`span`,0),HI(1),wI(2,Ke,2,0,`div`,1),ch(3,`span`,2)(4,`span`,3)),e&2&&(wD(2),CI(o.showProgress()?2:-1))},styles:[`.mat-mdc-icon-button {
  -webkit-user-select: none;
  user-select: none;
  display: inline-block;
  position: relative;
  box-sizing: border-box;
  border: none;
  outline: none;
  background-color: transparent;
  fill: currentColor;
  text-decoration: none;
  cursor: pointer;
  z-index: 0;
  overflow: visible;
  border-radius: var(--%NS%mat-icon-button-container-shape, var(--%NS%mat-sys-corner-full, 50%));
  flex-shrink: 0;
  text-align: center;
  width: var(--%NS%mat-icon-button-state-layer-size, 40px);
  height: var(--%NS%mat-icon-button-state-layer-size, 40px);
  padding: calc(calc(var(--%NS%mat-icon-button-state-layer-size, 40px) - var(--%NS%mat-icon-button-icon-size, 24px)) / 2);
  font-size: var(--%NS%mat-icon-button-icon-size, 24px);
  color: var(--%NS%mat-icon-button-icon-color, var(--%NS%mat-sys-on-surface-variant));
  -webkit-tap-highlight-color: transparent;
}
.mat-mdc-icon-button .mat-mdc-button-ripple,
.mat-mdc-icon-button .mat-mdc-button-persistent-ripple,
.mat-mdc-icon-button .mat-mdc-button-persistent-ripple::before {
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  position: absolute;
  pointer-events: none;
  border-radius: inherit;
}
.mat-mdc-icon-button .mat-mdc-button-ripple {
  overflow: hidden;
}
.mat-mdc-icon-button .mat-mdc-button-persistent-ripple::before {
  content: "";
  opacity: 0;
}
.mat-mdc-icon-button .mdc-button__label,
.mat-mdc-icon-button .mat-icon {
  z-index: 1;
  position: relative;
}
.mat-mdc-icon-button .mat-focus-indicator {
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  position: absolute;
  border-radius: inherit;
}
.mat-mdc-icon-button:focus-visible > .mat-focus-indicator::before {
  content: "";
  border-radius: inherit;
}
.mat-mdc-icon-button .mat-ripple-element {
  background-color: var(--%NS%mat-icon-button-ripple-color, color-mix(in srgb, var(--%NS%mat-sys-on-surface-variant) calc(var(--%NS%mat-sys-pressed-state-layer-opacity) * 100%), transparent));
}
.mat-mdc-icon-button .mat-mdc-button-persistent-ripple::before {
  background-color: var(--%NS%mat-icon-button-state-layer-color, var(--%NS%mat-sys-on-surface-variant));
}
.mat-mdc-icon-button.mat-mdc-button-disabled .mat-mdc-button-persistent-ripple::before {
  background-color: var(--%NS%mat-icon-button-disabled-state-layer-color, var(--%NS%mat-sys-on-surface-variant));
}
.mat-mdc-icon-button:hover > .mat-mdc-button-persistent-ripple::before {
  opacity: var(--%NS%mat-icon-button-hover-state-layer-opacity, var(--%NS%mat-sys-hover-state-layer-opacity));
}
.mat-mdc-icon-button.cdk-program-focused > .mat-mdc-button-persistent-ripple::before, .mat-mdc-icon-button.cdk-keyboard-focused > .mat-mdc-button-persistent-ripple::before, .mat-mdc-icon-button.mat-mdc-button-disabled-interactive:focus > .mat-mdc-button-persistent-ripple::before {
  opacity: var(--%NS%mat-icon-button-focus-state-layer-opacity, var(--%NS%mat-sys-focus-state-layer-opacity));
}
.mat-mdc-icon-button:active > .mat-mdc-button-persistent-ripple::before {
  opacity: var(--%NS%mat-icon-button-pressed-state-layer-opacity, var(--%NS%mat-sys-pressed-state-layer-opacity));
}
.mat-mdc-icon-button .mat-mdc-button-touch-target {
  position: absolute;
  top: 50%;
  height: var(--%NS%mat-icon-button-touch-target-size, 48px);
  display: var(--%NS%mat-icon-button-touch-target-display, block);
  left: 50%;
  width: var(--%NS%mat-icon-button-touch-target-size, 48px);
  transform: translate(-50%, -50%);
}
.mat-mdc-icon-button._mat-animation-noopable {
  transition: none !important;
  animation: none !important;
}
.mat-mdc-icon-button[disabled], .mat-mdc-icon-button.mat-mdc-button-disabled {
  cursor: default;
  pointer-events: none;
  color: var(--%NS%mat-icon-button-disabled-icon-color, color-mix(in srgb, var(--%NS%mat-sys-on-surface) 38%, transparent));
}
.mat-mdc-icon-button.mat-mdc-button-disabled-interactive {
  pointer-events: auto;
}
.mat-mdc-icon-button img,
.mat-mdc-icon-button svg {
  width: var(--%NS%mat-icon-button-icon-size, 24px);
  height: var(--%NS%mat-icon-button-icon-size, 24px);
  vertical-align: baseline;
}
.mat-mdc-icon-button .mat-mdc-button-progress-indicator-container .mdc-circular-progress__determinate-circle-graphic {
  width: inherit;
  height: inherit;
}
.mat-mdc-icon-button .mat-mdc-button-progress-indicator-container .mdc-circular-progress__indeterminate-circle-graphic {
  height: 100%;
}
.mat-mdc-icon-button .mat-mdc-button-persistent-ripple {
  border-radius: var(--%NS%mat-icon-button-container-shape, var(--%NS%mat-sys-corner-full, 50%));
}
.mat-mdc-icon-button[hidden] {
  display: none;
}
.mat-mdc-icon-button.mat-unthemed:not(.mdc-ripple-upgraded):focus::before, .mat-mdc-icon-button.mat-primary:not(.mdc-ripple-upgraded):focus::before, .mat-mdc-icon-button.mat-accent:not(.mdc-ripple-upgraded):focus::before, .mat-mdc-icon-button.mat-warn:not(.mdc-ripple-upgraded):focus::before {
  background: transparent;
  opacity: 1;
}

.mat-mdc-button-progress-indicator-container {
  position: absolute;
  inset-inline-start: 0;
  inset-block-start: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
}

.mat-mdc-button-progress-indicator-shown mat-icon {
  visibility: hidden;
}
`,`@media (forced-colors: active) {
  .mat-mdc-button:not(.mdc-button--outlined),
  .mat-mdc-unelevated-button:not(.mdc-button--outlined),
  .mat-mdc-raised-button:not(.mdc-button--outlined),
  .mat-mdc-outlined-button:not(.mdc-button--outlined),
  .mat-mdc-button-base.mat-tonal-button,
  .mat-mdc-icon-button.mat-mdc-icon-button,
  .mat-mdc-outlined-button .mdc-button__ripple {
    outline: solid 1px;
  }
}
`],encapsulation:2})}return a})();var Te=(()=>{class a{static ɵfac=function(e){return new(e||a)};static ɵmod=es({type:a});static ɵinj=Dr({imports:[ot]})}return a})();var ke=(()=>{class a{static ɵfac=function(e){return new(e||a)};static ɵmod=es({type:a});static ɵinj=Dr({imports:[Te,ot]})}return a})();var Ie=`ls-theme`;var yt=class a{destroyRef=g$1(me$1);mq=window.matchMedia(`(prefers-color-scheme: dark)`);colapsado=jL(!1);abierto=jL(!1);titulo=jL(``);fecha=jL(``);alternarMenu=PL();salir=PL();cerrando=Vw(()=>this.colapsado()||this.abierto());oscuro=fi(this.mq.matches);ngOnInit(){this.aplicar(this.leerPreferencia()),this.mq.addEventListener(`change`,this.onSistema),this.destroyRef.onDestroy(()=>this.mq.removeEventListener(`change`,this.onSistema))}alternarTema(){let n=this.oscuro()?`light`:`dark`;this.aplicar(n);try{localStorage.setItem(Ie,n)}catch(t){}}etiquetaTema=Vw(()=>this.oscuro()?`Cambiar a modo claro`:`Cambiar a modo oscuro`);onSistema=()=>{this.leerPreferencia()||this.aplicar(null)};leerPreferencia(){try{let n=localStorage.getItem(Ie);return n===`dark`||n===`light`?n:null}catch(n){return null}}aplicar(n){let t=n??(this.mq.matches?`dark`:`light`),e=document.documentElement;e.classList.toggle(`theme-dark`,t===`dark`),e.classList.toggle(`theme-light`,t===`light`),this.oscuro.set(t===`dark`)}static ɵfac=function(t){return new(t||a)};static ɵcmp=rI({type:a,selectors:[[`app-header`]],inputs:{colapsado:[1,`colapsado`],abierto:[1,`abierto`],titulo:[1,`titulo`],fecha:[1,`fecha`]},outputs:{alternarMenu:`alternarMenu`,salir:`salir`},decls:16,vars:8,consts:[[`role`,`banner`,1,`topbar`],[`mat-icon-button`,``,1,`topbar-menu`,3,`click`],[1,`topbar-title`],[1,`topbar-spacer`],[1,`topbar-date`],[`mat-icon-button`,``,`type`,`button`,1,`topbar-tema`,3,`click`],[1,`btn`,`sm`,`primary`,3,`click`]],template:function(t,e){t&1&&(Ui(0,`mat-toolbar`,0)(1,`button`,1),ph(`click`,function(){return e.alternarMenu.emit()}),Ui(2,`mat-icon`),pw(3),vu()(),Ui(4,`h1`,2),pw(5),vu(),ah(6,`span`,3),Ui(7,`span`,4),pw(8),vu(),Ui(9,`button`,5),ph(`click`,function(){return e.alternarTema()}),Ui(10,`mat-icon`),pw(11),vu()(),Ui(12,`button`,6),ph(`click`,function(){return e.salir.emit()}),Ui(13,`mat-icon`),pw(14,`logout`),vu(),pw(15,` Cerrar sesión `),vu()()),t&2&&(wD(),ih(`aria-label`,e.cerrando()?`Cerrar menú`:`Abrir menú`)(`title`,e.cerrando()?`Cerrar menú`:`Abrir menú`),wD(2),Mh(e.cerrando()?`menu_open`:`menu`),wD(2),Mh(e.titulo()),wD(3),Mh(e.fecha()),wD(),ih(`aria-label`,e.etiquetaTema())(`title`,e.etiquetaTema()),wD(2),Mh(e.oscuro()?`light_mode`:`dark_mode`))},dependencies:[ft,pt,Wt,Ot$1,ke,jt],styles:[`[_nghost-%COMP%]{display:block}`]})};var _t=class a{anio=new Date().getFullYear();static ɵfac=function(t){return new(t||a)};static ɵcmp=rI({type:a,selectors:[[`app-footer`]],decls:6,vars:1,consts:[[`role`,`contentinfo`,1,`app-footer`,`mat-elevation-z2`],[1,`spacer`],[1,`mat-body-small`,`muted`]],template:function(t,e){t&1&&(Ui(0,`mat-toolbar`,0)(1,`span`),pw(2),vu(),ah(3,`span`,1),Ui(4,`span`,2),pw(5,`v1.0`),vu()()),t&2&&(wD(2),Iu(`© `,e.anio,` Little Smile CRM`))},dependencies:[ft,pt],styles:[`[_nghost-%COMP%]{display:block}`]})};function Xe(a,n){if(a&1){let t=OI();Ui(0,`div`,7),ph(`click`,function(){td(t);return nd(jI().cerrarMenu())}),vu()}}var De=class a{colapsado=fi(!1);abierto=fi(!1);router=g$1(le$1);auth=g$1(u);sub;constructor(){this.sub=this.router.events.subscribe(n=>{n instanceof q$1&&this.abierto.set(!1)})}ngOnDestroy(){this.sub.unsubscribe()}get titulo(){let n=this.router.url.split(`/`)[1];return n===`citas`?`Citas`:n===`dashboard`?`Inicio`:n===`pacientes`?`Pacientes`:n===`odontologos`?`Odontólogos`:n===`servicios`?`Servicios`:n===`horarios`?`Horarios`:n===`agenda`?`Agenda`:n===`whatsapp`?`WhatsApp`:n===`redes-sociales`?`Redes sociales`:n===`google`?`Google Calendar`:n===`plantillas`?`Plantillas`:n===`automatizaciones`?`Automatizaciones`:n===`notificaciones`?`Notificaciones`:n===`agente`?`Agente IA`:n===`reportes`?`Reportes`:n===`auditoria`?`Auditoría`:n===`usuarios`?`Usuarios`:ut.flatMap(t=>t.items).find(t=>t.path===`/`+n)?.label??`Inicio`}get fecha(){return new Date().toLocaleDateString(`es-EC`,{weekday:`long`,day:`numeric`,month:`long`,year:`numeric`})}alternar(){this.esMovil()?this.abierto.update(n=>!n):this.colapsado.update(n=>!n)}cerrarMenu(){this.abierto.set(!1)}salir(){this.auth.logout()}esMovil(){return typeof window<`u`&&window.matchMedia(`(max-width: 991px)`).matches}cerrarConEscape(){this.abierto()&&this.cerrarMenu()}static ɵfac=function(t){return new(t||a)};static ɵcmp=rI({type:a,selectors:[[`app-layout`]],hostBindings:function(t,e){t&1&&ph(`keydown.escape`,function(){return e.cerrarConEscape()},kv)},decls:9,vars:7,consts:[[1,`app-shell`],[3,`salir`,`navegar`,`colapsado`,`abierto`],[`role`,`presentation`,1,`sidebar-backdrop`],[1,`main`],[3,`alternarMenu`,`salir`,`colapsado`,`abierto`,`titulo`,`fecha`],[1,`content`],[1,`content-wrapper`],[`role`,`presentation`,1,`sidebar-backdrop`,3,`click`]],template:function(t,e){t&1&&(Ui(0,`div`,0)(1,`app-sidebar`,1),ph(`salir`,function(){return e.salir()})(`navegar`,function(){return e.cerrarMenu()}),vu(),wI(2,Xe,1,0,`div`,2),Ui(3,`div`,3)(4,`app-header`,4),ph(`alternarMenu`,function(){return e.alternar()})(`salir`,function(){return e.salir()}),vu(),Ui(5,`main`,5)(6,`div`,6),ah(7,`router-outlet`),vu()(),ah(8,`app-footer`),vu()()),t&2&&(wD(),sh(`colapsado`,e.colapsado())(`abierto`,e.abierto()),wD(),CI(e.abierto()?2:-1),wD(2),sh(`colapsado`,e.colapsado())(`abierto`,e.abierto())(`titulo`,e.titulo)(`fecha`,e.fecha))},dependencies:[lr$1,bt,yt,_t],styles:[`[_nghost-%COMP%]{display:block;height:100%}`]})};export{De as LayoutComponent};