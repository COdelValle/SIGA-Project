/** Contratos del perfil que expone el BFF (`/api/bff/v1/estudiantes/perfil`). */

export interface PerfilClaseDTO {
  id: number;
  nivel: string;
  letra: string;
  anioAcademico: number;
}

export interface PerfilHorarioDTO {
  id: number;
  dia: string;
  horarioEntrada: string;
  horarioSalida: string;
  ubicacion: string;
}

export interface PerfilEvaluacionDTO {
  id: number;
  nombre: string;
  tipo: string;
  ponderacion: number;
  nota: number | null;
}

export interface PerfilAsignaturaDTO {
  id: number;
  name: string;
  description: string;
  idDocente: number;
  docente: string | null;
  horarios: PerfilHorarioDTO[];
  evaluaciones: PerfilEvaluacionDTO[];
}

export type EstadoAsistenciaDTO = 'PRESENTE' | 'AUSENTE' | 'ATRASADO';
export type JustificacionAsistenciaDTO = 'SI' | 'NO' | 'PENDIENTE' | 'NO_APLICA';

export interface AsistenciaDTO {
  id: number;
  idEstudiante: number;
  idAsignatura: number;
  fecha: string;
  estado: EstadoAsistenciaDTO;
  justificacion: JustificacionAsistenciaDTO;
  observacion: string | null;
}

export interface PerfilEstudianteDTO {
  id: number;
  idUsuario: string;
  rut: string;
  firstName: string;
  middleName: string | null;
  firstSurname: string;
  secondSurname: string | null;
  birthDate: string;
  allergies: string[];
  state: string;
  idClase: number | null;
  clase: PerfilClaseDTO | null;
  asignaturas: PerfilAsignaturaDTO[];
}
