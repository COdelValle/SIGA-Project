import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '@siga/core';
import { Observable, of, shareReplay } from 'rxjs';
import { PerfilEstudianteDTO } from '../models/perfil.model';

/**
 * Consume el perfil academico real del BFF. En modo demo (`useMocks`) devuelve
 * `null`; los errores reales se propagan a la vista (sin fallback a mocks).
 */
@Injectable({ providedIn: 'root' })
export class PerfilEstudianteService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);
  private readonly cache = new Map<string, Observable<PerfilEstudianteDTO | null>>();

  getPerfilMe(): Observable<PerfilEstudianteDTO | null> {
    return this.getPerfilPorClave('me');
  }

  getPerfil(id: number): Observable<PerfilEstudianteDTO | null> {
    return this.getPerfilPorClave(String(id));
  }

  private getPerfilPorClave(clave: string): Observable<PerfilEstudianteDTO | null> {
    const cacheado = this.cache.get(clave);
    if (cacheado) {
      return cacheado;
    }

    const request$ = this.config.useMocks
      ? of<PerfilEstudianteDTO | null>(null)
      : this.http.get<PerfilEstudianteDTO>(
          `${this.config.bffBaseUrl}/bff/v1/estudiantes/perfil/${clave}`,
        );

    const compartido$ = request$.pipe(shareReplay(1));
    this.cache.set(clave, compartido$);
    return compartido$;
  }

  /** Invalida el cache de un perfil (o todos) para ver cambios sin recargar. */
  invalidar(id?: number | 'me'): void {
    if (id === undefined) {
      this.cache.clear();
      return;
    }
    this.cache.delete(String(id));
  }
}
