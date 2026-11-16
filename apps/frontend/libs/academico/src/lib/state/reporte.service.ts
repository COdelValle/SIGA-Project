import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '@siga/core';
import { Observable, of } from 'rxjs';
import {
  EstudianteReporte,
  FiltrosReporte,
  IndicadorAsistencia,
  IndicadorRendimiento,
  ReporteFiltros,
} from '../models/reporte.model';

/**
 * Consume los indicadores de reportes desde el BFF. La SPA nunca se conecta
 * directamente a Kafka ni a ms-reportes; todo pasa por /api/bff/v1/reportes.
 */
@Injectable({ providedIn: 'root' })
export class ReporteService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  getFiltros(): Observable<ReporteFiltros> {
    if (this.config.useMocks) {
      return of({ cursos: [] });
    }
    return this.http.get<ReporteFiltros>(`${this.config.bffBaseUrl}/bff/v1/reportes/filtros`);
  }

  getEstudiantes(idCursoAsignatura: number): Observable<EstudianteReporte[]> {
    if (this.config.useMocks) {
      return of([]);
    }
    return this.http.get<EstudianteReporte[]>(
      `${this.config.bffBaseUrl}/bff/v1/reportes/filtros/estudiantes`,
      { params: new HttpParams().set('idCursoAsignatura', idCursoAsignatura) },
    );
  }

  getAsistencia(filtros: FiltrosReporte): Observable<IndicadorAsistencia[]> {
    if (this.config.useMocks) {
      return of([]);
    }
    return this.http.get<IndicadorAsistencia[]>(
      `${this.config.bffBaseUrl}/bff/v1/reportes/asistencia`,
      { params: this.parametros(filtros) },
    );
  }

  getRendimiento(filtros: FiltrosReporte): Observable<IndicadorRendimiento[]> {
    if (this.config.useMocks) {
      return of([]);
    }
    return this.http.get<IndicadorRendimiento[]>(
      `${this.config.bffBaseUrl}/bff/v1/reportes/rendimiento`,
      { params: this.parametros(filtros) },
    );
  }

  private parametros(filtros: FiltrosReporte): HttpParams {
    let params = new HttpParams();
    if (filtros.idCursoAsignatura != null) {
      params = params.set('idCursoAsignatura', filtros.idCursoAsignatura);
    }
    if (filtros.idEstudiante != null) {
      params = params.set('idEstudiante', filtros.idEstudiante);
    }
    if (filtros.from) {
      params = params.set('from', filtros.from);
    }
    if (filtros.to) {
      params = params.set('to', filtros.to);
    }
    return params;
  }
}
