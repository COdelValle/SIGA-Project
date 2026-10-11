import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '@siga/core';
import { DIAS_SEMANA, DiaSemana } from '@siga/academico';
import { ClaseDocente, CursoDocente } from '@siga/mocks';
import { Observable, map, of, shareReplay } from 'rxjs';

export interface ClaseDocenteDTO extends ClaseDocente {
  dia: string;
  horaInicio: string;
  horaFin: string;
}

/**
 * Datos reales del portal docente (cursos y horario). En modo demo (`useMocks`)
 * devuelve `null`; los errores reales se propagan a la vista.
 */
@Injectable({ providedIn: 'root' })
export class DocenteDatosService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);
  private readonly cache = new Map<string, Observable<unknown>>();

  getCursos(): Observable<CursoDocente[] | null> {
    return this.obtener<CursoDocente[]>('cursos');
  }

  getHorario(): Observable<Record<DiaSemana, ClaseDocente[]> | null> {
    return this.obtener<ClaseDocenteDTO[]>('horario').pipe(
      map((clases) => (clases ? agruparPorDia(clases) : null)),
    );
  }

  private obtener<T>(recurso: string): Observable<T | null> {
    const cacheado = this.cache.get(recurso);
    if (cacheado) {
      return cacheado as Observable<T | null>;
    }

    const request$ = this.config.useMocks
      ? of<T | null>(null)
      : this.http.get<T>(`${this.config.bffBaseUrl}/bff/v1/docentes/${recurso}`);

    const compartido$ = request$.pipe(shareReplay(1));
    this.cache.set(recurso, compartido$);
    return compartido$;
  }

  /** Invalida el cache de cursos/horario para forzar una recarga. */
  invalidar(): void {
    this.cache.clear();
  }
}

function normalizarDia(dia: string): DiaSemana | null {
  const norm = (valor: string) =>
    valor
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  const buscado = norm(dia ?? '');
  return DIAS_SEMANA.find((candidato) => norm(candidato) === buscado) ?? null;
}

function agruparPorDia(clases: ClaseDocenteDTO[]): Record<DiaSemana, ClaseDocente[]> {
  const horario: Record<DiaSemana, ClaseDocente[]> = {
    Lunes: [],
    Martes: [],
    Miércoles: [],
    Jueves: [],
    Viernes: [],
  };
  // El BFF envia cada bloque de 45 min como una clase; los bloques
  // consecutivos de un mismo curso comparten franja. Se deja una fila por
  // curso+franja para no duplicar (y no romper el track del @for).
  const vistos: Record<DiaSemana, Set<string>> = {
    Lunes: new Set(),
    Martes: new Set(),
    Miércoles: new Set(),
    Jueves: new Set(),
    Viernes: new Set(),
  };
  for (const clase of clases) {
    const dia = normalizarDia(clase.dia);
    if (!dia) {
      continue;
    }
    const clave = `${clase.cursoId}|${clase.franja}`;
    if (vistos[dia].has(clave)) {
      continue;
    }
    vistos[dia].add(clave);
    horario[dia].push({
      franja: clase.franja,
      cursoId: clase.cursoId,
      curso: clase.curso,
      asignatura: clase.asignatura,
      sala: clase.sala,
    });
  }
  for (const dia of DIAS_SEMANA) {
    horario[dia].sort((a, b) => a.franja - b.franja);
  }
  return horario;
}
