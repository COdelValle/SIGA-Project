import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG, RefrescoDatosService } from '@siga/core';
import { Observable, of, shareReplay, startWith, switchMap, tap } from 'rxjs';
import { AsistenciaDTO } from '../models/perfil.model';

export interface RegistrarAsistenciaRequest {
  idEstudiante: number;
  idCursoAsignatura: number;
  fecha: string;
  estado: 'PRESENTE' | 'AUSENTE' | 'ATRASADO';
  observacion?: string | null;
}

export interface ActualizarAsistenciaRequest {
  justificacion: 'SI' | 'NO' | 'PENDIENTE' | 'NO_APLICA';
  observacion?: string | null;
  estado?: 'PRESENTE' | 'AUSENTE' | 'ATRASADO' | null;
}

/**
 * Consume las asistencias reales del BFF. En modo demo (`useMocks`) devuelve
 * `null`/`[]`; los errores reales se propagan a la vista (sin fallback a mocks).
 * Cada clave se reconsulta cuando `RefrescoDatosService` avisa datos nuevos
 * (por ejemplo, al llegar una notificacion de asistencia).
 */
@Injectable({ providedIn: 'root' })
export class AsistenciaService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);
  private readonly refresco = inject(RefrescoDatosService);
  private readonly cache = new Map<string, Observable<AsistenciaDTO[] | null>>();

  getAsistenciasMe(): Observable<AsistenciaDTO[] | null> {
    return this.getAsistencias('me');
  }

  getAsistenciasEstudiante(id: number): Observable<AsistenciaDTO[] | null> {
    return this.getAsistencias(String(id));
  }

  /** Asistencias de una dictación en una fecha (portal docente). */
  getAsistenciasAsignatura(idCursoAsignatura: number, fecha: string): Observable<AsistenciaDTO[]> {
    if (this.config.useMocks) {
      return of<AsistenciaDTO[]>([]);
    }
    return this.http.get<AsistenciaDTO[]>(
      `${this.config.bffBaseUrl}/bff/v1/asistencias/asignatura/${idCursoAsignatura}`,
      { params: { fecha } },
    );
  }

  registrar(request: RegistrarAsistenciaRequest): Observable<AsistenciaDTO> {
    return this.http
      .post<AsistenciaDTO>(`${this.config.bffBaseUrl}/bff/v1/asistencias`, request)
      .pipe(tap((asistencia) => this.invalidar(asistencia.idEstudiante)));
  }

  actualizar(id: number, request: ActualizarAsistenciaRequest): Observable<AsistenciaDTO> {
    return this.http
      .put<AsistenciaDTO>(`${this.config.bffBaseUrl}/bff/v1/asistencias/${id}`, request)
      .pipe(tap((asistencia) => this.invalidar(asistencia.idEstudiante)));
  }

  /** Invalida el cache de un estudiante (o todos) para ver cambios sin recargar. */
  invalidar(idEstudiante?: number): void {
    if (idEstudiante === undefined) {
      this.cache.clear();
      return;
    }
    this.cache.delete(String(idEstudiante));
  }

  private getAsistencias(clave: string): Observable<AsistenciaDTO[] | null> {
    const cacheado = this.cache.get(clave);
    if (cacheado) {
      return cacheado;
    }

    const compartido$ = this.refresco.refresco$.pipe(
      startWith(undefined),
      switchMap(() =>
        this.config.useMocks
          ? of<AsistenciaDTO[] | null>(null)
          : this.http.get<AsistenciaDTO[]>(
              `${this.config.bffBaseUrl}/bff/v1/asistencias/estudiante/${clave}`,
            ),
      ),
      shareReplay({ bufferSize: 1, refCount: true }),
    );
    this.cache.set(clave, compartido$);
    return compartido$;
  }
}
