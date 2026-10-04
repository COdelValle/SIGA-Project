import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '@siga/core';
import {
  CURSOS_DOCENTE_MOCK,
  CursoDocente,
  DOCENTE_ACTUAL_ID,
  cursosDelDocente,
} from '@siga/mocks';
import { Observable, of } from 'rxjs';

export type TipoEvaluacion = 'FORMATIVA' | 'DIAGNOSTICO' | 'SUMATIVA';

export interface EvaluacionNotas {
  id: number;
  nombre: string;
  tipo: TipoEvaluacion;
  ponderacion: number;
}

export interface NotaCurso {
  id: number;
  idEvaluacion: number;
  score: number;
}

export interface AlumnoNotas {
  id: number;
  nombres: string;
  firstSurname: string;
  secondSurname: string;
  notas: NotaCurso[];
}

export interface CursoNotas {
  asignaturaId: number;
  curso: string;
  asignatura: string;
  evaluaciones: EvaluacionNotas[];
  alumnos: AlumnoNotas[];
}

export interface EvaluacionRequest {
  nombre: string;
  tipo: TipoEvaluacion;
  ponderacion: number;
}

const TIPOS: TipoEvaluacion[] = ['FORMATIVA', 'SUMATIVA', 'DIAGNOSTICO'];
const PONDERACIONES = [30, 50, 20];
const NOMBRES = ['CONTROL 1', 'PRUEBA', 'DIAGNOSTICO'];

/**
 * Notas y evaluaciones del curso contra el BFF. En modo demo (`useMocks`) usa
 * un set en memoria derivado de los mocks del docente.
 */
@Injectable({ providedIn: 'root' })
export class DocenteNotasService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);
  private readonly demo = new Map<number, CursoNotas>();
  private siguienteIdDemo = -1;

  getCursoNotas(asignaturaId: number): Observable<CursoNotas> {
    if (this.config.useMocks) {
      return of(this.demoCurso(asignaturaId));
    }
    return this.http.get<CursoNotas>(
      `${this.config.bffBaseUrl}/bff/v1/docentes/cursos/${asignaturaId}/notas`,
    );
  }

  crearNota(idEstudiante: number, idEvaluacion: number, score: number): Observable<NotaCurso> {
    if (this.config.useMocks) {
      const nota: NotaCurso = { id: this.siguienteIdDemo--, idEvaluacion, score };
      this.alumnoDemo(idEstudiante)?.notas.push(nota);
      return of(nota);
    }
    return this.http.post<NotaCurso>(`${this.config.bffBaseUrl}/bff/v1/notas`, {
      idEstudiante,
      idEvaluacion,
      score,
    });
  }

  editarNota(id: number, score: number): Observable<NotaCurso> {
    if (this.config.useMocks) {
      const nota = this.buscarNotaDemo(id);
      if (nota) {
        nota.score = score;
      }
      return of(nota ?? { id, idEvaluacion: 0, score });
    }
    return this.http.put<NotaCurso>(`${this.config.bffBaseUrl}/bff/v1/notas/${id}`, { score });
  }

  eliminarNota(id: number): Observable<void> {
    if (this.config.useMocks) {
      for (const curso of this.demo.values()) {
        for (const alumno of curso.alumnos) {
          alumno.notas = alumno.notas.filter((nota) => nota.id !== id);
        }
      }
      return of(void 0);
    }
    return this.http.delete<void>(`${this.config.bffBaseUrl}/bff/v1/notas/${id}`);
  }

  crearEvaluacion(asignaturaId: number, request: EvaluacionRequest): Observable<EvaluacionNotas> {
    if (this.config.useMocks) {
      const evaluacion: EvaluacionNotas = { id: this.siguienteIdDemo--, ...request };
      this.demoCurso(asignaturaId).evaluaciones.push(evaluacion);
      return of(evaluacion);
    }
    return this.http.post<EvaluacionNotas>(`${this.config.bffBaseUrl}/bff/v1/evaluaciones`, {
      ...request,
      idAsignatura: asignaturaId,
    });
  }

  editarEvaluacion(id: number, request: EvaluacionRequest): Observable<EvaluacionNotas> {
    if (this.config.useMocks) {
      const evaluacion = this.buscarEvaluacionDemo(id);
      if (evaluacion) {
        Object.assign(evaluacion, request);
      }
      return of(evaluacion ?? { id, ...request });
    }
    return this.http.put<EvaluacionNotas>(
      `${this.config.bffBaseUrl}/bff/v1/evaluaciones/${id}`,
      request,
    );
  }

  eliminarEvaluacion(id: number): Observable<void> {
    if (this.config.useMocks) {
      for (const curso of this.demo.values()) {
        curso.evaluaciones = curso.evaluaciones.filter((evaluacion) => evaluacion.id !== id);
        for (const alumno of curso.alumnos) {
          alumno.notas = alumno.notas.filter((nota) => nota.idEvaluacion !== id);
        }
      }
      return of(void 0);
    }
    return this.http.delete<void>(`${this.config.bffBaseUrl}/bff/v1/evaluaciones/${id}`);
  }

  // --- Modo demo ---

  private demoCurso(asignaturaId: number): CursoNotas {
    const existente = this.demo.get(asignaturaId);
    if (existente) {
      return existente;
    }
    const base: CursoDocente | undefined = cursosDelDocente(DOCENTE_ACTUAL_ID).find(
      (curso) => curso.id === asignaturaId,
    );
    const curso: CursoNotas = {
      asignaturaId,
      curso: base?.nombre ?? `Curso ${asignaturaId}`,
      asignatura: base?.asignatura ?? CURSOS_DOCENTE_MOCK[0].asignatura,
      evaluaciones: NOMBRES.map((nombre, indice) => ({
        id: this.siguienteIdDemo--,
        nombre,
        tipo: TIPOS[indice],
        ponderacion: PONDERACIONES[indice],
      })),
      alumnos: (base?.alumnos ?? []).map((alumno) => ({
        id: alumno.id,
        nombres: alumno.nombres,
        firstSurname: alumno.firstSurname,
        secondSurname: alumno.secondSurname,
        notas: [],
      })),
    };
    curso.alumnos.forEach((alumno, indice) => {
      alumno.notas = curso.evaluaciones.map((evaluacion, evalIndice) => ({
        id: this.siguienteIdDemo--,
        idEvaluacion: evaluacion.id,
        score: Math.round((5 + ((indice + evalIndice) % 15) / 10) * 10) / 10,
      }));
    });
    this.demo.set(asignaturaId, curso);
    return curso;
  }

  private alumnoDemo(idEstudiante: number): AlumnoNotas | undefined {
    for (const curso of this.demo.values()) {
      const alumno = curso.alumnos.find((item) => item.id === idEstudiante);
      if (alumno) {
        return alumno;
      }
    }
    return undefined;
  }

  private buscarNotaDemo(id: number): NotaCurso | undefined {
    for (const curso of this.demo.values()) {
      for (const alumno of curso.alumnos) {
        const nota = alumno.notas.find((item) => item.id === id);
        if (nota) {
          return nota;
        }
      }
    }
    return undefined;
  }

  private buscarEvaluacionDemo(id: number): EvaluacionNotas | undefined {
    for (const curso of this.demo.values()) {
      const evaluacion = curso.evaluaciones.find((item) => item.id === id);
      if (evaluacion) {
        return evaluacion;
      }
    }
    return undefined;
  }
}
