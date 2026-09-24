import {
  AsistenciaRegistro,
  AsistenciaResumen,
  ConfiguracionAcademica,
  DiaSemana,
  HorarioBloque,
  NotaDetalle,
  NotaItem,
  PeriodoAcademico,
  SemestreNotas,
  promedioAsignaturaPeriodo,
  promedioAsignaturas,
  promedioFinal,
  promedioNotas,
  redondear1,
  resumirAsistencia,
} from '../models/academico.model';

/**
 * Datos de ejemplo (mock) para las vistas academicas mientras no exista el
 * backend de horarios/asistencias/notas. Reemplazar por llamadas al BFF.
 * Todo esta indexado por id de estudiante (1 = Camila, 2 = Lilith).
 */

export interface EstudianteMock {
  id: number;
  nombre: string;
  curso: string;
}

export const ESTUDIANTES_MOCK: EstudianteMock[] = [
  { id: 1, nombre: 'Camila Antonieta Soto Hernández', curso: '8° Básico A' },
  { id: 2, nombre: 'Lilith Fernanda Soto Hernández', curso: '4° Básico B' },
];

export const ESTUDIANTE_ACTUAL_ID = 1;

interface AsignaturaBase {
  id: number;
  nombre: string;
  docente: string;
}

const ASIGNATURAS_CAMILA: AsignaturaBase[] = [
  { id: 1, nombre: 'Historia', docente: 'Augusto Andrés Figueroa Ríos' },
  { id: 2, nombre: 'Lenguaje', docente: 'Romina Belén Cárdenas Pizarro' },
  { id: 3, nombre: 'Matemáticas', docente: 'Camila Antonia Castro Medina' },
  { id: 4, nombre: 'Ciencias Naturales', docente: 'Alejandro Javier Silva Morales' },
  { id: 5, nombre: 'Inglés', docente: 'Valeria Paz Contreras Navarro' },
  { id: 6, nombre: 'Educación Física', docente: 'Carlos Alberto Mendoza Fuentes' },
  { id: 7, nombre: 'Artes Visuales', docente: 'Paula Andrea Salazar Muñoz' },
  { id: 8, nombre: 'Tecnología', docente: 'Francisco José Toledo Olivares' },
  { id: 9, nombre: 'Música', docente: 'Gabriel Antonio Miranda Lagos' },
  { id: 10, nombre: 'Orientación', docente: 'Valentina Isabel Alarcón Bustos' },
  { id: 11, nombre: 'Educación Financiera', docente: 'Claudia Marcela Espinoza Cortez' },
];

const ASIGNATURAS_LILITH: AsignaturaBase[] = [
  { id: 1, nombre: 'Historia', docente: 'Daniela Ignacia Paredes Rojas' },
  { id: 2, nombre: 'Lenguaje', docente: 'Carolina Paz Vega Fuentes' },
  { id: 3, nombre: 'Matemáticas', docente: 'Marcela Soledad Guzmán Rivas' },
  { id: 4, nombre: 'Ciencias Naturales', docente: 'Rodrigo Andrés Salinas Torres' },
  { id: 5, nombre: 'Inglés', docente: 'Javiera Paz Molina Sepúlveda' },
  { id: 6, nombre: 'Educación Física', docente: 'Héctor Manuel Bravo Cárdenas' },
  { id: 7, nombre: 'Artes Visuales', docente: 'Ignacio Tomás Herrera Soto' },
  { id: 8, nombre: 'Tecnología', docente: 'Sebastián Andrés Cáceres Núñez' },
  { id: 9, nombre: 'Música', docente: 'Constanza Belén Reyes Fuentes' },
  { id: 10, nombre: 'Orientación', docente: 'Patricia Elena Orellana Díaz' },
  { id: 11, nombre: 'Religión', docente: 'Mónica Alejandra Leiva Campos' },
];

export const ASIGNATURAS_POR_ESTUDIANTE: Record<number, AsignaturaBase[]> = {
  1: ASIGNATURAS_CAMILA,
  2: ASIGNATURAS_LILITH,
};

export const CONFIG_ACADEMICA_MOCK: ConfiguracionAcademica = {
  modoCalculo: 'PONDERADO',
  pesoSemestres: { s1: 50, s2: 50 },
};

const HORAS: [string, string][] = [
  ['08:00', '08:45'],
  ['08:45', '09:30'],
  ['09:50', '10:35'],
  ['10:35', '11:20'],
  ['12:15', '13:00'],
  ['13:00', '13:45'],
  ['13:55', '14:40'],
  ['14:40', '15:25'],
];

function bloques(
  asignatura: string,
  profesor: string,
  sala: string,
  franja: number,
): HorarioBloque[] {
  const inicio = (franja - 1) * 2;
  return [
    { hora: `${HORAS[inicio][0]} - ${HORAS[inicio][1]}`, asignatura, profesor, sala },
    { hora: `${HORAS[inicio + 1][0]} - ${HORAS[inicio + 1][1]}`, asignatura, profesor, sala },
  ];
}

const SALA_CAMILA = 'Sala 8° Básico A';
const SALA_LILITH = 'Sala 4° Básico B';

const HORARIO_CAMILA: Record<DiaSemana, HorarioBloque[]> = {
  Lunes: [
    ...bloques('Ciencias Naturales', 'Alejandro Javier Silva Morales', SALA_CAMILA, 1),
    ...bloques('Lenguaje', 'Romina Belén Cárdenas Pizarro', SALA_CAMILA, 2),
    ...bloques('Historia', 'Augusto Andrés Figueroa Ríos', SALA_CAMILA, 3),
    ...bloques('Educación Física', 'Carlos Alberto Mendoza Fuentes', 'Cancha Techada 1', 4),
  ],
  Martes: [
    ...bloques('Tecnología', 'Francisco José Toledo Olivares', SALA_CAMILA, 1),
    ...bloques('Matemáticas', 'Camila Antonia Castro Medina', SALA_CAMILA, 2),
    ...bloques('Música', 'Gabriel Antonio Miranda Lagos', SALA_CAMILA, 3),
    ...bloques('Inglés', 'Valeria Paz Contreras Navarro', SALA_CAMILA, 4),
  ],
  Miércoles: [
    ...bloques('Historia', 'Augusto Andrés Figueroa Ríos', SALA_CAMILA, 1),
    ...bloques('Ciencias Naturales', 'Alejandro Javier Silva Morales', SALA_CAMILA, 2),
    ...bloques('Lenguaje', 'Romina Belén Cárdenas Pizarro', SALA_CAMILA, 3),
    ...bloques('Orientación', 'Valentina Isabel Alarcón Bustos', SALA_CAMILA, 4),
  ],
  Jueves: [
    ...bloques('Artes Visuales', 'Paula Andrea Salazar Muñoz', SALA_CAMILA, 1),
    ...bloques('Inglés', 'Valeria Paz Contreras Navarro', SALA_CAMILA, 2),
    ...bloques('Matemáticas', 'Camila Antonia Castro Medina', SALA_CAMILA, 3),
    ...bloques('Educación Física', 'Carlos Alberto Mendoza Fuentes', 'Cancha Techada 1', 4),
  ],
  Viernes: [
    ...bloques('Lenguaje', 'Romina Belén Cárdenas Pizarro', SALA_CAMILA, 1),
    ...bloques('Historia', 'Augusto Andrés Figueroa Ríos', SALA_CAMILA, 2),
    ...bloques('Ciencias Naturales', 'Alejandro Javier Silva Morales', SALA_CAMILA, 3),
    ...bloques('Educación Financiera', 'Claudia Marcela Espinoza Cortez', SALA_CAMILA, 4),
  ],
};

const HORARIO_LILITH: Record<DiaSemana, HorarioBloque[]> = {
  Lunes: [
    ...bloques('Lenguaje', 'Carolina Paz Vega Fuentes', SALA_LILITH, 1),
    ...bloques('Música', 'Constanza Belén Reyes Fuentes', SALA_LILITH, 2),
    ...bloques('Matemáticas', 'Marcela Soledad Guzmán Rivas', SALA_LILITH, 3),
    ...bloques('Orientación', 'Patricia Elena Orellana Díaz', SALA_LILITH, 4),
  ],
  Martes: [
    ...bloques('Ciencias Naturales', 'Rodrigo Andrés Salinas Torres', SALA_LILITH, 1),
    ...bloques('Historia', 'Daniela Ignacia Paredes Rojas', SALA_LILITH, 2),
    ...bloques('Educación Física', 'Héctor Manuel Bravo Cárdenas', 'Patio Cubierto 2', 3),
    ...bloques('Lenguaje', 'Carolina Paz Vega Fuentes', SALA_LILITH, 4),
  ],
  Miércoles: [
    ...bloques('Religión', 'Mónica Alejandra Leiva Campos', SALA_LILITH, 1),
    ...bloques('Inglés', 'Javiera Paz Molina Sepúlveda', SALA_LILITH, 2),
    ...bloques('Ciencias Naturales', 'Rodrigo Andrés Salinas Torres', SALA_LILITH, 3),
    ...bloques('Artes Visuales', 'Ignacio Tomás Herrera Soto', SALA_LILITH, 4),
  ],
  Jueves: [
    ...bloques('Tecnología', 'Sebastián Andrés Cáceres Núñez', SALA_LILITH, 1),
    ...bloques('Historia', 'Daniela Ignacia Paredes Rojas', SALA_LILITH, 2),
    ...bloques('Lenguaje', 'Carolina Paz Vega Fuentes', SALA_LILITH, 3),
    ...bloques('Ciencias Naturales', 'Rodrigo Andrés Salinas Torres', SALA_LILITH, 4),
  ],
  Viernes: [
    ...bloques('Matemáticas', 'Marcela Soledad Guzmán Rivas', SALA_LILITH, 1),
    ...bloques('Inglés', 'Javiera Paz Molina Sepúlveda', SALA_LILITH, 2),
    ...bloques('Historia', 'Daniela Ignacia Paredes Rojas', SALA_LILITH, 3),
    ...bloques('Educación Física', 'Héctor Manuel Bravo Cárdenas', 'Patio Cubierto 2', 4),
  ],
};

export const HORARIO_MOCK: Record<number, Record<DiaSemana, HorarioBloque[]>> = {
  1: HORARIO_CAMILA,
  2: HORARIO_LILITH,
};

const DIA_JS: Record<DiaSemana, number> = {
  Lunes: 1,
  Martes: 2,
  Miércoles: 3,
  Jueves: 4,
  Viernes: 5,
};

interface PlanAsistencia {
  id: number;
  clasesRegistradas: number;
  clasesAsistidas: number;
}

export interface PeriodoAsistencia {
  anio: number;
  resumen: AsistenciaResumen[];
  registros: Record<number, AsistenciaRegistro[]>;
  asistenciaGeneral: number;
}

const ASISTENCIA_CAMILA_2026: PlanAsistencia[] = [
  { id: 1, clasesRegistradas: 60, clasesAsistidas: 51 },
  { id: 2, clasesRegistradas: 60, clasesAsistidas: 54 },
  { id: 3, clasesRegistradas: 50, clasesAsistidas: 47 },
  { id: 4, clasesRegistradas: 60, clasesAsistidas: 58 },
  { id: 5, clasesRegistradas: 50, clasesAsistidas: 43 },
  { id: 6, clasesRegistradas: 40, clasesAsistidas: 36 },
  { id: 7, clasesRegistradas: 30, clasesAsistidas: 24 },
  { id: 8, clasesRegistradas: 20, clasesAsistidas: 18 },
  { id: 9, clasesRegistradas: 25, clasesAsistidas: 21 },
  { id: 10, clasesRegistradas: 30, clasesAsistidas: 28 },
  { id: 11, clasesRegistradas: 20, clasesAsistidas: 19 },
];

const ASISTENCIA_CAMILA_2025: PlanAsistencia[] = [
  { id: 1, clasesRegistradas: 60, clasesAsistidas: 33 },
  { id: 2, clasesRegistradas: 60, clasesAsistidas: 36 },
  { id: 3, clasesRegistradas: 50, clasesAsistidas: 26 },
  { id: 4, clasesRegistradas: 60, clasesAsistidas: 30 },
  { id: 5, clasesRegistradas: 50, clasesAsistidas: 28 },
  { id: 6, clasesRegistradas: 40, clasesAsistidas: 30 },
  { id: 7, clasesRegistradas: 30, clasesAsistidas: 12 },
  { id: 8, clasesRegistradas: 20, clasesAsistidas: 12 },
  { id: 9, clasesRegistradas: 25, clasesAsistidas: 13 },
  { id: 10, clasesRegistradas: 30, clasesAsistidas: 20 },
  { id: 11, clasesRegistradas: 20, clasesAsistidas: 10 },
];

const ASISTENCIA_LILITH_2026: PlanAsistencia[] = [
  { id: 1, clasesRegistradas: 60, clasesAsistidas: 52 },
  { id: 2, clasesRegistradas: 60, clasesAsistidas: 57 },
  { id: 3, clasesRegistradas: 50, clasesAsistidas: 39 },
  { id: 4, clasesRegistradas: 60, clasesAsistidas: 55 },
  { id: 5, clasesRegistradas: 50, clasesAsistidas: 44 },
  { id: 6, clasesRegistradas: 40, clasesAsistidas: 31 },
  { id: 7, clasesRegistradas: 30, clasesAsistidas: 26 },
  { id: 8, clasesRegistradas: 20, clasesAsistidas: 15 },
  { id: 9, clasesRegistradas: 25, clasesAsistidas: 23 },
  { id: 10, clasesRegistradas: 30, clasesAsistidas: 22 },
  { id: 11, clasesRegistradas: 20, clasesAsistidas: 17 },
];

const ASISTENCIA_LILITH_2025: PlanAsistencia[] = [
  { id: 1, clasesRegistradas: 60, clasesAsistidas: 54 },
  { id: 2, clasesRegistradas: 60, clasesAsistidas: 55 },
  { id: 3, clasesRegistradas: 50, clasesAsistidas: 41 },
  { id: 4, clasesRegistradas: 60, clasesAsistidas: 52 },
  { id: 5, clasesRegistradas: 50, clasesAsistidas: 42 },
  { id: 6, clasesRegistradas: 40, clasesAsistidas: 33 },
  { id: 7, clasesRegistradas: 30, clasesAsistidas: 25 },
  { id: 8, clasesRegistradas: 20, clasesAsistidas: 16 },
  { id: 9, clasesRegistradas: 25, clasesAsistidas: 22 },
  { id: 10, clasesRegistradas: 30, clasesAsistidas: 24 },
  { id: 11, clasesRegistradas: 20, clasesAsistidas: 16 },
];

function diasClaseDe(
  horario: Record<DiaSemana, HorarioBloque[]>,
  asignatura: string,
): number[] {
  const dias = new Set<number>();
  (Object.keys(horario) as DiaSemana[]).forEach((dia) => {
    if (horario[dia].some((bloque) => bloque.asignatura === asignatura)) {
      dias.add(DIA_JS[dia]);
    }
  });
  return [...dias];
}

function isoDe(fecha: Date): string {
  return fecha.toISOString().slice(0, 10);
}

function generarRegistros(
  horario: Record<DiaSemana, HorarioBloque[]>,
  asignatura: string,
  plan: PlanAsistencia,
  fechaReferencia: Date,
): AsistenciaRegistro[] {
  const diasClase = diasClaseDe(horario, asignatura);
  if (diasClase.length === 0) {
    return [];
  }

  const fechas: Date[] = [];
  const cursor = new Date(fechaReferencia);

  while (fechas.length < plan.clasesRegistradas) {
    if (diasClase.includes(cursor.getUTCDay())) {
      fechas.push(new Date(cursor));
    }
    cursor.setUTCDate(cursor.getUTCDate() - 1);
  }
  fechas.reverse();

  const ausencias = plan.clasesRegistradas - plan.clasesAsistidas;
  const indicesAusentes = new Set<number>();
  for (let i = 1; i <= ausencias; i += 1) {
    indicesAusentes.add(
      Math.min(
        plan.clasesRegistradas - 1,
        Math.floor((i * plan.clasesRegistradas) / (ausencias + 1)),
      ),
    );
  }

  let ausenciaN = 0;
  return fechas.map((fecha, index) => {
    if (indicesAusentes.has(index)) {
      const justificado = ausenciaN % 3 === 0 ? 'Sí' : 'No';
      ausenciaN += 1;
      return { fecha: isoDe(fecha), tipo: 'Inasistencia', justificado };
    }
    return { fecha: isoDe(fecha), tipo: 'Presente', justificado: 'No aplica' };
  });
}

function construirPeriodoAsistencia(
  estudianteId: number,
  anio: number,
  plan: PlanAsistencia[],
  fechaReferencia: Date,
): PeriodoAsistencia {
  const asignaturas = ASIGNATURAS_POR_ESTUDIANTE[estudianteId] ?? [];
  const horario = HORARIO_MOCK[estudianteId] ?? HORARIO_CAMILA;
  const registros: Record<number, AsistenciaRegistro[]> = {};
  const resumen: AsistenciaResumen[] = [];

  plan.forEach((item) => {
    const asignatura = asignaturas.find((base) => base.id === item.id);
    if (!asignatura) {
      return;
    }
    const registrosAsignatura = generarRegistros(horario, asignatura.nombre, item, fechaReferencia);
    registros[item.id] = registrosAsignatura;
    resumen.push(resumirAsistencia(item.id, asignatura.nombre, registrosAsignatura));
  });

  const totalRegistradas = resumen.reduce((suma, item) => suma + item.clasesRegistradas, 0);
  const totalAsistidas = resumen.reduce((suma, item) => suma + item.clasesAsistidas, 0);
  const asistenciaGeneral =
    totalRegistradas === 0 ? 0 : Math.round((totalAsistidas / totalRegistradas) * 100);

  return { anio, resumen, registros, asistenciaGeneral };
}

export const ASISTENCIA_PERIODOS_MOCK: Record<number, PeriodoAsistencia[]> = {
  1: [
    construirPeriodoAsistencia(1, 2026, ASISTENCIA_CAMILA_2026, new Date(Date.UTC(2026, 8, 30))),
    construirPeriodoAsistencia(1, 2025, ASISTENCIA_CAMILA_2025, new Date(Date.UTC(2025, 11, 15))),
  ],
  2: [
    construirPeriodoAsistencia(2, 2026, ASISTENCIA_LILITH_2026, new Date(Date.UTC(2026, 8, 30))),
    construirPeriodoAsistencia(2, 2025, ASISTENCIA_LILITH_2025, new Date(Date.UTC(2025, 11, 15))),
  ],
};

interface PlanNotas {
  id: number;
  promedio: number;
  s1: number;
  s2: number;
}

const NOTAS_CAMILA_2026: PlanNotas[] = [
  { id: 1, promedio: 5.6, s1: 4, s2: 3 },
  { id: 2, promedio: 5.8, s1: 5, s2: 4 },
  { id: 3, promedio: 6.8, s1: 6, s2: 4 },
  { id: 4, promedio: 7.0, s1: 6, s2: 5 },
  { id: 5, promedio: 5.7, s1: 5, s2: 3 },
  { id: 6, promedio: 5.8, s1: 3, s2: 2 },
  { id: 7, promedio: 5.2, s1: 4, s2: 3 },
  { id: 8, promedio: 5.9, s1: 3, s2: 3 },
  { id: 9, promedio: 5.4, s1: 3, s2: 2 },
  { id: 10, promedio: 6.2, s1: 3, s2: 2 },
  { id: 11, promedio: 6.7, s1: 4, s2: 3 },
];

const NOTAS_CAMILA_2025: PlanNotas[] = [
  { id: 1, promedio: 3.8, s1: 4, s2: 3 },
  { id: 2, promedio: 3.5, s1: 5, s2: 4 },
  { id: 3, promedio: 3.8, s1: 6, s2: 4 },
  { id: 4, promedio: 3.9, s1: 6, s2: 5 },
  { id: 5, promedio: 3.6, s1: 5, s2: 3 },
  { id: 6, promedio: 4.2, s1: 3, s2: 2 },
  { id: 7, promedio: 3.2, s1: 4, s2: 3 },
  { id: 8, promedio: 4.0, s1: 3, s2: 3 },
  { id: 9, promedio: 3.7, s1: 3, s2: 2 },
  { id: 10, promedio: 4.1, s1: 3, s2: 2 },
  { id: 11, promedio: 3.9, s1: 4, s2: 3 },
];

const NOTAS_LILITH_2026: PlanNotas[] = [
  { id: 1, promedio: 5.5, s1: 4, s2: 3 },
  { id: 2, promedio: 5.5, s1: 5, s2: 4 },
  { id: 3, promedio: 4.9, s1: 5, s2: 4 },
  { id: 4, promedio: 5.3, s1: 5, s2: 4 },
  { id: 5, promedio: 5.2, s1: 4, s2: 3 },
  { id: 6, promedio: 5.9, s1: 3, s2: 2 },
  { id: 7, promedio: 6.5, s1: 5, s2: 4 },
  { id: 8, promedio: 5.3, s1: 3, s2: 3 },
  { id: 9, promedio: 6.4, s1: 4, s2: 3 },
  { id: 10, promedio: 5.5, s1: 3, s2: 2 },
  { id: 11, promedio: 5.6, s1: 3, s2: 2 },
];

const NOTAS_LILITH_2025: PlanNotas[] = [
  { id: 1, promedio: 5.2, s1: 4, s2: 3 },
  { id: 2, promedio: 5.2, s1: 5, s2: 4 },
  { id: 3, promedio: 4.8, s1: 5, s2: 4 },
  { id: 4, promedio: 5.0, s1: 5, s2: 4 },
  { id: 5, promedio: 4.9, s1: 4, s2: 3 },
  { id: 6, promedio: 5.6, s1: 3, s2: 2 },
  { id: 7, promedio: 6.2, s1: 5, s2: 4 },
  { id: 8, promedio: 5.0, s1: 3, s2: 3 },
  { id: 9, promedio: 6.1, s1: 4, s2: 3 },
  { id: 10, promedio: 5.2, s1: 3, s2: 2 },
  { id: 11, promedio: 5.3, s1: 3, s2: 2 },
];

const PATRONES = [-0.4, 0.3, -0.2, 0.5, -0.6, 0.2, 0.4, -0.3];
const PESOS = [20, 30, 50];

function clamp(valor: number, minimo: number, maximo: number): number {
  return Math.min(maximo, Math.max(minimo, valor));
}

function generarNotas(promedio: number, cantidad: number, semilla: number): NotaDetalle[] {
  const notas: NotaDetalle[] = [];
  let sumaPesos = 0;
  let sumaPonderada = 0;

  for (let i = 0; i < cantidad; i += 1) {
    const ponderacion = PESOS[i % PESOS.length];
    const offset = PATRONES[(i + semilla) % PATRONES.length];
    const valor = redondear1(clamp(promedio + offset, 1, 7));
    notas.push({ numero: i + 1, valor, ponderacion });
    sumaPesos += ponderacion;
    sumaPonderada += valor * ponderacion;
  }

  const ultima = notas[notas.length - 1];
  if (ultima && ultima.ponderacion > 0) {
    const delta = promedio * sumaPesos - sumaPonderada;
    ultima.valor = redondear1(clamp(ultima.valor + delta / ultima.ponderacion, 1, 7));
  }

  return notas;
}

function construirSemestre(
  estudianteId: number,
  numero: 1 | 2,
  plan: PlanNotas[],
  config: ConfiguracionAcademica,
): SemestreNotas {
  const asignaturasBase = ASIGNATURAS_POR_ESTUDIANTE[estudianteId] ?? [];
  const asignaturas = plan.map((item, index) => {
    const base = asignaturasBase.find((asignatura) => asignatura.id === item.id);
    const cantidad = numero === 1 ? item.s1 : item.s2;
    const notas = generarNotas(item.promedio, cantidad, index + numero + estudianteId);
    return {
      id: item.id,
      asignatura: base?.nombre ?? 'Asignatura',
      docente: base?.docente ?? '',
      notas,
      promedio: promedioNotas(notas, config.modoCalculo),
    };
  });

  return { numero, asignaturas, promedio: promedioAsignaturas(asignaturas) };
}

function construirPeriodoAcademico(
  estudianteId: number,
  anio: number,
  curso: string,
  estado: PeriodoAcademico['estado'],
  plan: PlanNotas[],
  config: ConfiguracionAcademica,
): PeriodoAcademico {
  const semestres = [
    construirSemestre(estudianteId, 1, plan, config),
    construirSemestre(estudianteId, 2, plan, config),
  ];
  const asistencia = ASISTENCIA_PERIODOS_MOCK[estudianteId]?.find(
    (periodo) => periodo.anio === anio,
  );

  return {
    anio,
    curso,
    estado,
    semestres,
    promedioFinal: promedioFinal(semestres[0].promedio, semestres[1].promedio, config.pesoSemestres),
    asistenciaGeneral: asistencia?.asistenciaGeneral ?? 0,
  };
}

export const HISTORIAL_NOTAS_MOCK: Record<number, PeriodoAcademico[]> = {
  1: [
    construirPeriodoAcademico(1, 2026, '8° Básico A', 'EN_CURSO', NOTAS_CAMILA_2026, CONFIG_ACADEMICA_MOCK),
    construirPeriodoAcademico(1, 2025, '8° Básico A', 'FINALIZADO', NOTAS_CAMILA_2025, CONFIG_ACADEMICA_MOCK),
  ],
  2: [
    construirPeriodoAcademico(2, 2026, '4° Básico B', 'EN_CURSO', NOTAS_LILITH_2026, CONFIG_ACADEMICA_MOCK),
    construirPeriodoAcademico(2, 2025, '3° Básico B', 'FINALIZADO', NOTAS_LILITH_2025, CONFIG_ACADEMICA_MOCK),
  ],
};

export function horarioDe(estudianteId: number): Record<DiaSemana, HorarioBloque[]> {
  return HORARIO_MOCK[estudianteId] ?? HORARIO_MOCK[ESTUDIANTE_ACTUAL_ID];
}

export function asistenciaDe(estudianteId: number): PeriodoAsistencia[] {
  return ASISTENCIA_PERIODOS_MOCK[estudianteId] ?? ASISTENCIA_PERIODOS_MOCK[ESTUDIANTE_ACTUAL_ID] ?? [];
}

export function notasDe(estudianteId: number): PeriodoAcademico[] {
  return HISTORIAL_NOTAS_MOCK[estudianteId] ?? HISTORIAL_NOTAS_MOCK[ESTUDIANTE_ACTUAL_ID] ?? [];
}

export function periodoActualDe(estudianteId: number): PeriodoAcademico {
  const periodos = notasDe(estudianteId);
  return periodos.find((periodo) => periodo.estado === 'EN_CURSO') ?? periodos[0];
}

export function notasResumenDe(estudianteId: number): NotaItem[] {
  const asignaturas = ASIGNATURAS_POR_ESTUDIANTE[estudianteId] ?? [];
  const periodo = periodoActualDe(estudianteId);
  return asignaturas.map((asignatura) => ({
    asignatura: asignatura.nombre,
    nota: promedioAsignaturaPeriodo(periodo, asignatura.id, CONFIG_ACADEMICA_MOCK),
  }));
}

export function asistenciaActualDe(estudianteId: number): PeriodoAsistencia {
  const periodos = asistenciaDe(estudianteId);
  return periodos.find((periodo) => periodo.anio === periodoActualDe(estudianteId)?.anio) ?? periodos[0];
}

export function asistenciaResumenDe(estudianteId: number): AsistenciaResumen[] {
  return asistenciaActualDe(estudianteId)?.resumen ?? [];
}

export function asistenciaRegistrosDe(estudianteId: number): Record<number, AsistenciaRegistro[]> {
  return asistenciaActualDe(estudianteId)?.registros ?? {};
}
