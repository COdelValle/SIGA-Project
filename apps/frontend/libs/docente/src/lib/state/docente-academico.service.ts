import { Injectable, computed, effect, signal } from '@angular/core';
import { DiaSemana, formatearFecha } from '@siga/academico';
import {
  CURSOS_DOCENTE_MOCK,
  DOCENTE_ACTUAL_ID,
  CursoDocente,
  horarioDelDocente,
  cursosDelDocente,
} from '../mocks/docente.mock';

export interface NotaAlumno {
  numero: number;
  valor: number;
}

export type EstadoAsistencia = 'Presente' | 'Ausente';
export type Justificacion = 'No aplica' | 'Pendiente' | 'Sí' | 'No';

export interface RegistroAsistencia {
  alumnoId: number;
  estado: EstadoAsistencia;
  justificacion: Justificacion;
  /** Fecha limite (ISO) para gestionar el justificado (solo Ausente). */
  limite?: string;
}

interface Persistencia {
  notas: Record<string, NotaAlumno[]>;
  asistencia: Record<string, RegistroAsistencia[]>;
  guardados: string[];
}

const DIAS_JS: Record<number, DiaSemana> = {
  1: 'Lunes',
  2: 'Martes',
  3: 'Miércoles',
  4: 'Jueves',
  5: 'Viernes',
};

function isoDe(fecha: Date): string {
  return fecha.toISOString().slice(0, 10);
}

/** Fecha ISO (yyyy-MM-dd) usando la fecha LOCAL del navegador. */
function isoLocal(fecha: Date): string {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
}

function parseIso(iso: string): Date {
  const [anio, mes, dia] = iso.split('-').map(Number);
  return new Date(Date.UTC(anio, mes - 1, dia));
}

function sumarDiasHabiles(iso: string, dias: number): string {
  const cursor = parseIso(iso);
  let restantes = dias;
  while (restantes > 0) {
    cursor.setUTCDate(cursor.getUTCDate() + 1);
    const dia = cursor.getUTCDay();
    if (dia !== 0 && dia !== 6) {
      restantes -= 1;
    }
  }
  return isoDe(cursor);
}

function claveNotas(cursoId: number, alumnoId: number): string {
  return `${cursoId}|${alumnoId}`;
}

function claveAsistencia(cursoId: number, fecha: string): string {
  return `${cursoId}|${fecha}`;
}

function semillaNotas(cursoId: number, alumnoId: number): NotaAlumno[] {
  const base = 5 + ((cursoId + alumnoId) % 15) / 10;
  const cantidad = 2 + ((cursoId + alumnoId) % 3);
  return Array.from({ length: cantidad }, (_, index) => ({
    numero: index + 1,
    valor: Math.round(Math.min(7, base + ((index * 7) % 9) / 10 - 0.3) * 10) / 10,
  }));
}

@Injectable({ providedIn: 'root' })
export class DocenteAcademicoService {
  private readonly storageKey = 'siga.docente.academico.v1';
  private readonly docenteId = DOCENTE_ACTUAL_ID;
  private readonly estado = signal<Persistencia>(this.cargar());

  readonly cursos: CursoDocente[] = cursosDelDocente(this.docenteId);
  readonly horario = horarioDelDocente(this.docenteId);

  readonly fechaHoy = isoLocal(new Date());
  readonly semestreActual = computed(() => this.semestreDe(this.fechaHoy));

  readonly diaHoy = computed<DiaSemana | null>(() => DIAS_JS[parseIso(this.fechaHoy).getUTCDay()] ?? null);
  readonly clasesDeHoy = computed(() => {
    const dia = this.diaHoy();
    return dia ? this.horario[dia] : [];
  });
  readonly cursosConClaseHoy = computed(() =>
    this.clasesDeHoy().map((clase) => CURSOS_DOCENTE_MOCK.find((curso) => curso.id === clase.cursoId)).filter((curso): curso is CursoDocente => !!curso),
  );

  constructor() {
    effect(() => {
      localStorage.setItem(this.storageKey, JSON.stringify(this.estado()));
    });
  }

  semestreDe(fecha: string): 1 | 2 {
    const mes = parseIso(fecha).getUTCMonth() + 1;
    return mes >= 7 ? 2 : 1;
  }

  hoyLegible(): string {
    return formatearFecha(this.fechaHoy);
  }

  // --- Notas ---

  notasDe(cursoId: number, alumnoId: number): NotaAlumno[] {
    const clave = claveNotas(cursoId, alumnoId);
    return this.estado().notas[clave] ?? semillaNotas(cursoId, alumnoId);
  }

  siguienteNumero(cursoId: number, alumnoId: number): number {
    return this.notasDe(cursoId, alumnoId).length + 1;
  }

  crearNota(cursoId: number, alumnoId: number, valor: number): void {
    const clave = claveNotas(cursoId, alumnoId);
    const actuales = this.notasDe(cursoId, alumnoId);
    this.guardarNotas(clave, [...actuales, { numero: actuales.length + 1, valor }]);
    this.marcarPendiente(`notas|${cursoId}`);
  }

  editarNota(cursoId: number, alumnoId: number, numero: number, valor: number): void {
    const clave = claveNotas(cursoId, alumnoId);
    const actualizadas = this.notasDe(cursoId, alumnoId).map((nota) =>
      nota.numero === numero ? { ...nota, valor } : nota,
    );
    this.guardarNotas(clave, actualizadas);
    this.marcarPendiente(`notas|${cursoId}`);
  }

  eliminarNota(cursoId: number, alumnoId: number, numero: number): void {
    const clave = claveNotas(cursoId, alumnoId);
    const restantes = this.notasDe(cursoId, alumnoId)
      .filter((nota) => nota.numero !== numero)
      .map((nota, index) => ({ ...nota, numero: index + 1 }));
    this.guardarNotas(clave, restantes);
    this.marcarPendiente(`notas|${cursoId}`);
  }

  // --- Asistencia ---

  asistenciaDe(cursoId: number, fecha: string): RegistroAsistencia[] {
    return this.estado().asistencia[claveAsistencia(cursoId, fecha)] ?? [];
  }

  registroDe(cursoId: number, fecha: string, alumnoId: number): RegistroAsistencia | undefined {
    return this.asistenciaDe(cursoId, fecha).find((registro) => registro.alumnoId === alumnoId);
  }

  marcar(cursoId: number, fecha: string, alumnoId: number, estado: EstadoAsistencia): void {
    if (!this.fechaEditable(fecha)) {
      return;
    }
    const clave = claveAsistencia(cursoId, fecha);
    const registro: RegistroAsistencia =
      estado === 'Presente'
        ? { alumnoId, estado, justificacion: 'No aplica' }
        : { alumnoId, estado, justificacion: 'Pendiente', limite: sumarDiasHabiles(fecha, 3) };

    const actuales = this.asistenciaDe(cursoId, fecha).filter((item) => item.alumnoId !== alumnoId);
    this.estado.update((valor) => ({
      ...valor,
      asistencia: { ...valor.asistencia, [clave]: [...actuales, registro] },
    }));
    this.marcarPendiente(`asistencia|${cursoId}|${fecha}`);
  }

  justificacionDe(registro: RegistroAsistencia, fecha: string): Justificacion {
    if (registro.estado === 'Presente') {
      return 'No aplica';
    }
    if (registro.justificacion === 'Pendiente' && registro.limite && this.fechaHoy > registro.limite) {
      return 'No';
    }
    void fecha;
    return registro.justificacion;
  }

  fechaEditable(fecha: string): boolean {
    return this.fechaHoy <= sumarDiasHabiles(fecha, 3);
  }

  // --- Borradores / guardado ---

  pendiente(clave: string): boolean {
    return !this.estado().guardados.includes(clave);
  }

  guardarNotasCurso(cursoId: number): void {
    this.marcarGuardado(`notas|${cursoId}`);
  }

  guardarAsistenciaCurso(cursoId: number, fecha: string): void {
    this.marcarGuardado(`asistencia|${cursoId}|${fecha}`);
  }

  // --- Internos ---

  private guardarNotas(clave: string, notas: NotaAlumno[]): void {
    this.estado.update((valor) => ({
      ...valor,
      notas: { ...valor.notas, [clave]: notas },
    }));
  }

  private marcarPendiente(clave: string): void {
    this.estado.update((valor) => ({
      ...valor,
      guardados: valor.guardados.filter((item) => item !== clave),
    }));
  }

  private marcarGuardado(clave: string): void {
    this.estado.update((valor) => ({
      ...valor,
      guardados: valor.guardados.includes(clave) ? valor.guardados : [...valor.guardados, clave],
    }));
  }

  private cargar(): Persistencia {
    const vacio: Persistencia = { notas: {}, asistencia: {}, guardados: [] };
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) {
      return vacio;
    }
    try {
      const data = JSON.parse(raw) as Partial<Persistencia>;
      return {
        notas: data.notas ?? {},
        asistencia: data.asistencia ?? {},
        guardados: data.guardados ?? [],
      };
    } catch {
      return vacio;
    }
  }
}
