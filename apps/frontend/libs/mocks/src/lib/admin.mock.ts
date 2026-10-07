export type RolAdmin = 'ADMIN' | 'DOCENTE' | 'APODERADO' | 'ESTUDIANTE';
export type EstadoAdmin = 'ACTIVO' | 'INACTIVO';

export interface UsuarioAdmin {
  id: string;
  nombre: string;
  email: string;
  rol: RolAdmin;
  estado: EstadoAdmin;
}

/** Detalle de usuario del panel admin (cuenta + resumen del perfil del rol). */
export interface UsuarioDetalle {
  id: string;
  fullName: string;
  email: string;
  rol: string | null;
  estado: string | null;
  rut: string | null;
  fechaNacimiento: string | null;
  detalle: string;
  etiquetas: string[];
}

export type CaracterAsignatura = 'OBLIGATORIA' | 'OPTATIVA' | 'ELECTIVA';
export type PlanFormacion = 'COMUN' | 'DIFERENCIADA_HC' | 'DIFERENCIADA_TP';

export interface AsignaturaAdmin {
  id: number;
  nombre: string;
  area: string;
  calificable: boolean;
  niveles: string[];
  activa: boolean;
}

export interface MallaFila {
  id: number;
  nivel: string;
  idAsignatura: number;
  nombre: string;
  area: string;
  caracter: CaracterAsignatura;
  plan: PlanFormacion;
  horasSemanales: number | null;
  calificable: boolean;
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

const BASICOS = [
  '1ro Básico', '2do Básico', '3ro Básico', '4to Básico',
  '5to Básico', '6to Básico', '7mo Básico', '8vo Básico',
];
const MEDIOS = ['1ro Medio', '2do Medio', '3ro Medio', '4to Medio'];
export const NIVELES_MOCK = [...BASICOS, ...MEDIOS];

const rango = (desde: string, hasta: string): string[] =>
  NIVELES_MOCK.slice(NIVELES_MOCK.indexOf(desde), NIVELES_MOCK.indexOf(hasta) + 1);

const AREA = {
  lenguaje: 'Lenguaje y Comunicación',
  matematicas: 'Matemáticas',
  ciencias: 'Ciencias Naturales y Exactas',
  cienciasCiudadania: 'Ciencias para la Ciudadanía',
  historia: 'Historia y Ciencias Sociales',
  idiomas: 'Lenguas e Idiomas',
  artes: 'Artes y Música',
  educacionFisica: 'Educación Física',
  tecnologia: 'Tecnología e Informática',
  orientacion: 'Orientación',
  religion: 'Religión',
  economia: 'Economía y Finanzas',
  ciudadania: 'Formación Ciudadana',
  filosofia: 'Filosofía',
};

/** Catálogo general de asignaturas (no incluye cursos ni docentes). */
export const ASIGNATURAS_MOCK: AsignaturaAdmin[] = [
  { id: 1, nombre: 'Lenguaje y Comunicación', area: AREA.lenguaje, calificable: true, niveles: rango('1ro Básico', '6to Básico'), activa: true },
  { id: 2, nombre: 'Lengua y Literatura', area: AREA.lenguaje, calificable: true, niveles: rango('7mo Básico', '4to Medio'), activa: true },
  { id: 3, nombre: 'Matemática', area: AREA.matematicas, calificable: true, niveles: rango('1ro Básico', '4to Medio'), activa: true },
  { id: 4, nombre: 'Ciencias Naturales', area: AREA.ciencias, calificable: true, niveles: rango('1ro Básico', '8vo Básico'), activa: true },
  { id: 5, nombre: 'Biología', area: AREA.ciencias, calificable: true, niveles: rango('1ro Medio', '2do Medio'), activa: true },
  { id: 6, nombre: 'Física', area: AREA.ciencias, calificable: true, niveles: rango('1ro Medio', '2do Medio'), activa: true },
  { id: 7, nombre: 'Química', area: AREA.ciencias, calificable: true, niveles: rango('1ro Medio', '2do Medio'), activa: true },
  { id: 8, nombre: 'Ciencias para la Ciudadanía', area: AREA.cienciasCiudadania, calificable: true, niveles: rango('3ro Medio', '4to Medio'), activa: true },
  { id: 9, nombre: 'Historia, Geografía y Ciencias Sociales', area: AREA.historia, calificable: true, niveles: rango('1ro Básico', '4to Medio'), activa: true },
  { id: 10, nombre: 'Inglés', area: AREA.idiomas, calificable: true, niveles: rango('1ro Básico', '4to Medio'), activa: true },
  { id: 11, nombre: 'Artes Visuales', area: AREA.artes, calificable: true, niveles: rango('1ro Básico', '2do Medio'), activa: true },
  { id: 12, nombre: 'Música', area: AREA.artes, calificable: true, niveles: rango('1ro Básico', '2do Medio'), activa: true },
  { id: 13, nombre: 'Educación Física y Salud', area: AREA.educacionFisica, calificable: true, niveles: rango('1ro Básico', '2do Medio'), activa: true },
  { id: 14, nombre: 'Tecnología', area: AREA.tecnologia, calificable: true, niveles: rango('1ro Básico', '2do Medio'), activa: true },
  { id: 15, nombre: 'Orientación', area: AREA.orientacion, calificable: false, niveles: rango('1ro Básico', '4to Medio'), activa: true },
  { id: 16, nombre: 'Religión', area: AREA.religion, calificable: true, niveles: rango('1ro Básico', '4to Medio'), activa: true },
  { id: 17, nombre: 'Educación Financiera', area: AREA.economia, calificable: true, niveles: rango('5to Básico', '2do Medio'), activa: true },
  { id: 18, nombre: 'Lengua de Señas', area: AREA.idiomas, calificable: true, niveles: rango('7mo Básico', '8vo Básico'), activa: true },
  { id: 19, nombre: 'Lengua y Cultura de los Pueblos Originarios', area: AREA.idiomas, calificable: true, niveles: rango('7mo Básico', '8vo Básico'), activa: true },
  { id: 20, nombre: 'Economía y Sociedad', area: AREA.economia, calificable: true, niveles: rango('1ro Medio', '2do Medio'), activa: true },
  { id: 21, nombre: 'Educación Ciudadana', area: AREA.ciudadania, calificable: true, niveles: rango('3ro Medio', '4to Medio'), activa: true },
  { id: 22, nombre: 'Filosofía', area: AREA.filosofia, calificable: true, niveles: rango('3ro Medio', '4to Medio'), activa: true },
];

type DefMalla = [idAsignatura: number, caracter: CaracterAsignatura];

const MALLA_1_A_4: DefMalla[] = [
  [1, 'OBLIGATORIA'], [3, 'OBLIGATORIA'], [4, 'OBLIGATORIA'], [9, 'OBLIGATORIA'],
  [10, 'OPTATIVA'], [11, 'OBLIGATORIA'], [12, 'OBLIGATORIA'], [13, 'OBLIGATORIA'],
  [14, 'OBLIGATORIA'], [15, 'OBLIGATORIA'], [16, 'OPTATIVA'],
];
const MALLA_5_A_6: DefMalla[] = [
  [1, 'OBLIGATORIA'], [3, 'OBLIGATORIA'], [4, 'OBLIGATORIA'], [9, 'OBLIGATORIA'],
  [10, 'OBLIGATORIA'], [11, 'OBLIGATORIA'], [12, 'OBLIGATORIA'], [13, 'OBLIGATORIA'],
  [14, 'OBLIGATORIA'], [15, 'OBLIGATORIA'], [16, 'OPTATIVA'], [17, 'OPTATIVA'],
];
const MALLA_7_A_8: DefMalla[] = [
  [2, 'OBLIGATORIA'], [3, 'OBLIGATORIA'], [4, 'OBLIGATORIA'], [9, 'OBLIGATORIA'],
  [10, 'OBLIGATORIA'], [11, 'OBLIGATORIA'], [12, 'OBLIGATORIA'], [13, 'OBLIGATORIA'],
  [14, 'OBLIGATORIA'], [15, 'OBLIGATORIA'], [16, 'OPTATIVA'], [17, 'OPTATIVA'],
  [18, 'OPTATIVA'], [19, 'OPTATIVA'],
];
const MALLA_1_A_2_MEDIO: DefMalla[] = [
  [2, 'OBLIGATORIA'], [10, 'OBLIGATORIA'], [3, 'OBLIGATORIA'], [9, 'OBLIGATORIA'],
  [5, 'OBLIGATORIA'], [6, 'OBLIGATORIA'], [7, 'OBLIGATORIA'], [14, 'OBLIGATORIA'],
  [11, 'ELECTIVA'], [12, 'ELECTIVA'], [13, 'OBLIGATORIA'], [15, 'OBLIGATORIA'],
  [16, 'OPTATIVA'], [20, 'OPTATIVA'],
];
const MALLA_3_A_4_MEDIO: DefMalla[] = [
  [2, 'OBLIGATORIA'], [10, 'OBLIGATORIA'], [3, 'OBLIGATORIA'], [21, 'OBLIGATORIA'],
  [22, 'OBLIGATORIA'], [8, 'OBLIGATORIA'], [15, 'OBLIGATORIA'], [16, 'OPTATIVA'],
];

const definiciones: Record<string, DefMalla[]> = {
  '1ro Básico': MALLA_1_A_4,
  '2do Básico': MALLA_1_A_4,
  '3ro Básico': MALLA_1_A_4,
  '4to Básico': MALLA_1_A_4,
  '5to Básico': MALLA_5_A_6,
  '6to Básico': MALLA_5_A_6,
  '7mo Básico': MALLA_7_A_8,
  '8vo Básico': MALLA_7_A_8,
  '1ro Medio': MALLA_1_A_2_MEDIO,
  '2do Medio': MALLA_1_A_2_MEDIO,
  '3ro Medio': MALLA_3_A_4_MEDIO,
  '4to Medio': MALLA_3_A_4_MEDIO,
};

let mallaId = 0;
export const MALLA_MOCK: MallaFila[] = NIVELES_MOCK.flatMap((nivel) =>
  definiciones[nivel].map(([idAsignatura, caracter]) => {
    const asignatura = ASIGNATURAS_MOCK.find((item) => item.id === idAsignatura)!;
    return {
      id: ++mallaId,
      nivel,
      idAsignatura,
      nombre: asignatura.nombre,
      area: asignatura.area,
      caracter,
      plan: 'COMUN' as PlanFormacion,
      horasSemanales: null,
      calificable: asignatura.calificable,
      activa: asignatura.activa,
    };
  }),
);

export const ROLES_MOCK = [
  { nombre: 'ADMIN', descripcion: 'Gestion institucional, usuarios y datos generales.' },
  { nombre: 'DOCENTE', descripcion: 'Cursos, evaluaciones, notas y asistencias.' },
  { nombre: 'APODERADO', descripcion: 'Seguimiento academico de sus pupilos.' },
  { nombre: 'ESTUDIANTE', descripcion: 'Notas, horarios y asistencias propias.' },
];

// --- Registro asincrono de usuarios (usuario + perfil de rol) ---

export type EstadoProcesoRegistro = 'PENDIENTE' | 'EN_PROCESO' | 'COMPLETADO' | 'FALLIDO';
export type EstadoPasoRegistro = 'PENDIENTE' | 'COMPLETADO' | 'FALLIDO';
export type RolRegistrable = 'ESTUDIANTE' | 'DOCENTE' | 'APODERADO';

export interface CertificadoRegistro {
  nombre: string;
  institucionRealizacion: string;
  fechaTitulacion: string;
}

export interface DatosEstudianteRegistro {
  firstName: string;
  middleName?: string | null;
  firstSurname: string;
  secondSurname?: string | null;
  rut: string;
  birthDate: string;
  allergies?: string[] | null;
  idClase?: number | null;
}

export interface DatosDocenteRegistro {
  firstName: string;
  middleName?: string | null;
  firstSurname: string;
  secondSurname?: string | null;
  rut: string;
  fechaContratacion: string;
  area: string;
  certificados: CertificadoRegistro[];
}

export interface VinculoEstudianteRegistro {
  idEstudiante: number;
  parentesco: string;
}

export interface DatosApoderadoRegistro {
  firstName: string;
  middleName?: string | null;
  firstSurname: string;
  secondSurname?: string | null;
  rut: string;
  telefonos: string[];
  estudiantes: VinculoEstudianteRegistro[];
}

export interface DatosRegistroRol {
  estudiante?: DatosEstudianteRegistro | null;
  docente?: DatosDocenteRegistro | null;
  apoderado?: DatosApoderadoRegistro | null;
}

export interface RegistroUsuarioPayload {
  /** Opcional: si se omite, el backend lo genera como nombre.apellido@dominio. */
  email?: string | null;
  /** Opcional: si se omite, el backend lo deriva de los nombres del rol. */
  fullName?: string | null;
  requestedRole: RolRegistrable;
  azureUserId?: string | null;
  contactEmail?: string | null;
  roleData: DatosRegistroRol;
}

export interface RegistroUsuarioEstado {
  processId: string;
  state: EstadoProcesoRegistro;
  azureState: EstadoPasoRegistro;
  domainState: EstadoPasoRegistro;
  requestedRole: RolAdmin;
  email: string;
  userId: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CredencialTemporal {
  processId: string | null;
  email: string;
  userId: string | null;
  temporaryPassword: string;
  expiresAt: string | null;
}

/** Valores visibles de AreaAcademica (core-share serializa el nombre). */
export const AREAS_ACADEMICAS = [
  'Matemáticas',
  'Ciencias Naturales y Exactas',
  'Ciencias para la Ciudadanía',
  'Lenguaje y Comunicación',
  'Historia y Ciencias Sociales',
  'Formación Ciudadana',
  'Filosofía',
  'Lenguas e Idiomas',
  'Artes y Música',
  'Educación Física',
  'Tecnología e Informática',
  'Orientación',
  'Religión',
  'Economía y Finanzas',
  'Otra Área',
];

/** Valores visibles de Parentesco (core-share acepta nombre o texto). */
export const PARENTESCOS = [
  'Madre/Padre',
  'Tía/Tío',
  'Abuela/Abuelo',
  'Hermana/Hermano',
  'Primo/Prima',
  'Tutor/a Legal',
  'Otro',
];

export const esEstadoFinal = (estado: EstadoProcesoRegistro): boolean =>
  estado === 'COMPLETADO' || estado === 'FALLIDO';

// --- Selectores del registro (clases y alumnos) ---

export interface ClaseOpcion {
  id: number;
  nivel: string;
  letra: string;
  anioAcademico: number;
}

export interface EstudianteOpcion {
  id: number;
  rut: string;
  firstName: string;
  firstSurname: string;
}

/** 22126386-3 -> 22.126.386-3 */
export const formatearRut = (rut: string | null | undefined): string => {
  const limpio = (rut ?? '').trim().toUpperCase();
  const [cuerpo, dv] = limpio.split('-');
  if (!cuerpo || !dv) {
    return limpio;
  }
  return `${cuerpo.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}-${dv}`;
};

/** 1ro Básico A · 2026 */
export const etiquetaClase = (clase: ClaseOpcion): string =>
  `${clase.nivel} ${clase.letra} · ${clase.anioAcademico}`;

// --- Normalizacion de nombres propios (espejo del backend NombrePropio) ---

const PARTICULAS_NOMBRE = new Set(['de', 'del', 'la', 'las', 'los', 'y', 'e']);

const normalizarPalabraNombre = (palabra: string): string => {
  if (!palabra) {
    return palabra;
  }
  if (PARTICULAS_NOMBRE.has(palabra)) {
    return palabra;
  }
  let resultado = palabra.replace(
    /(^|[-'’])(\p{L})/gu,
    (_coincidencia, separador: string, letra: string) => separador + letra.toUpperCase(),
  );
  const lower = resultado.toLowerCase();
  if (lower.startsWith('mc') && resultado.length > 2) {
    resultado = resultado.slice(0, 2) + resultado.charAt(2).toUpperCase() + resultado.slice(3);
  } else if (lower.startsWith('mac') && resultado.length > 3 && !'aeiou'.includes(lower.charAt(3))) {
    resultado = resultado.slice(0, 3) + resultado.charAt(3).toUpperCase() + resultado.slice(4);
  }
  return resultado;
};

/** Catalina, Juan de la Rosa, McDonald, O'Hara, Pérez-Gómez. */
export const normalizarNombrePropio = (texto: string | null | undefined): string => {
  const limpio = (texto ?? '').trim().replace(/\s+/g, ' ');
  if (!limpio) {
    return '';
  }
  return limpio
    .toLowerCase()
    .split(' ')
    .map((palabra) => normalizarPalabraNombre(palabra))
    .join(' ');
};

/** 22.126.386-3 | Catalina Ormeño */
export const etiquetaEstudiante = (estudiante: EstudianteOpcion): string =>
  `${formatearRut(estudiante.rut)} | ${normalizarNombrePropio(estudiante.firstName)} ${normalizarNombrePropio(estudiante.firstSurname)}`;

// --- Mascara de RUT para el formulario ---

const conPuntosMiles = (cuerpo: string): string =>
  cuerpo.replace(/\B(?=(\d{3})+(?!\d))/g, '.');

/**
 * Mascara de RUT: muestra puntos mientras se escribe y agrega el guion cuando
 * se teclea el digito verificador (o el usuario escribe "-"). Sin DV: 13.789.943.
 */
export const formatearRutEntrada = (valor: string | null | undefined): string => {
  const bruto = (valor ?? '').toUpperCase();
  if (bruto.includes('-')) {
    const [cuerpoBruto, ...resto] = bruto.split('-');
    const cuerpo = cuerpoBruto.replace(/[^0-9]/g, '').slice(0, 8);
    const dv = resto.join('').replace(/[^0-9K]/g, '').slice(0, 1);
    return `${conPuntosMiles(cuerpo)}-${dv}`;
  }
  const limpio = bruto.replace(/[^0-9K]/g, '').slice(0, 9);
  if (limpio.length <= 8) {
    return conPuntosMiles(limpio);
  }
  return `${conPuntosMiles(limpio.slice(0, 8))}-${limpio.charAt(8)}`;
};

// --- Generación del correo institucional (espejo del backend, solo preview) ---

export const DOMINIO_CORREO = 'platformsiga.onmicrosoft.com';

export const normalizarParaCorreo = (texto: string | null | undefined): string =>
  (texto ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]/g, '');

const primeraPalabraCorreo = (texto: string | null | undefined): string =>
  normalizarParaCorreo((texto ?? '').trim().split(/\s+/)[0] ?? '');

/** catalina.ormeno@platformsiga.onmicrosoft.com (vacío si faltan nombre o apellido). */
export const correoInstitucionalSugerido = (
  primerNombre: string | null | undefined,
  primerApellido: string | null | undefined,
): string => {
  const nombre = primeraPalabraCorreo(primerNombre);
  const apellido = primeraPalabraCorreo(primerApellido);
  if (!nombre || !apellido) {
    return '';
  }
  return `${nombre}.${apellido}@${DOMINIO_CORREO}`;
};

/** Une los nombres presentes con espacio. */
export const nombreCompletoSugerido = (...partes: (string | null | undefined)[]): string =>
  partes
    .map((parte) => (parte ?? '').trim())
    .filter(Boolean)
    .join(' ');
