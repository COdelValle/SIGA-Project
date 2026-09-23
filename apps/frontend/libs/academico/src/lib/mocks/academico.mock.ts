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
 */

export const HORARIO_MOCK: Record<DiaSemana, HorarioBloque[]> = {
  Lunes: [
    { hora: '08:00 - 08:45', asignatura: 'Ciencias Naturales', profesor: 'Alejandro Javier Silva Morales', sala: 'Sala 8° Básico A' },
    { hora: '08:45 - 09:30', asignatura: 'Ciencias Naturales', profesor: 'Alejandro Javier Silva Morales', sala: 'Sala 8° Básico A' },
    { hora: '09:50 - 10:35', asignatura: 'Lenguaje', profesor: 'Romina Belén Cárdenas Pizarro', sala: 'Sala 8° Básico A' },
    { hora: '10:35 - 11:20', asignatura: 'Lenguaje', profesor: 'Romina Belén Cárdenas Pizarro', sala: 'Sala 8° Básico A' },
    { hora: '12:15 - 13:00', asignatura: 'Historia', profesor: 'Augusto Andrés Figueroa Ríos', sala: 'Sala 8° Básico A' },
    { hora: '13:00 - 13:45', asignatura: 'Historia', profesor: 'Augusto Andrés Figueroa Ríos', sala: 'Sala 8° Básico A' },
    { hora: '13:55 - 14:40', asignatura: 'Educación Física', profesor: 'Carlos Alberto Mendoza Fuentes', sala: 'Cancha Techada 1' },
    { hora: '14:40 - 15:25', asignatura: 'Educación Física', profesor: 'Carlos Alberto Mendoza Fuentes', sala: 'Cancha Techada 1' },
  ],
  Martes: [
    { hora: '08:00 - 08:45', asignatura: 'Tecnología', profesor: 'Francisco José Toledo Olivares', sala: 'Sala 8° Básico A' },
    { hora: '08:45 - 09:30', asignatura: 'Tecnología', profesor: 'Francisco José Toledo Olivares', sala: 'Sala 8° Básico A' },
    { hora: '09:50 - 10:35', asignatura: 'Matemáticas', profesor: 'Camila Antonia Castro Medina', sala: 'Sala 8° Básico A' },
    { hora: '10:35 - 11:20', asignatura: 'Matemáticas', profesor: 'Camila Antonia Castro Medina', sala: 'Sala 8° Básico A' },
    { hora: '12:15 - 13:00', asignatura: 'Música', profesor: 'Gabriel Antonio Miranda Lagos', sala: 'Sala 8° Básico A' },
    { hora: '13:00 - 13:45', asignatura: 'Música', profesor: 'Gabriel Antonio Miranda Lagos', sala: 'Sala 8° Básico A' },
    { hora: '13:55 - 14:40', asignatura: 'Inglés', profesor: 'Valeria Paz Contreras Navarro', sala: 'Sala 8° Básico A' },
    { hora: '14:40 - 15:25', asignatura: 'Inglés', profesor: 'Valeria Paz Contreras Navarro', sala: 'Sala 8° Básico A' },
  ],
  Miércoles: [
    { hora: '08:00 - 08:45', asignatura: 'Historia', profesor: 'Augusto Andrés Figueroa Ríos', sala: 'Sala 8° Básico A' },
    { hora: '08:45 - 09:30', asignatura: 'Historia', profesor: 'Augusto Andrés Figueroa Ríos', sala: 'Sala 8° Básico A' },
    { hora: '09:50 - 10:35', asignatura: 'Ciencias Naturales', profesor: 'Alejandro Javier Silva Morales', sala: 'Sala 8° Básico A' },
    { hora: '10:35 - 11:20', asignatura: 'Ciencias Naturales', profesor: 'Alejandro Javier Silva Morales', sala: 'Sala 8° Básico A' },
    { hora: '12:15 - 13:00', asignatura: 'Lenguaje', profesor: 'Romina Belén Cárdenas Pizarro', sala: 'Sala 8° Básico A' },
    { hora: '13:00 - 13:45', asignatura: 'Lenguaje', profesor: 'Romina Belén Cárdenas Pizarro', sala: 'Sala 8° Básico A' },
    { hora: '13:55 - 14:40', asignatura: 'Orientación', profesor: 'Valentina Isabel Alarcón Bustos', sala: 'Sala 8° Básico A' },
    { hora: '14:40 - 15:25', asignatura: 'Orientación', profesor: 'Valentina Isabel Alarcón Bustos', sala: 'Sala 8° Básico A' },
  ],
  Jueves: [
    { hora: '08:00 - 08:45', asignatura: 'Artes Visuales', profesor: 'Paula Andrea Salazar Muñoz', sala: 'Sala 8° Básico A' },
    { hora: '08:45 - 09:30', asignatura: 'Artes Visuales', profesor: 'Paula Andrea Salazar Muñoz', sala: 'Sala 8° Básico A' },
    { hora: '09:50 - 10:35', asignatura: 'Inglés', profesor: 'Valeria Paz Contreras Navarro', sala: 'Sala 8° Básico A' },
    { hora: '10:35 - 11:20', asignatura: 'Inglés', profesor: 'Valeria Paz Contreras Navarro', sala: 'Sala 8° Básico A' },
    { hora: '12:15 - 13:00', asignatura: 'Matemáticas', profesor: 'Camila Antonia Castro Medina', sala: 'Sala 8° Básico A' },
    { hora: '13:00 - 13:45', asignatura: 'Matemáticas', profesor: 'Camila Antonia Castro Medina', sala: 'Sala 8° Básico A' },
    { hora: '13:55 - 14:40', asignatura: 'Educación Física', profesor: 'Carlos Alberto Mendoza Fuentes', sala: 'Cancha Techada 1' },
    { hora: '14:40 - 15:25', asignatura: 'Educación Física', profesor: 'Carlos Alberto Mendoza Fuentes', sala: 'Cancha Techada 1' },
  ],
  Viernes: [
    { hora: '08:00 - 08:45', asignatura: 'Lenguaje', profesor: 'Romina Belén Cárdenas Pizarro', sala: 'Sala 8° Básico A' },
    { hora: '08:45 - 09:30', asignatura: 'Lenguaje', profesor: 'Romina Belén Cárdenas Pizarro', sala: 'Sala 8° Básico A' },
    { hora: '09:50 - 10:35', asignatura: 'Historia', profesor: 'Augusto Andrés Figueroa Ríos', sala: 'Sala 8° Básico A' },
    { hora: '10:35 - 11:20', asignatura: 'Historia', profesor: 'Augusto Andrés Figueroa Ríos', sala: 'Sala 8° Básico A' },
    { hora: '12:15 - 13:00', asignatura: 'Ciencias Naturales', profesor: 'Alejandro Javier Silva Morales', sala: 'Sala 8° Básico A' },
    { hora: '13:00 - 13:45', asignatura: 'Ciencias Naturales', profesor: 'Alejandro Javier Silva Morales', sala: 'Sala 8° Básico A' },
    { hora: '13:55 - 14:40', asignatura: 'Educación Financiera', profesor: 'Claudia Marcela Espinoza Cortez', sala: 'Sala 8° Básico A' },
    { hora: '14:40 - 15:25', asignatura: 'Educación Financiera', profesor: 'Claudia Marcela Espinoza Cortez', sala: 'Sala 8° Básico A' },
  ],
};

export const CONFIG_ACADEMICA_MOCK: ConfiguracionAcademica = {
  modoCalculo: 'PONDERADO',
  pesoSemestres: { s1: 50, s2: 50 },
};

interface AsignaturaBase {
  id: number;
  nombre: string;
  docente: string;
}

export const ASIGNATURAS_BASE: AsignaturaBase[] = [
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

interface PeriodoAsistencia {
  anio: number;
  resumen: AsistenciaResumen[];
  registros: Record<number, AsistenciaRegistro[]>;
  asistenciaGeneral: number;
}

const PLAN_ASISTENCIA_2026: PlanAsistencia[] = [
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

const PLAN_ASISTENCIA_2025: PlanAsistencia[] = [
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

function diasClaseDe(asignatura: string): number[] {
  const dias = new Set<number>();
  (Object.keys(HORARIO_MOCK) as DiaSemana[]).forEach((dia) => {
    if (HORARIO_MOCK[dia].some((bloque) => bloque.asignatura === asignatura)) {
      dias.add(DIA_JS[dia]);
    }
  });
  return [...dias];
}

function isoDe(fecha: Date): string {
  return fecha.toISOString().slice(0, 10);
}

function generarRegistros(
  asignatura: string,
  plan: PlanAsistencia,
  fechaReferencia: Date,
): AsistenciaRegistro[] {
  const diasClase = diasClaseDe(asignatura);
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
  anio: number,
  plan: PlanAsistencia[],
  fechaReferencia: Date,
): PeriodoAsistencia {
  const registros: Record<number, AsistenciaRegistro[]> = {};
  const resumen: AsistenciaResumen[] = [];

  plan.forEach((item) => {
    const asignatura = ASIGNATURAS_BASE.find((base) => base.id === item.id);
    if (!asignatura) {
      return;
    }
    const registrosAsignatura = generarRegistros(asignatura.nombre, item, fechaReferencia);
    registros[item.id] = registrosAsignatura;
    resumen.push(resumirAsistencia(item.id, asignatura.nombre, registrosAsignatura));
  });

  const totalRegistradas = resumen.reduce((suma, item) => suma + item.clasesRegistradas, 0);
  const totalAsistidas = resumen.reduce((suma, item) => suma + item.clasesAsistidas, 0);
  const asistenciaGeneral =
    totalRegistradas === 0 ? 0 : Math.round((totalAsistidas / totalRegistradas) * 100);

  return { anio, resumen, registros, asistenciaGeneral };
}

export const ASISTENCIA_PERIODOS_MOCK: PeriodoAsistencia[] = [
  construirPeriodoAsistencia(2026, PLAN_ASISTENCIA_2026, new Date(Date.UTC(2026, 8, 30))),
  construirPeriodoAsistencia(2025, PLAN_ASISTENCIA_2025, new Date(Date.UTC(2025, 11, 15))),
];

export const ASISTENCIA_RESUMEN_MOCK: AsistenciaResumen[] =
  ASISTENCIA_PERIODOS_MOCK[0]?.resumen ?? [];
export const ASISTENCIA_REGISTROS_MOCK: Record<number, AsistenciaRegistro[]> =
  ASISTENCIA_PERIODOS_MOCK[0]?.registros ?? {};

interface PlanNotas {
  id: number;
  /** Promedio objetivo del periodo. */
  promedio: number;
  /** Cantidad de notas por semestre. */
  s1: number;
  s2: number;
}

const PLAN_NOTAS_2026: PlanNotas[] = [
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

const PLAN_NOTAS_2025: PlanNotas[] = [
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

const PATRONES = [-0.4, 0.3, -0.2, 0.5, -0.6, 0.2, 0.4, -0.3];
const PESOS = [20, 30, 50];

function clamp(valor: number, minimo: number, maximo: number): number {
  return Math.min(maximo, Math.max(minimo, valor));
}

/**
 * Genera notas con ponderaciones variadas y ajusta la ultima para que el
 * promedio ponderado coincida con el objetivo del periodo.
 */
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
  numero: 1 | 2,
  plan: PlanNotas[],
  config: ConfiguracionAcademica,
): SemestreNotas {
  const asignaturas = plan.map((item, index) => {
    const base = ASIGNATURAS_BASE.find((asignatura) => asignatura.id === item.id);
    const cantidad = numero === 1 ? item.s1 : item.s2;
    const notas = generarNotas(item.promedio, cantidad, index + numero);
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
  anio: number,
  curso: string,
  estado: PeriodoAcademico['estado'],
  plan: PlanNotas[],
  config: ConfiguracionAcademica,
): PeriodoAcademico {
  const semestres = [
    construirSemestre(1, plan, config),
    construirSemestre(2, plan, config),
  ];
  const asistencia = ASISTENCIA_PERIODOS_MOCK.find((periodo) => periodo.anio === anio);

  return {
    anio,
    curso,
    estado,
    semestres,
    promedioFinal: promedioFinal(semestres[0].promedio, semestres[1].promedio, config.pesoSemestres),
    asistenciaGeneral: asistencia?.asistenciaGeneral ?? 0,
  };
}

export const HISTORIAL_NOTAS_MOCK: PeriodoAcademico[] = [
  construirPeriodoAcademico(2026, '8° Básico A', 'EN_CURSO', PLAN_NOTAS_2026, CONFIG_ACADEMICA_MOCK),
  construirPeriodoAcademico(2025, '8° Básico A', 'FINALIZADO', PLAN_NOTAS_2025, CONFIG_ACADEMICA_MOCK),
];

export const PERIODO_ACTUAL_MOCK: PeriodoAcademico =
  HISTORIAL_NOTAS_MOCK.find((periodo) => periodo.estado === 'EN_CURSO') ?? HISTORIAL_NOTAS_MOCK[0];

/** Resumen por asignatura del periodo actual (para la tarjeta de Inicio). */
export const NOTAS_RESUMEN_MOCK: NotaItem[] = ASIGNATURAS_BASE.map((asignatura) => ({
  asignatura: asignatura.nombre,
  nota: promedioAsignaturaPeriodo(PERIODO_ACTUAL_MOCK, asignatura.id, CONFIG_ACADEMICA_MOCK),
}));
