export interface Curso {
  id: number;
  nombre: string;
  asignatura: string;
  profesor: string;
  alumnos: number;
}

export interface Alumno {
  id: number;
  nombre: string;
}

/** Datos de ejemplo mientras no exista el backend de cursos/notas/asistencias. */
export const CURSOS_MOCK: Curso[] = [
  { id: 1, nombre: '8° Básico A', asignatura: 'Ciencias Naturales', profesor: 'Alejandro Javier Silva Morales', alumnos: 32 },
  { id: 2, nombre: '8° Básico B', asignatura: 'Ciencias Naturales', profesor: 'Alejandro Javier Silva Morales', alumnos: 30 },
  { id: 3, nombre: '8° Básico A', asignatura: 'Matemáticas', profesor: 'Camila Antonia Castro Medina', alumnos: 32 },
  { id: 4, nombre: '8° Básico A', asignatura: 'Lenguaje', profesor: 'Romina Belén Cárdenas Pizarro', alumnos: 32 },
  { id: 5, nombre: '8° Básico A', asignatura: 'Historia', profesor: 'Augusto Andrés Figueroa Ríos', alumnos: 32 },
  { id: 6, nombre: '8° Básico A', asignatura: 'Tecnología', profesor: 'Francisco José Toledo Olivares', alumnos: 32 },
  { id: 7, nombre: '8° Básico A', asignatura: 'Música', profesor: 'Gabriel Antonio Miranda Lagos', alumnos: 32 },
  { id: 8, nombre: '8° Básico A', asignatura: 'Inglés', profesor: 'Valeria Paz Contreras Navarro', alumnos: 32 },
  { id: 9, nombre: '8° Básico A', asignatura: 'Orientación', profesor: 'Valentina Isabel Alarcón Bustos', alumnos: 32 },
  { id: 10, nombre: '8° Básico A', asignatura: 'Artes Visuales', profesor: 'Paula Andrea Salazar Muñoz', alumnos: 32 },
  { id: 11, nombre: '8° Básico A', asignatura: 'Educación Física', profesor: 'Carlos Alberto Mendoza Fuentes', alumnos: 32 },
  { id: 12, nombre: '8° Básico A', asignatura: 'Educación Financiera', profesor: 'Claudia Marcela Espinoza Cortez', alumnos: 32 },
];

export const ALUMNOS_MOCK: Alumno[] = [
  { id: 1, nombre: 'Camila Antonieta Soto Hernández' },
  { id: 2, nombre: 'Matías Ignacio Soto Hernández' },
  { id: 3, nombre: 'Fernanda Ignacia Rojas Peña' },
  { id: 4, nombre: 'Diego Alonso Muñoz Castro' },
  { id: 5, nombre: 'Antonia Belén Vergara Díaz' },
];
