import { DiaSemana } from '@siga/academico';

/** Datos de ejemplo del dominio docente mientras no exista el backend. */

export interface DocenteMock {
  id: number;
  nombre: string;
  asignatura: string;
  contratoHoras: number;
}

export interface Alumno {
  id: number;
  nombres: string;
  firstSurname: string;
  secondSurname: string;
}

export interface CursoDocente {
  id: number;
  nombre: string;
  nivel: number;
  seccion: string;
  asignatura: string;
  docenteId: number;
  sala: string;
  diasClase: DiaSemana[];
  alumnos: Alumno[];
}

export interface ClaseDocente {
  franja: number;
  cursoId: number;
  curso: string;
  asignatura: string;
  sala: string;
}

export const DOCENTES_MOCK: DocenteMock[] = [
  { id: 1, nombre: 'Alejandro Javier Silva Morales', asignatura: 'Ciencias Naturales', contratoHoras: 40 },
  { id: 2, nombre: 'Natalia Paz Fuentes Cárdenas', asignatura: 'Ciencias Naturales', contratoHoras: 40 },
];

/** Docente autenticado (mock: Alejandro). */
export const DOCENTE_ACTUAL_ID = 1;

export const FRANJAS = [
  { numero: 1, inicio: '08:00', fin: '09:30' },
  { numero: 2, inicio: '09:50', fin: '11:20' },
  { numero: 3, inicio: '12:15', fin: '13:45' },
  { numero: 4, inicio: '13:55', fin: '15:25' },
];

export function horaDeFranja(franja: number): string {
  const encontrada = FRANJAS.find((item) => item.numero === franja);
  return encontrada ? `${encontrada.inicio} - ${encontrada.fin}` : '';
}

const POOL_ALUMNOS: Omit<Alumno, 'id'>[] = [
  { nombres: 'Camila Antonieta', firstSurname: 'Soto', secondSurname: 'Hernández' },
  { nombres: 'Matías Ignacio', firstSurname: 'Rojas', secondSurname: 'Peña' },
  { nombres: 'Fernanda Ignacia', firstSurname: 'Muñoz', secondSurname: 'Castro' },
  { nombres: 'Diego Alonso', firstSurname: 'Vergara', secondSurname: 'Díaz' },
  { nombres: 'Antonia Belén', firstSurname: 'González', secondSurname: 'Fuentes' },
  { nombres: 'Vicente Tomás', firstSurname: 'Pérez', secondSurname: 'Lagos' },
  { nombres: 'Josefa Paz', firstSurname: 'Cárdenas', secondSurname: 'Rojas' },
  { nombres: 'Benjamín Andrés', firstSurname: 'Silva', secondSurname: 'Morales' },
  { nombres: 'Martina Soledad', firstSurname: 'Contreras', secondSurname: 'Navarro' },
  { nombres: 'Lucas Emilio', firstSurname: 'Salazar', secondSurname: 'Muñoz' },
  { nombres: 'Emilia Constanza', firstSurname: 'Espinoza', secondSurname: 'Cortez' },
  { nombres: 'Agustín Nicolás', firstSurname: 'Toledo', secondSurname: 'Olivares' },
  { nombres: 'Isidora Belén', firstSurname: 'Araya', secondSurname: 'Vega' },
  { nombres: 'Tomás Alejandro', firstSurname: 'Fuentes', secondSurname: 'Rivas' },
];

export function nombreCompleto(alumno: Alumno): string {
  return `${alumno.nombres} ${alumno.firstSurname} ${alumno.secondSurname}`.trim();
}

function compararAlumnos(a: Alumno, b: Alumno): number {
  return (
    a.firstSurname.localeCompare(b.firstSurname, 'es') ||
    a.secondSurname.localeCompare(b.secondSurname, 'es') ||
    a.nombres.localeCompare(b.nombres, 'es')
  );
}

function alumnosDeCurso(cursoId: number): Alumno[] {
  const cantidad = 8 + (cursoId % 3);
  const offset = (cursoId - 1) * 3;
  const alumnos: Alumno[] = [];

  for (let i = 0; i < cantidad; i += 1) {
    const base = POOL_ALUMNOS[(offset + i) % POOL_ALUMNOS.length];
    alumnos.push({ id: cursoId * 1000 + i + 1, ...base });
  }

  return alumnos.sort(compararAlumnos);
}

interface CursoBase {
  id: number;
  nivel: number;
  seccion: string;
  docenteId: number;
  diasClase: DiaSemana[];
}

const CURSOS_BASE: CursoBase[] = [
  { id: 1, nivel: 8, seccion: 'A', docenteId: 1, diasClase: ['Lunes', 'Miércoles', 'Viernes'] },
  { id: 2, nivel: 8, seccion: 'B', docenteId: 1, diasClase: ['Lunes', 'Martes', 'Jueves'] },
  { id: 3, nivel: 8, seccion: 'C', docenteId: 1, diasClase: ['Martes', 'Miércoles', 'Viernes'] },
  { id: 4, nivel: 7, seccion: 'A', docenteId: 1, diasClase: ['Lunes', 'Miércoles', 'Jueves'] },
  { id: 5, nivel: 7, seccion: 'B', docenteId: 1, diasClase: ['Martes', 'Jueves', 'Viernes'] },
  { id: 6, nivel: 7, seccion: 'C', docenteId: 1, diasClase: ['Lunes', 'Martes', 'Viernes'] },
  { id: 7, nivel: 6, seccion: 'A', docenteId: 2, diasClase: ['Lunes', 'Miércoles', 'Viernes'] },
  { id: 8, nivel: 6, seccion: 'B', docenteId: 2, diasClase: ['Lunes', 'Martes', 'Jueves'] },
  { id: 9, nivel: 6, seccion: 'C', docenteId: 2, diasClase: ['Martes', 'Miércoles', 'Viernes'] },
  { id: 10, nivel: 5, seccion: 'A', docenteId: 2, diasClase: ['Lunes', 'Miércoles', 'Jueves'] },
  { id: 11, nivel: 5, seccion: 'B', docenteId: 2, diasClase: ['Martes', 'Jueves', 'Viernes'] },
  { id: 12, nivel: 5, seccion: 'C', docenteId: 2, diasClase: ['Lunes', 'Martes', 'Viernes'] },
];

export const CURSOS_DOCENTE_MOCK: CursoDocente[] = CURSOS_BASE.map((base) => ({
  ...base,
  nombre: `${base.nivel}° Básico ${base.seccion}`,
  asignatura: 'Ciencias Naturales',
  sala: `Sala ${base.nivel}° Básico ${base.seccion}`,
  alumnos: alumnosDeCurso(base.id),
}));

export function cursosDelDocente(docenteId: number): CursoDocente[] {
  return CURSOS_DOCENTE_MOCK.filter((curso) => curso.docenteId === docenteId);
}

function construirHorario(docenteId: number): Record<DiaSemana, ClaseDocente[]> {
  const horario: Record<DiaSemana, ClaseDocente[]> = {
    Lunes: [],
    Martes: [],
    Miércoles: [],
    Jueves: [],
    Viernes: [],
  };
  const ocupadas: Record<DiaSemana, Set<number>> = {
    Lunes: new Set(),
    Martes: new Set(),
    Miércoles: new Set(),
    Jueves: new Set(),
    Viernes: new Set(),
  };

  cursosDelDocente(docenteId).forEach((curso) => {
    curso.diasClase.forEach((dia) => {
      let franja = 1;
      while (ocupadas[dia].has(franja)) {
        franja += 1;
      }
      ocupadas[dia].add(franja);
      horario[dia].push({
        franja,
        cursoId: curso.id,
        curso: curso.nombre,
        asignatura: curso.asignatura,
        sala: curso.sala,
      });
    });
  });

  (Object.keys(horario) as DiaSemana[]).forEach((dia) => {
    horario[dia].sort((a, b) => a.franja - b.franja);
  });

  return horario;
}

export const HORARIO_DOCENTE_MOCK: Record<number, Record<DiaSemana, ClaseDocente[]>> = {
  1: construirHorario(1),
  2: construirHorario(2),
};

export function horarioDelDocente(docenteId: number): Record<DiaSemana, ClaseDocente[]> {
  return HORARIO_DOCENTE_MOCK[docenteId] ?? HORARIO_DOCENTE_MOCK[DOCENTE_ACTUAL_ID];
}

export function cursoPorId(cursoId: number): CursoDocente | undefined {
  return CURSOS_DOCENTE_MOCK.find((curso) => curso.id === cursoId);
}

export function horasLectivasDelDocente(docenteId: number): number {
  const franjas = cursosDelDocente(docenteId).reduce(
    (total, curso) => total + curso.diasClase.length,
    0,
  );
  return franjas * 1.5;
}

export function horasNoLectivasDelDocente(docenteId: number): number {
  const docente = DOCENTES_MOCK.find((item) => item.id === docenteId);
  const contrato = docente?.contratoHoras ?? 40;
  return Math.max(0, contrato - horasLectivasDelDocente(docenteId));
}
