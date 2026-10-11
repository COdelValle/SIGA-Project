export type TipoNotificacion = 'EVALUACION' | 'NOTA' | 'ASISTENCIA';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  accion: string;
  titulo: string;
  resumen: string;
  fechaHora: string;
  leida: boolean;
}

/** Contrato mínimo compartido `PageResponseDTO<T>` devuelto por el BFF. */
export interface PaginaNotificaciones {
  content: Notificacion[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface ContadorNotificaciones {
  noLeidas: number;
}
