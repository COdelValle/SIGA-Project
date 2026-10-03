import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '@siga/core';
import { Observable, catchError, of, shareReplay } from 'rxjs';
import { PerfilEstudianteDTO } from '../models/perfil.model';

/**
 * Consume el perfil academico real del BFF. Devuelve `null` cuando el BFF no
 * responde (o `useMocks` esta activo) para que las vistas usen el mock de
 * respaldo.
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
      : this.http
          .get<PerfilEstudianteDTO>(
            `${this.config.bffBaseUrl}/bff/v1/estudiantes/perfil/${clave}`,
          )
          .pipe(catchError(() => of<PerfilEstudianteDTO | null>(null)));

    const compartido$ = request$.pipe(shareReplay(1));
    this.cache.set(clave, compartido$);
    return compartido$;
  }
}
