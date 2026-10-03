import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '@siga/core';
import { AsignaturaAdmin, UsuarioAdmin } from '@siga/mocks';
import { Observable, catchError, of, shareReplay } from 'rxjs';

/**
 * Datos de gestion institucional (usuarios y asignaturas) para el portal admin.
 * Devuelve `null` cuando el BFF no responde para que las vistas usen el mock.
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

  private obtener<T>(recurso: string): Observable<T | null> {
    const cacheado = this.cache.get(recurso);
    if (cacheado) {
      return cacheado as Observable<T | null>;
    }

    const request$ = this.config.useMocks
      ? of<T | null>(null)
      : this.http
          .get<T>(`${this.config.bffBaseUrl}/bff/v1/admin/${recurso}`)
          .pipe(catchError(() => of<T | null>(null)));

    const compartido$ = request$.pipe(shareReplay(1));
    this.cache.set(recurso, compartido$);
    return compartido$;
  }
}
