import {
  AsistenciaRegistro,
  AsistenciaResumen,
  ConfiguracionAcademica,
  DIAS_SEMANA,
  DiaSemana,
  HorarioBloque,
  Justificacion,
  NotaDetalle,
  NotaItem,
  PeriodoAcademico,
  SemestreNotas,
  promedioAsignaturas,
  promedioNotas,
} from './models/academico.model';
import {
  AsistenciaDTO,
  JustificacionAsistenciaDTO,
  PerfilEstudianteDTO,
} from './models/perfil.model';

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
        asignatura: asignatura.name,
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
  asistencias: AsistenciaDTO[] = [],
): PeriodoAcademico {
  const asignaturas = perfil.asignaturas
    .filter((asignatura) => asignatura.calificable)
    .map((asignatura) => {
      const notas: NotaDetalle[] = asignatura.evaluaciones
        .filter((evaluacion) => evaluacion.nota !== null)
        .map((evaluacion, indice) => ({
          numero: indice + 1,
          nombre: evaluacion.nombre,
          valor: evaluacion.nota as number,
          ponderacion: evaluacion.ponderacion,
        }));

      return {
        id: asignatura.id,
        asignatura: asignatura.name,
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
    asistenciaGeneral: asistenciaGeneralDePerfil(asistencias),
  };
}

/** Resumen de notas por asignatura para las tarjetas de Inicio. */
export function notasResumenDePerfil(
  perfil: PerfilEstudianteDTO,
  config: ConfiguracionAcademica,
): NotaItem[] {
  return perfil.asignaturas
    .filter((asignatura) => asignatura.calificable)
    .map((asignatura) => {
      const notas: NotaDetalle[] = asignatura.evaluaciones
        .filter((evaluacion) => evaluacion.nota !== null)
        .map((evaluacion) => ({
          numero: 0,
          nombre: evaluacion.nombre,
          valor: evaluacion.nota as number,
          ponderacion: evaluacion.ponderacion,
        }));
      return {
        asignatura: asignatura.name,
        nota: promedioNotas(notas, config.modoCalculo),
      };
    });
}

function agruparPorAsignatura(asistencias: AsistenciaDTO[]): Map<number, AsistenciaDTO[]> {
  const porAsignatura = new Map<number, AsistenciaDTO[]>();
  for (const asistencia of asistencias) {
    const lista = porAsignatura.get(asistencia.idCursoAsignatura) ?? [];
    lista.push(asistencia);
    porAsignatura.set(asistencia.idCursoAsignatura, lista);
  }
  return porAsignatura;
}

function justificacionTexto(justificacion: JustificacionAsistenciaDTO): Justificacion {
  switch (justificacion) {
    case 'SI':
      return 'Sí';
    case 'NO':
      return 'No';
    case 'PENDIENTE':
      return 'Pendiente';
    default:
      return 'No aplica';
  }
}

/** Resumen de asistencia por asignatura a partir de las asistencias reales. */
export function asistenciaResumenDePerfil(
  perfil: PerfilEstudianteDTO,
  asistencias: AsistenciaDTO[],
): AsistenciaResumen[] {
  const porAsignatura = agruparPorAsignatura(asistencias);
  return perfil.asignaturas.map((asignatura) => {
    const registros = porAsignatura.get(asignatura.id) ?? [];
    const clasesRegistradas = registros.length;
    const clasesAsistidas = registros.filter((registro) => registro.estado === 'PRESENTE').length;
    const porcentaje =
      clasesRegistradas === 0 ? 0 : Math.round((clasesAsistidas / clasesRegistradas) * 100);
    return {
      id: asignatura.id,
      asignatura: asignatura.name,
      clasesRegistradas,
      clasesAsistidas,
      porcentaje,
    };
  });
}

/** Historial de asistencia por asignatura (fechas ISO ordenadas). */
export function asistenciaRegistrosDePerfil(
  perfil: PerfilEstudianteDTO,
  asistencias: AsistenciaDTO[],
): Record<number, AsistenciaRegistro[]> {
  const porAsignatura = agruparPorAsignatura(asistencias);
  const registros: Record<number, AsistenciaRegistro[]> = {};
  for (const asignatura of perfil.asignaturas) {
    registros[asignatura.id] = (porAsignatura.get(asignatura.id) ?? [])
      .slice()
      .sort((a, b) => a.fecha.localeCompare(b.fecha))
      .map((asistencia) => ({
        fecha: asistencia.fecha,
        tipo: asistencia.estado === 'PRESENTE' ? 'Presente' : 'Inasistencia',
        justificado: justificacionTexto(asistencia.justificacion),
      }));
  }
  return registros;
}

/** Porcentaje general de asistencia (presentes / registros). */
export function asistenciaGeneralDePerfil(asistencias: AsistenciaDTO[]): number {
  const total = asistencias.length;
  const presentes = asistencias.filter((asistencia) => asistencia.estado === 'PRESENTE').length;
  return total === 0 ? 0 : Math.round((presentes / total) * 100);
}
