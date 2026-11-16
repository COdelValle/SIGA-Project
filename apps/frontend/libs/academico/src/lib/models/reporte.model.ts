export type NivelRiesgo = 'NORMAL' | 'PREVENTIVO' | 'CRITICO';
export type EstadoAsistencia = 'PRESENTE' | 'AUSENTE' | 'ATRASADO';

export interface IndicadorAsistencia {
  idEstudiante: number;
  idCursoAsignatura: number;
  fecha: string;
  estado: EstadoAsistencia;
  porcentajeInasistencia: number;
  totalRegistros: number;
  totalInasistencias: number;
  nivelRiesgo: NivelRiesgo;
  actualizadoEn: string;
}

export interface IndicadorRendimiento {
  idEstudiante: number;
  idEvaluacion: number;
  idCursoAsignatura: number;
  puntaje: number | null;
  promedio: number | null;
  evaluacionesInsuficientes: number;
  nivelRiesgo: NivelRiesgo;
  actualizadoEn: string;
}

export interface CursoReporte {
  id: number;
  nombre: string;
  curso: string;
  idClase: number;
}

export interface EstudianteReporte {
  id: number;
  nombre: string;
  rut: string | null;
}

export interface ReporteFiltros {
  cursos: CursoReporte[];
}

export interface FiltrosReporte {
  idCursoAsignatura: number | null;
  idEstudiante: number | null;
  from: string | null;
  to: string | null;
}

export interface ReporteDashboardData {
  asistencia: IndicadorAsistencia[];
  rendimiento: IndicadorRendimiento[];
}
