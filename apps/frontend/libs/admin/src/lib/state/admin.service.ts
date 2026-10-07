import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '@siga/core';
import {
  AsignaturaAdmin,
  ClaseOpcion,
  CredencialTemporal,
  EstudianteOpcion,
  MallaFila,
  RegistroUsuarioEstado,
  RegistroUsuarioPayload,
  UsuarioAdmin,
  UsuarioDetalle,
} from '@siga/mocks';
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

  iniciarRegistro(payload: RegistroUsuarioPayload): Observable<RegistroUsuarioEstado | null> {
    if (this.config.useMocks) {
      return of<RegistroUsuarioEstado | null>(null);
    }
    return this.http.post<RegistroUsuarioEstado>(
      `${this.config.bffBaseUrl}/bff/v1/admin/registraciones`,
      payload,
    );
  }

  getRegistro(processId: string): Observable<RegistroUsuarioEstado> {
    return this.http.get<RegistroUsuarioEstado>(
      `${this.config.bffBaseUrl}/bff/v1/admin/registraciones/${processId}`,
    );
  }

  getCredencialTemporal(processId: string): Observable<CredencialTemporal> {
    return this.http.get<CredencialTemporal>(
      `${this.config.bffBaseUrl}/bff/v1/admin/registraciones/${processId}/credencial`,
    );
  }

  resetPassword(idUsuario: string): Observable<CredencialTemporal> {
    return this.http.post<CredencialTemporal>(
      `${this.config.bffBaseUrl}/bff/v1/admin/usuarios/${idUsuario}/reset-password`,
      {},
    );
  }

  eliminarUsuario(idUsuario: string): Observable<void> {
    return this.http.delete<void>(
      `${this.config.bffBaseUrl}/bff/v1/admin/usuarios/${idUsuario}`,
    );
  }

  getUsuarioDetalle(idUsuario: string): Observable<UsuarioDetalle> {
    return this.http.get<UsuarioDetalle>(
      `${this.config.bffBaseUrl}/bff/v1/admin/usuarios/${idUsuario}`,
    );
  }

  getClases(anioAcademico: number): Observable<ClaseOpcion[] | null> {
    if (this.config.useMocks) {
      return of<ClaseOpcion[] | null>(null);
    }
    return this.http.get<ClaseOpcion[]>(`${this.config.bffBaseUrl}/bff/v1/admin/clases`, {
      params: { anioAcademico },
    });
  }

  buscarEstudiantes(texto: string): Observable<EstudianteOpcion[] | null> {
    if (this.config.useMocks) {
      return of<EstudianteOpcion[] | null>(null);
    }
    return this.http.get<EstudianteOpcion[]>(`${this.config.bffBaseUrl}/bff/v1/admin/estudiantes`, {
      params: { q: texto },
    });
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
