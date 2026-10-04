import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '@siga/core';
import { AsignaturaAdmin, MallaFila, UsuarioAdmin } from '@siga/mocks';
import { Observable, of, shareReplay } from 'rxjs';

/**
 * Datos de gestion institucional (usuarios y asignaturas) para el portal admin.
 * En modo demo (`useMocks`) devuelve `null`; los errores reales se propagan.
 */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);
  private readonly cache = new Map<string, Observable<unknown>>();

  getUsuarios(): Observable<UsuarioAdmin[] | null> {
    return this.obtener<UsuarioAdmin[]>('usuarios');
  }

  getAsignaturas(): Observable<AsignaturaAdmin[] | null> {
    return this.obtener<AsignaturaAdmin[]>('asignaturas');
  }

  getMalla(): Observable<MallaFila[] | null> {
    return this.obtener<MallaFila[]>('malla');
  }

  private obtener<T>(recurso: string): Observable<T | null> {
    const cacheado = this.cache.get(recurso);
    if (cacheado) {
      return cacheado as Observable<T | null>;
    }

    const request$ = this.config.useMocks
      ? of<T | null>(null)
      : this.http.get<T>(`${this.config.bffBaseUrl}/bff/v1/admin/${recurso}`);

    const compartido$ = request$.pipe(shareReplay(1));
    this.cache.set(recurso, compartido$);
    return compartido$;
  }

  /** Invalida el cache para forzar una recarga en la proxima navegacion. */
  invalidar(): void {
    this.cache.clear();
  }
}
