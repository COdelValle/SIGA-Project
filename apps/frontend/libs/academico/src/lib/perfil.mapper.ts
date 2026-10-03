import {
  AsistenciaRegistro,
  AsistenciaResumen,
  ConfiguracionAcademica,
  DIAS_SEMANA,
  DiaSemana,
  HorarioBloque,
  NotaDetalle,
  NotaItem,
  PeriodoAcademico,
  SemestreNotas,
  promedioAsignaturas,
  promedioNotas,
} from './models/academico.model';
import { PerfilEstudianteDTO } from './models/perfil.model';

const NOMBRES_ASIGNATURA: Record<string, string> = {
  MATEMATICA: 'Matemáticas',
  LENGUAJE: 'Lenguaje',
  CIENCIAS: 'Ciencias Naturales',
  HISTORIA: 'Historia',
  'ARTES VISUALES': 'Artes Visuales',
  TECNOLOGIA: 'Tecnología',
  MUSICA: 'Música',
  ORIENTACION: 'Orientación',
  'EDUCACION FINANCIERA': 'Educación Financiera',
  RELIGION: 'Religión',
  INGLES: 'Inglés',
  'EDUCACION FISICA': 'Educación Física',
};

/** Nombre de asignatura listo para mostrar (quita el sufijo de curso y restaura tildes). */
export function nombreAsignatura(name: string): string {
  const base = name.replace(/\s+\d+[A-C]$/i, '').trim().toUpperCase();
  return NOMBRES_ASIGNATURA[base] ?? name;
}

/** Convierte "ROMINA BELÉN CÁRDENAS" en "Romina Belén Cárdenas". */
export function nombrePropio(valor: string): string {
  return valor
    .toLocaleLowerCase('es-CL')
    .split(/\s+/)
    .filter((parte) => parte.length > 0)
    .map((parte) => parte.charAt(0).toLocaleUpperCase('es-CL') + parte.slice(1))
    .join(' ');
}

export function cursoDePerfil(perfil: PerfilEstudianteDTO): string {
  const clase = perfil.clase;
  return clase ? `${clase.nivel} ${clase.letra}` : '';
}

function normalizarDia(dia: string): DiaSemana | null {
  const norm = (valor: string) =>
    valor
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase();
  const buscado = norm(dia);
  return DIAS_SEMANA.find((candidato) => norm(candidato) === buscado) ?? null;
}

/** Arma el horario semanal del frontend a partir del perfil real. */
export function horarioDePerfil(perfil: PerfilEstudianteDTO): Record<DiaSemana, HorarioBloque[]> {
  const horario: Record<DiaSemana, HorarioBloque[]> = {
    Lunes: [],
    Martes: [],
    Miércoles: [],
    Jueves: [],
    Viernes: [],
  };

  for (const asignatura of perfil.asignaturas) {
    for (const bloque of asignatura.horarios) {
      const dia = normalizarDia(bloque.dia);
      if (!dia) {
        continue;
      }
      horario[dia].push({
        hora: `${bloque.horarioEntrada} - ${bloque.horarioSalida}`,
        asignatura: nombreAsignatura(asignatura.name),
        profesor: asignatura.docente ? nombrePropio(asignatura.docente) : '',
        sala: bloque.ubicacion,
      });
    }
  }

  for (const dia of DIAS_SEMANA) {
    horario[dia].sort((a, b) => a.hora.localeCompare(b.hora));
  }
  return horario;
}

/** Convierte el perfil real al periodo academico que consumen las vistas de notas. */
export function periodoDePerfil(
  perfil: PerfilEstudianteDTO,
  config: ConfiguracionAcademica,
): PeriodoAcademico {
  const asignaturas = perfil.asignaturas.map((asignatura) => {
    const notas: NotaDetalle[] = asignatura.evaluaciones
      .filter((evaluacion) => evaluacion.nota !== null)
      .map((evaluacion, indice) => ({
        numero: indice + 1,
        valor: evaluacion.nota as number,
        ponderacion: evaluacion.ponderacion,
      }));

    return {
      id: asignatura.id,
      asignatura: nombreAsignatura(asignatura.name),
      docente: asignatura.docente ? nombrePropio(asignatura.docente) : '',
      notas,
      promedio: promedioNotas(notas, config.modoCalculo),
    };
  });

  // La base aun no distingue semestres: las evaluaciones vigentes se muestran
  // en el semestre 2 (periodo en curso) y el 1 queda vacio.
  const semestreActual: SemestreNotas = {
    numero: 2,
    asignaturas,
    promedio: promedioAsignaturas(asignaturas),
  };
  const semestreAnterior: SemestreNotas = { numero: 1, asignaturas: [], promedio: 0 };

  return {
    anio: perfil.clase?.anioAcademico ?? new Date().getFullYear(),
    curso: cursoDePerfil(perfil),
    estado: 'EN_CURSO',
    semestres: [semestreAnterior, semestreActual],
    promedioFinal: semestreActual.promedio,
    asistenciaGeneral: 0,
  };
}

/** Resumen de notas por asignatura para las tarjetas de Inicio. */
export function notasResumenDePerfil(
  perfil: PerfilEstudianteDTO,
  config: ConfiguracionAcademica,
): NotaItem[] {
  return perfil.asignaturas.map((asignatura) => {
    const notas: NotaDetalle[] = asignatura.evaluaciones
      .filter((evaluacion) => evaluacion.nota !== null)
      .map((evaluacion) => ({
        numero: 0,
        valor: evaluacion.nota as number,
        ponderacion: evaluacion.ponderacion,
      }));
    return {
      asignatura: nombreAsignatura(asignatura.name),
      nota: promedioNotas(notas, config.modoCalculo),
    };
  });
}

/** Asistencia del perfil: se completa cuando exista ms-asistencias. */
export function asistenciaResumenDePerfil(perfil: PerfilEstudianteDTO): AsistenciaResumen[] {
  void perfil;
  return [];
}

export function asistenciaRegistrosDePerfil(
  perfil: PerfilEstudianteDTO,
): Record<number, AsistenciaRegistro[]> {
  void perfil;
  return {};
}
