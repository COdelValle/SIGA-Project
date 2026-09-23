export type RolAdmin = 'ADMIN' | 'DOCENTE' | 'APODERADO' | 'ESTUDIANTE';
export type EstadoAdmin = 'ACTIVO' | 'INACTIVO';

export interface UsuarioAdmin {
  id: string;
  nombre: string;
  email: string;
  rol: RolAdmin;
  estado: EstadoAdmin;
}

export interface AsignaturaAdmin {
  id: number;
  nombre: string;
  descripcion: string;
  activa: boolean;
}

/** Datos de ejemplo mientras el BFF no exponga la gestion de usuarios. */
export const USUARIOS_MOCK: UsuarioAdmin[] = [
  { id: '3fb9467c', nombre: 'Administrador SIGA', email: 'admin@platformsiga.onmicrosoft.com', rol: 'ADMIN', estado: 'ACTIVO' },
  { id: '2f63f650', nombre: 'Camila Antonieta Soto Hernández', email: 'camila.soto@platformsiga.onmicrosoft.com', rol: 'ESTUDIANTE', estado: 'ACTIVO' },
  { id: '90dca6f5', nombre: 'Claudia Andrea Hernández Morales', email: 'claudia.hernandez@platformsiga.onmicrosoft.com', rol: 'APODERADO', estado: 'ACTIVO' },
  { id: 'ed6ba585', nombre: 'Alejandro Javier Silva Morales', email: 'alejandro.silva@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'd0000001', nombre: 'Romina Belén Cárdenas Pizarro', email: 'romina.cardenas@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'd0000002', nombre: 'Augusto Andrés Figueroa Ríos', email: 'augusto.figueroa@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'd0000003', nombre: 'Carlos Alberto Mendoza Fuentes', email: 'carlos.mendoza@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'd0000004', nombre: 'Francisco José Toledo Olivares', email: 'francisco.toledo@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'd0000005', nombre: 'Camila Antonia Castro Medina', email: 'camila.castro@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'd0000006', nombre: 'Gabriel Antonio Miranda Lagos', email: 'gabriel.miranda@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'd0000007', nombre: 'Valeria Paz Contreras Navarro', email: 'valeria.contreras@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'd0000008', nombre: 'Valentina Isabel Alarcón Bustos', email: 'valentina.alarcon@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'd0000009', nombre: 'Paula Andrea Salazar Muñoz', email: 'paula.salazar@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'd0000010', nombre: 'Claudia Marcela Espinoza Cortez', email: 'claudia.espinoza@platformsiga.onmicrosoft.com', rol: 'DOCENTE', estado: 'ACTIVO' },
];

export const ASIGNATURAS_MOCK: AsignaturaAdmin[] = [
  { id: 1, nombre: 'MATEMATICA', descripcion: 'matematica', activa: true },
  { id: 2, nombre: 'LENGUAJE', descripcion: 'lenguaje y comunicacion', activa: true },
  { id: 3, nombre: 'CIENCIAS', descripcion: 'ciencias naturales', activa: true },
  { id: 4, nombre: 'HISTORIA', descripcion: 'historia y ciencias sociales', activa: true },
  { id: 5, nombre: 'INGLES', descripcion: 'idioma ingles', activa: true },
  { id: 6, nombre: 'EDUCACION FISICA', descripcion: 'educacion fisica', activa: true },
  { id: 7, nombre: 'TECNOLOGIA', descripcion: 'tecnologia', activa: true },
  { id: 8, nombre: 'MUSICA', descripcion: 'musica', activa: true },
  { id: 9, nombre: 'ORIENTACION', descripcion: 'orientacion', activa: true },
  { id: 10, nombre: 'ARTES VISUALES', descripcion: 'artes visuales', activa: true },
  { id: 11, nombre: 'EDUCACION FINANCIERA', descripcion: 'educacion financiera', activa: true },
];

export const ROLES_MOCK = [
  { nombre: 'ADMIN', descripcion: 'Gestion institucional, usuarios y datos generales.' },
  { nombre: 'DOCENTE', descripcion: 'Cursos, evaluaciones, notas y asistencias.' },
  { nombre: 'APODERADO', descripcion: 'Seguimiento academico de sus pupilos.' },
  { nombre: 'ESTUDIANTE', descripcion: 'Notas, horarios y asistencias propias.' },
];
