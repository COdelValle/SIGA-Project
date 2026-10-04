import { Signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Observable, catchError, map, of } from 'rxjs';

export type EstadoRemoto = 'cargando' | 'listo' | 'error';

export interface RecursoRemoto<T> {
  estado: EstadoRemoto;
  dato: T | null;
  error: unknown;
}

/**
 * Convierte una fuente HTTP en una signal con estado explicito. Los errores
 * dejan de caer silenciosamente a mocks: la vista muestra estado de error.
 */
export function recursoRemoto<T>(fuente$: Observable<T>): Signal<RecursoRemoto<T>> {
  return toSignal(
    fuente$.pipe(
      map((dato): RecursoRemoto<T> => ({ estado: 'listo', dato, error: null })),
      catchError((error: unknown) =>
        of<RecursoRemoto<T>>({ estado: 'error', dato: null, error }),
      ),
    ),
    { initialValue: { estado: 'cargando', dato: null, error: null } },
  );
}
