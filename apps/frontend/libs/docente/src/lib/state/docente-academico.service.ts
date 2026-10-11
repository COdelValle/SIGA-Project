import { Injectable, computed, inject } from '@angular/core';
import { APP_CONFIG, EstadoRemoto, recursoRemoto } from '@siga/core';
import { DiaSemana, formatearFecha } from '@siga/academico';
import {
  ClaseDocente,
  CursoDocente,
  DOCENTE_ACTUAL_ID,
  horarioDelDocente,
  cursosDelDocente,
} from '@siga/mocks';
import { Subject, startWith, switchMap } from 'rxjs';
import { DocenteDatosService } from './docente-datos.service';

const DIAS_JS: Record<number, DiaSemana> = {
  1: 'Lunes',
  2: 'Martes',
  3: 'Miércoles',
  4: 'Jueves',
  5: 'Viernes',
};

const HORARIO_VACIO: Record<DiaSemana, ClaseDocente[]> = {
  Lunes: [],
  Martes: [],
  Miércoles: [],
  Jueves: [],
  Viernes: [],
};

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

/**
 * Calendario del docente y sus cursos/horario reales. En modo demo (`useMocks`)
 * usa los mocks; en modo real los errores quedan a la vista (estado remoto).
 */
@Injectable({ providedIn: 'root' })
export class DocenteAcademicoService {
  private readonly config = inject(APP_CONFIG);
  private readonly docenteId = DOCENTE_ACTUAL_ID;
  private readonly datosService = inject(DocenteDatosService);

  private readonly refresco = new Subject<void>();

  private readonly cursosRemotos = recursoRemoto(
    this.refresco.pipe(
      startWith(void 0),
      switchMap(() => this.datosService.getCursos()),
    ),
  );
  private readonly horarioRemoto = recursoRemoto(
    this.refresco.pipe(
      startWith(void 0),
      switchMap(() => this.datosService.getHorario()),
    ),
  );

  readonly estadoCursos = computed<EstadoRemoto>(() =>
    this.config.useMocks ? 'listo' : this.cursosRemotos().estado,
  );

  readonly estadoHorario = computed<EstadoRemoto>(() =>
    this.config.useMocks ? 'listo' : this.horarioRemoto().estado,
  );

  /** Cursos del docente (BFF en real, mocks en modo demo). */
  readonly cursos = computed<CursoDocente[]>(() =>
    this.config.useMocks
      ? cursosDelDocente(this.docenteId)
      : this.cursosRemotos().dato ?? [],
  );

  /** Horario semanal del docente (BFF en real, mocks en modo demo). */
  readonly horario = computed<Record<DiaSemana, ClaseDocente[]>>(() =>
    this.config.useMocks
      ? horarioDelDocente(this.docenteId)
      : this.horarioRemoto().dato ?? HORARIO_VACIO,
  );

  readonly fechaHoy = isoLocal(new Date());
  readonly semestreActual = computed(() => this.semestreDe(this.fechaHoy));

  readonly diaHoy = computed<DiaSemana | null>(
    () => DIAS_JS[parseIso(this.fechaHoy).getUTCDay()] ?? null,
  );
  readonly clasesDeHoy = computed(() => {
    const dia = this.diaHoy();
    return dia ? this.horario()[dia] : [];
  });
  readonly cursosConClaseHoy = computed(() =>
    this.clasesDeHoy()
      .map((clase) => this.cursos().find((curso) => curso.id === clase.cursoId))
      .filter((curso): curso is CursoDocente => !!curso),
  );

  /** Invalida el cache y reintenta cursos/horario. */
  recargarDatos(): void {
    this.datosService.invalidar();
    this.refresco.next();
  }

  semestreDe(fecha: string): 1 | 2 {
    const mes = parseIso(fecha).getUTCMonth() + 1;
    return mes >= 7 ? 2 : 1;
  }

  hoyLegible(): string {
    return formatearFecha(this.fechaHoy);
  }
}
