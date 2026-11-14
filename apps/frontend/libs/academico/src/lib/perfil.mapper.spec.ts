import { ConfiguracionAcademica } from './models/academico.model';
import { AsistenciaDTO, PerfilEstudianteDTO } from './models/perfil.model';
import {
  asistenciaGeneralDePerfil,
  asistenciaRegistrosDePerfil,
  asistenciaResumenDePerfil,
  cursoDePerfil,
  horarioDePerfil,
  nombrePropio,
  notasResumenDePerfil,
  periodoDePerfil,
} from './perfil.mapper';

const configPonderado: ConfiguracionAcademica = {
  modoCalculo: 'PONDERADO',
  pesoSemestres: { s1: 40, s2: 60 },
};

const configSimple: ConfiguracionAcademica = {
  modoCalculo: 'SIMPLE',
  pesoSemestres: { s1: 40, s2: 60 },
};

const perfil: PerfilEstudianteDTO = {
  id: 5,
  idUsuario: 'oid-camila',
  rut: '12345678-9',
  firstName: 'Camila',
  middleName: null,
  firstSurname: 'Soto',
  secondSurname: 'Hernández',
  birthDate: '2012-04-01',
  allergies: [],
  state: 'ACTIVO',
  idClase: 3,
  clase: { id: 3, nivel: '8vo Básico', letra: 'A', anioAcademico: 2026 },
  asignaturas: [
    {
      id: 10,
      idAsignatura: 2,
      name: 'Ciencias Naturales',
      description: '',
      area: 'CIENCIAS',
      caracter: 'OBLIGATORIA',
      calificable: true,
      idDocente: 7,
      docente: 'ALEJANDRO SILVA',
      horarios: [
        {
          id: 1,
          dia: 'miércoles',
          horarioEntrada: '09:50',
          horarioSalida: '11:20',
          ubicacion: 'Sala 8A',
        },
        {
          id: 2,
          dia: 'MIERCOLES',
          horarioEntrada: '08:00',
          horarioSalida: '08:45',
          ubicacion: 'Laboratorio',
        },
        {
          id: 3,
          dia: 'LUNES',
          horarioEntrada: '08:00',
          horarioSalida: '09:30',
          ubicacion: 'Sala 8A',
        },
        {
          id: 4,
          dia: 'sábado',
          horarioEntrada: '10:00',
          horarioSalida: '11:00',
          ubicacion: 'Sala 8A',
        },
      ],
      evaluaciones: [
        { id: 1, nombre: 'PRUEBA 1', tipo: 'SUMATIVA', ponderacion: 60, nota: 6.0 },
        { id: 2, nombre: 'TRABAJO 1', tipo: 'FORMATIVA', ponderacion: 0, nota: 5.0 },
        { id: 3, nombre: 'PRUEBA 2', tipo: 'SUMATIVA', ponderacion: 40, nota: null },
      ],
    },
    {
      id: 11,
      idAsignatura: 15,
      name: 'Orientación',
      description: '',
      area: 'ORIENTACION',
      caracter: 'OBLIGATORIA',
      calificable: false,
      idDocente: 8,
      docente: null,
      horarios: [],
      evaluaciones: [{ id: 4, nombre: 'INFORME', tipo: 'FORMATIVA', ponderacion: 0, nota: 7 }],
    },
  ],
};

const asistencias: AsistenciaDTO[] = [
  {
    id: 4,
    idEstudiante: 5,
    idCursoAsignatura: 10,
    fecha: '2026-10-04',
    estado: 'PRESENTE',
    justificacion: 'PENDIENTE',
    observacion: null,
  },
  {
    id: 1,
    idEstudiante: 5,
    idCursoAsignatura: 10,
    fecha: '2026-09-30',
    estado: 'PRESENTE',
    justificacion: 'SI',
    observacion: null,
  },
  {
    id: 5,
    idEstudiante: 5,
    idCursoAsignatura: 99,
    fecha: '2026-10-05',
    estado: 'PRESENTE',
    justificacion: 'SI',
    observacion: null,
  },
  {
    id: 3,
    idEstudiante: 5,
    idCursoAsignatura: 10,
    fecha: '2026-10-06',
    estado: 'AUSENTE',
    justificacion: 'NO_APLICA',
    observacion: null,
  },
  {
    id: 2,
    idEstudiante: 5,
    idCursoAsignatura: 10,
    fecha: '2026-10-02',
    estado: 'AUSENTE',
    justificacion: 'NO',
    observacion: null,
  },
];

describe('perfil.mapper', () => {
  it('normaliza nombres propios con acentos y espacios sobrantes', () => {
    expect(nombrePropio('  ROMINA   BELÉN CÁRDENAS ')).toBe('Romina Belén Cárdenas');
    expect(nombrePropio('')).toBe('');
  });

  it('arma el curso y devuelve vacio cuando no hay clase', () => {
    expect(cursoDePerfil(perfil)).toBe('8vo Básico A');
    expect(cursoDePerfil({ ...perfil, clase: null })).toBe('');
  });

  it('agrupa el horario por dia, normaliza acentos y ordena por hora', () => {
    const horario = horarioDePerfil(perfil);

    expect(horario.Lunes).toEqual([
      {
        hora: '08:00 - 09:30',
        asignatura: 'Ciencias Naturales',
        profesor: 'Alejandro Silva',
        sala: 'Sala 8A',
      },
    ]);
    expect(horario.Miércoles).toEqual([
      {
        hora: '08:00 - 08:45',
        asignatura: 'Ciencias Naturales',
        profesor: 'Alejandro Silva',
        sala: 'Laboratorio',
      },
      {
        hora: '09:50 - 11:20',
        asignatura: 'Ciencias Naturales',
        profesor: 'Alejandro Silva',
        sala: 'Sala 8A',
      },
    ]);
    expect(horario.Martes).toEqual([]);
    expect(horario.Jueves).toEqual([]);
    expect(horario.Viernes).toEqual([]);
  });

  it('construye el periodo solo con asignaturas calificables y notas presentes', () => {
    const periodo = periodoDePerfil(perfil, configPonderado, asistencias);

    expect(periodo.anio).toBe(2026);
    expect(periodo.curso).toBe('8vo Básico A');
    expect(periodo.estado).toBe('EN_CURSO');
    expect(periodo.semestres.map((semestre) => semestre.numero)).toEqual([1, 2]);
    expect(periodo.semestres[0]).toEqual({ numero: 1, asignaturas: [], promedio: 0 });
    expect(periodo.semestres[1].asignaturas).toEqual([
      {
        id: 10,
        asignatura: 'Ciencias Naturales',
        docente: 'Alejandro Silva',
        notas: [
          { numero: 1, nombre: 'PRUEBA 1', valor: 6, ponderacion: 60 },
          { numero: 2, nombre: 'TRABAJO 1', valor: 5, ponderacion: 0 },
        ],
        promedio: 6,
      },
    ]);
    expect(periodo.semestres[1].promedio).toBe(6);
    expect(periodo.promedioFinal).toBe(6);
    expect(periodo.asistenciaGeneral).toBe(60);
  });

  it('usa el anio actual y curso vacio cuando el perfil no tiene clase', () => {
    const periodo = periodoDePerfil({ ...perfil, clase: null, asignaturas: [] }, configPonderado);

    expect(periodo.anio).toBe(new Date().getFullYear());
    expect(periodo.curso).toBe('');
    expect(periodo.promedioFinal).toBe(0);
    expect(periodo.asistenciaGeneral).toBe(0);
  });

  it('resume las notas con el modo de calculo configurado', () => {
    expect(notasResumenDePerfil(perfil, configSimple)).toEqual([
      { asignatura: 'Ciencias Naturales', nota: 5.5 },
    ]);
    expect(notasResumenDePerfil(perfil, configPonderado)).toEqual([
      { asignatura: 'Ciencias Naturales', nota: 6 },
    ]);
  });

  it('resume la asistencia por asignatura incluyendo las que no tienen registros', () => {
    expect(asistenciaResumenDePerfil(perfil, asistencias)).toEqual([
      {
        id: 10,
        asignatura: 'Ciencias Naturales',
        clasesRegistradas: 4,
        clasesAsistidas: 2,
        porcentaje: 50,
      },
      {
        id: 11,
        asignatura: 'Orientación',
        clasesRegistradas: 0,
        clasesAsistidas: 0,
        porcentaje: 0,
      },
    ]);
  });

  it('arma el historial de asistencia ordenado y traduce el estado y la justificacion', () => {
    const registros = asistenciaRegistrosDePerfil(perfil, asistencias);

    expect(registros[10]).toEqual([
      { fecha: '2026-09-30', tipo: 'Presente', justificado: 'Sí' },
      { fecha: '2026-10-02', tipo: 'Inasistencia', justificado: 'No' },
      { fecha: '2026-10-04', tipo: 'Presente', justificado: 'Pendiente' },
      { fecha: '2026-10-06', tipo: 'Inasistencia', justificado: 'No aplica' },
    ]);
    expect(registros[11]).toEqual([]);
    expect(registros[99]).toBeUndefined();
  });

  it('calcula el porcentaje general de asistencia y evita dividir por cero', () => {
    expect(asistenciaGeneralDePerfil(asistencias)).toBe(60);
    expect(asistenciaGeneralDePerfil([])).toBe(0);
  });
});
