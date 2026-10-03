import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '@siga/core';
import { Observable, catchError, of, shareReplay } from 'rxjs';
import { AsistenciaDTO } from '../models/perfil.model';

export interface RegistrarAsistenciaRequest {
  idEstudiante: number;
  idAsignatura: number;
  fecha: string;
  estado: 'PRESENTE' | 'AUSENTE' | 'ATRASADO';
  observacion?: string | null;
}

export interface ActualizarAsistenciaRequest {
  justificacion: 'SI' | 'NO' | 'PENDIENTE' | 'NO_APLICA';
  observacion?: string | null;
}

/**
 * Consume las asistencias reales del BFF. Devuelve `null` cuando el BFF no
 * responde (o `useMocks` esta activo) para que las vistas usen el mock.
 */
@Injectable({ providedIn: 'root' })
export class AsistenciaService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);
  private readonly cache = new Map<string, Observable<AsistenciaDTO[] | null>>();

  getAsistenciasMe(): Observable<AsistenciaDTO[] | null> {
    return this.getAsistencias('me');
  }

  getAsistenciasEstudiante(id: number): Observable<AsistenciaDTO[] | null> {
    return this.getAsistencias(String(id));
  }

  registrar(request: RegistrarAsistenciaRequest): Observable<AsistenciaDTO> {
    return this.http.post<AsistenciaDTO>(`${this.config.bffBaseUrl}/bff/v1/asistencias`, request);
  }

  actualizar(id: number, request: ActualizarAsistenciaRequest): Observable<AsistenciaDTO> {
    return this.http.put<AsistenciaDTO>(`${this.config.bffBaseUrl}/bff/v1/asistencias/${id}`, request);
  }

  private getAsistencias(clave: string): Observable<AsistenciaDTO[] | null> {
    const cacheado = this.cache.get(clave);
    if (cacheado) {
      return cacheado;
    }

    const request$ = this.config.useMocks
      ? of<AsistenciaDTO[] | null>(null)
      : this.http
          .get<AsistenciaDTO[]>(`${this.config.bffBaseUrl}/bff/v1/asistencias/estudiante/${clave}`)
          .pipe(catchError(() => of<AsistenciaDTO[] | null>(null)));

    const compartido$ = request$.pipe(shareReplay(1));
    this.cache.set(clave, compartido$);
    return compartido$;
  }
}
