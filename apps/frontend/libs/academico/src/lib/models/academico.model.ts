export const DIAS_SEMANA = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes'] as const;

export type DiaSemana = (typeof DIAS_SEMANA)[number];

/** Dia de la semana actual (sabado/domingo caen en Lunes). */
export function diaActual(): DiaSemana {
  const mapa: Record<number, DiaSemana> = {
    1: 'Lunes',
    2: 'Martes',
    3: 'Miércoles',
    4: 'Jueves',
    5: 'Viernes',
  };
  return mapa[new Date().getDay()] ?? 'Lunes';
}

export interface HorarioBloque {
  hora: string;
  asignatura: string;
  profesor: string;
  sala: string;
}

export interface HorarioResumen {
  hora: string;
  asignatura: string;
  profesor: string;
  sala: string;
}

export interface NotaItem {
  asignatura: string;
  nota: number;
}

export type TipoAsistencia = 'Presente' | 'Inasistencia';
export type Justificacion = 'Sí' | 'No' | 'No aplica';

export interface AsistenciaRegistro {
  /** Fecha en ISO 8601 (yyyy-MM-dd). */
  fecha: string;
  tipo: TipoAsistencia;
  justificado: Justificacion;
}

export interface AsistenciaResumen {
  id: number;
  asignatura: string;
  clasesRegistradas: number;
  clasesAsistidas: number;
  porcentaje: number;
}

/** Modo de calculo del promedio configurable por institucion. */
export type ModoCalculo = 'SIMPLE' | 'PONDERADO';

export interface NotaDetalle {
  numero: number;
  valor: number;
  /** Peso de la nota en el promedio (modo PONDERADO). */
  ponderacion: number;
}

export interface AsignaturaNotas {
  id: number;
  asignatura: string;
  docente: string;
  notas: NotaDetalle[];
  promedio: number;
}

export interface SemestreNotas {
  numero: 1 | 2;
  asignaturas: AsignaturaNotas[];
  promedio: number;
}

export interface PeriodoAcademico {
  anio: number;
  curso: string;
  estado: 'EN_CURSO' | 'FINALIZADO';
  semestres: SemestreNotas[];
  promedioFinal: number;
  asistenciaGeneral: number;
}

export interface ConfiguracionAcademica {
  modoCalculo: ModoCalculo;
  pesoSemestres: { s1: number; s2: number };
}

export function redondear1(valor: number): number {
  return Math.round(valor * 10) / 10;
}

function horaInicio(bloque: HorarioBloque): string {
  return bloque.hora.split(' - ')[0];
}

function horaFin(bloque: HorarioBloque): string {
  return bloque.hora.split(' - ')[1];
}

/**
 * Junta bloques consecutivos de la misma asignatura/profesor/sala en un solo
 * periodo (resumen de Inicio). Ej.: 08:00-08:45 + 08:45-09:30 => 08:00-09:30.
 */
export function resumirBloques(bloques: HorarioBloque[]): HorarioResumen[] {
  const resumen: HorarioResumen[] = [];

  for (const bloque of bloques) {
    const ultimo = resumen[resumen.length - 1];
    if (
      ultimo &&
      ultimo.asignatura === bloque.asignatura &&
      ultimo.profesor === bloque.profesor &&
      ultimo.sala === bloque.sala &&
      horaFin(bloque) !== '' &&
      ultimo.hora.includes(horaInicio(bloque))
    ) {
      const inicioOriginal = ultimo.hora.split(' - ')[0];
      ultimo.hora = `${inicioOriginal} - ${horaFin(bloque)}`;
      continue;
    }

    resumen.push({
      hora: bloque.hora,
      asignatura: bloque.asignatura,
      profesor: bloque.profesor,
      sala: bloque.sala,
    });
  }

  return resumen;
}

/** Calcula el resumen de asistencia a partir de los registros. */
export function resumirAsistencia(
  id: number,
  asignatura: string,
  registros: AsistenciaRegistro[],
): AsistenciaResumen {
  const clasesRegistradas = registros.length;
  const clasesAsistidas = registros.filter((registro) => registro.tipo === 'Presente').length;
  const porcentaje =
    clasesRegistradas === 0 ? 0 : Math.round((clasesAsistidas / clasesRegistradas) * 100);

  return { id, asignatura, clasesRegistradas, clasesAsistidas, porcentaje };
}

/** Formatea una fecha ISO (yyyy-MM-dd) a dd/MM/yyyy para mostrar en la UI. */
export function formatearFecha(iso: string): string {
  const [anio, mes, dia] = iso.split('-');
  if (!anio || !mes || !dia) {
    return iso;
  }
  return `${dia}/${mes}/${anio}`;
}

/** Formatea una nota para mostrarla en la UI: 1 decimal con coma (6.0 -> "6,0"). */
export function formatearNota(valor: number): string {
  return valor.toFixed(1).replace('.', ',');
}

/**
 * Parsea una nota escrita por el usuario (acepta coma o punto), valida el rango
 * 1.0-7.0 y la redondea a 1 decimal. Devuelve null si no es valida.
 */
export function parseNota(texto: string): number | null {
  const limpio = (texto ?? '').trim().replace(',', '.');
  const valor = Number(limpio);
  if (!Number.isFinite(valor) || valor < 1 || valor > 7) {
    return null;
  }
  return Math.round(valor * 10) / 10;
}

/**
 * Promedio de una lista de notas segun el modo configurado.
 * - SIMPLE: suma(valores) / cantidad.
 * - PONDERADO: suma(valor * ponderacion) / suma(ponderacion).
 */
export function promedioNotas(notas: NotaDetalle[], modo: ModoCalculo): number {
  if (notas.length === 0) {
    return 0;
  }
  if (modo === 'SIMPLE') {
    return redondear1(notas.reduce((suma, nota) => suma + nota.valor, 0) / notas.length);
  }
  const sumaPesos = notas.reduce((suma, nota) => suma + nota.ponderacion, 0);
  if (sumaPesos === 0) {
    return redondear1(notas.reduce((suma, nota) => suma + nota.valor, 0) / notas.length);
  }
  return redondear1(
    notas.reduce((suma, nota) => suma + nota.valor * nota.ponderacion, 0) / sumaPesos,
  );
}

/** Promedio simple de los promedios de las asignaturas. */
export function promedioAsignaturas(asignaturas: AsignaturaNotas[]): number {
  if (asignaturas.length === 0) {
    return 0;
  }
  return redondear1(
    asignaturas.reduce((suma, asignatura) => suma + asignatura.promedio, 0) / asignaturas.length,
  );
}

/** Promedio final del periodo ponderando los semestres. */
export function promedioFinal(
  promedioS1: number,
  promedioS2: number,
  pesos: { s1: number; s2: number },
): number {
  const sumaPesos = pesos.s1 + pesos.s2;
  if (sumaPesos === 0) {
    return redondear1((promedioS1 + promedioS2) / 2);
  }
  return redondear1((promedioS1 * pesos.s1 + promedioS2 * pesos.s2) / sumaPesos);
}

/** Promedio de una asignatura en el periodo, ponderando los semestres. */
export function promedioAsignaturaPeriodo(
  periodo: PeriodoAcademico,
  asignaturaId: number,
  config: ConfiguracionAcademica,
): number {
  const [s1, s2] = periodo.semestres;
  const p1 = s1?.asignaturas.find((asignatura) => asignatura.id === asignaturaId)?.promedio ?? 0;
  const p2 = s2?.asignaturas.find((asignatura) => asignatura.id === asignaturaId)?.promedio ?? 0;
  return promedioFinal(p1, p2, config.pesoSemestres);
}
