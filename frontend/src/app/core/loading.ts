import { ChangeDetectorRef } from '@angular/core';
import { Observable, finalize, tap } from 'rxjs';

export interface Loadable {
  cargando: boolean;
}

/**
 * Patrón estándar de carga del ERP.
 *
 * Uso:
 *   withLoading(this, this.api.get<T>('/ruta'), undefined, this.cdr).subscribe({
 *     next:  (data) => { ...asignar estado...; this.cdr.markForCheck(); },
 *     error: (e)    => { this.error = '...'; this.cdr.markForCheck(); },
 *   });
 *
 * Con peticiones secuenciales (reqSeq), pasar `isCurrent` para que
 * `finalize` de una respuesta obsoleta NO apague el spinner mientras
 * la petición más reciente sigue en vuelo.
 *
 * - `cargando = true` se pone al invocar.
 * - `finalize` lo pone en `false` al terminar (next/error/cancel),
 *   salvo que `isCurrent()` devuelva false.
 * - `markForCheck()` (vía `cdr`) marca la vista dirty para que Angular
 *   refresque el DOM en el siguiente tick (necesario en Angular 22).
 */
export function withLoading<T>(
  target: Loadable,
  source$: Observable<T>,
  isCurrent?: () => boolean,
  cdr?: ChangeDetectorRef,
): Observable<T> {
  target.cargando = true;
  cdr?.markForCheck();
  return source$.pipe(
    tap({
      next: () => cdr?.markForCheck(),
      error: () => cdr?.markForCheck(),
    }),
    finalize(() => {
      if (!isCurrent || isCurrent()) {
        target.cargando = false;
        cdr?.markForCheck();
      }
    }),
  );
}
