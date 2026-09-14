export type Rol = 'ADMIN' | 'DOCENTE' | 'APODERADO' | 'ESTUDIANTE';

export const ROL_HOME: Record<Rol, string> = {
  ESTUDIANTE: '/estudiante',
  APODERADO: '/apoderado',
  DOCENTE: '/docente',
  ADMIN: '/admin',
};
