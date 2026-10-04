import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { AsistenciaDTO, AsistenciaService, EstadoAsistenciaDTO } from '@siga/academico';
import { Alumno, nombreCompleto } from '@siga/mocks';
import { SeccionCardComponent, SelectComponent, SelectOption } from '@siga/shared-ui';
import { DocenteAcademicoService } from '../state/docente-academico.service';

const ETIQUETA_JUSTIFICACION: Record<string, string> = {
  SI: 'Justificada',
  NO: 'No justificada',
  PENDIENTE: 'Pendiente',
  NO_APLICA: 'No aplica',
};

const ETIQUETA_ESTADO: Record<string, string> = {
  PRESENTE: 'Presente',
  AUSENTE: 'Ausente',
  ATRASADO: 'Atrasado',
};

@Component({
  selector: 'siga-docente-registrar-asistencias',
  imports: [SeccionCardComponent, SelectComponent],
  template: `
    <div class="mx-auto flex max-w-5xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Registrar asistencias</h1>

      <p class="text-sm text-muted">
        Hoy es {{ diaHoy() }} {{ hoyLegible }} · Semestre {{ semestre() }}
      </p>

      @if (estadoHorario() === 'error') {
        <siga-seccion-card title="No se pudo cargar el horario">
          <div class="flex flex-col items-center gap-3 py-4 text-center">
            <p class="text-sm text-muted">Revisa la conexión con el BFF e inténtalo nuevamente.</p>
            <button
              type="button"
              (click)="recargarHorario()"
              class="rounded-lg border border-brand px-4 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
            >
              Reintentar
            </button>
          </div>
        </siga-seccion-card>
      } @else if (cursos().length > 0) {
        <div class="w-full max-w-md">
          <siga-select
            [options]="opcionesCurso()"
            [value]="cursoId()"
            [disabled]="hayPendientes()"
            ariaLabel="Seleccionar curso"
            (valueChange)="cambiarCurso($event)"
          />
        </div>

        <siga-seccion-card [title]="(curso()?.nombre ?? '') + ' · ' + (curso()?.asignatura ?? '')">
          @if (errorCarga()) {
            <div class="flex flex-col items-center gap-3 py-4 text-center">
              <p class="text-sm text-muted">No se pudieron cargar las asistencias de hoy.</p>
              <button
                type="button"
                (click)="recargarAsistencias()"
                class="rounded-lg border border-brand px-4 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
              >
                Reintentar
              </button>
            </div>
          } @else if (cargando()) {
            <p class="py-6 text-center text-sm text-muted">Cargando asistencias…</p>
          } @else {
            <ul class="flex flex-col gap-2">
              @for (alumno of alumnos(); track alumno.id) {
                <li
                  class="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-surface px-4 py-3"
                >
                  <div>
                    <p class="text-sm font-medium text-ink">{{ nombre(alumno) }}</p>
                    @if (registro(alumno.id); as reg) {
                      <p class="text-xs text-muted">
                        {{ etiquetaEstado(reg.estado) }} · {{ detalle(reg) }}
                      </p>
                    } @else {
                      <p class="text-xs text-muted">Sin marcar</p>
                    }
                    @if (errorDe(alumno.id); as mensaje) {
                      <p class="mt-1 text-xs text-bad">
                        {{ mensaje }}
                        <button type="button" (click)="reintentar(alumno.id)" class="underline">
                          Reintentar
                        </button>
                      </p>
                    }
                  </div>

                  <div class="flex items-center gap-2">
                    @if (guardandoCelda(alumno.id)) {
                      <span class="text-xs text-muted">Guardando…</span>
                    }
                    <button
                      type="button"
                      (click)="marcar(alumno.id, 'PRESENTE')"
                      class="rounded-lg border px-3 py-1.5 text-sm font-semibold transition"
                      [class]="clase(alumno.id, 'PRESENTE')"
                    >
                      Presente
                    </button>
                    <button
                      type="button"
                      (click)="marcar(alumno.id, 'AUSENTE')"
                      class="rounded-lg border px-3 py-1.5 text-sm font-semibold transition"
                      [class]="clase(alumno.id, 'AUSENTE')"
                    >
                      Ausente
                    </button>
                  </div>
                </li>
              }
            </ul>
          }
        </siga-seccion-card>
      } @else {
        <siga-seccion-card title="Sin clases hoy">
          <p class="rounded-xl bg-panel px-4 py-6 text-center text-sm text-muted">
            Hoy no tienes clases programadas, por lo que no hay asistencia por registrar.
          </p>
        </siga-seccion-card>
      }
    </div>
  `,
})
export class DocenteRegistrarAsistenciasComponent {
  private readonly academico = inject(DocenteAcademicoService);
  private readonly asistenciaService = inject(AsistenciaService);

  protected readonly cursos = this.academico.cursosConClaseHoy;
  protected readonly estadoHorario = this.academico.estadoHorario;
  protected readonly opcionesCurso = computed<SelectOption[]>(() =>
    this.cursos().map((curso) => ({
      value: curso.id,
      label: `${curso.nombre} · ${curso.asignatura}`,
    })),
  );
  protected readonly cursoId = signal(0);
  protected readonly curso = computed(() =>
    this.cursos().find((item) => item.id === this.cursoId()),
  );
  protected readonly alumnos = computed(() => this.curso()?.alumnos ?? []);
  protected readonly diaHoy = this.academico.diaHoy;
  protected readonly fechaHoy = this.academico.fechaHoy;
  protected readonly hoyLegible = this.academico.hoyLegible();
  protected readonly semestre = this.academico.semestreActual;

  private readonly recargaAsistencias = signal(0);
  private readonly registros = signal<ReadonlyMap<number, AsistenciaDTO>>(new Map());
  private readonly guardando = signal<ReadonlySet<number>>(new Set());
  private readonly errores = signal<ReadonlyMap<number, string>>(new Map());
  private readonly intentos = new Map<number, EstadoAsistenciaDTO>();
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal(false);
  protected readonly hayPendientes = computed(() => this.guardando().size > 0);

  constructor() {
    effect(() => {
      const cursos = this.cursos();
      if (cursos.length > 0 && !cursos.some((curso) => curso.id === this.cursoId())) {
        this.cursoId.set(cursos[0].id);
      }
    });

    effect((onCleanup) => {
      const id = this.cursoId();
      this.recargaAsistencias();
      if (!id) {
        this.registros.set(new Map());
        return;
      }
      this.cargando.set(true);
      this.errorCarga.set(false);
      this.errores.set(new Map());
      const sub = this.asistenciaService
        .getAsistenciasAsignatura(id, this.fechaHoy)
        .subscribe({
          next: (asistencias) => {
            this.registros.set(new Map(asistencias.map((item) => [item.idEstudiante, item])));
            this.cargando.set(false);
          },
          error: () => {
            this.errorCarga.set(true);
            this.cargando.set(false);
          },
        });
      onCleanup(() => sub.unsubscribe());
    });
  }

  protected nombre(alumno: Alumno): string {
    return nombreCompleto(alumno);
  }

  protected recargarHorario(): void {
    this.academico.recargarDatos();
  }

  protected recargarAsistencias(): void {
    this.recargaAsistencias.update((valor) => valor + 1);
  }

  protected cambiarCurso(value: string | number): void {
    if (this.hayPendientes()) {
      return;
    }
    this.cursoId.set(Number(value));
  }

  protected registro(alumnoId: number): AsistenciaDTO | undefined {
    return this.registros().get(alumnoId);
  }

  protected etiquetaEstado(estado: string): string {
    return ETIQUETA_ESTADO[estado] ?? estado;
  }

  protected detalle(registro: AsistenciaDTO): string {
    return ETIQUETA_JUSTIFICACION[registro.justificacion] ?? registro.justificacion;
  }

  protected clase(alumnoId: number, estado: EstadoAsistenciaDTO): string {
    const registro = this.registro(alumnoId);
    if (registro?.estado !== estado) {
      return 'border-line text-ink/80 hover:bg-brand/10 hover:text-brand';
    }
    return estado === 'PRESENTE' ? 'border-ok bg-ok/15 text-ok' : 'border-bad bg-bad/15 text-bad';
  }

  protected marcar(alumnoId: number, estado: EstadoAsistenciaDTO): void {
    if (this.guardando().has(alumnoId)) {
      return;
    }
    const actual = this.registro(alumnoId);
    if (actual?.estado === estado) {
      return;
    }
    const justificacion: AsistenciaDTO['justificacion'] =
      estado === 'PRESENTE' ? 'NO_APLICA' : 'PENDIENTE';

    this.intentos.set(alumnoId, estado);
    this.marcarGuardando(alumnoId, true);
    this.limpiarError(alumnoId);
    const operacion$ = actual
      ? this.asistenciaService.actualizar(actual.id, {
          justificacion,
          observacion: actual.observacion,
          estado,
        })
      : this.asistenciaService.registrar({
          idEstudiante: alumnoId,
          idCursoAsignatura: this.cursoId(),
          fecha: this.fechaHoy,
          estado,
        });

    operacion$.subscribe({
      next: (guardada) => {
        this.registros.update((actuales) => new Map(actuales).set(alumnoId, guardada));
        this.marcarGuardando(alumnoId, false);
      },
      error: (error: unknown) => {
        this.marcarGuardando(alumnoId, false);
        if (error instanceof HttpErrorResponse && error.status === 409) {
          // Otro cliente ya registro la asistencia: se recarga la lista.
          this.limpiarError(alumnoId);
          this.recargarAsistencias();
          return;
        }
        const mensaje =
          error instanceof HttpErrorResponse && (error.error as { message?: string })?.message
            ? ((error.error as { message?: string }).message as string)
            : 'No se pudo guardar';
        this.marcarError(alumnoId, mensaje);
      },
    });
  }

  protected guardandoCelda(alumnoId: number): boolean {
    return this.guardando().has(alumnoId);
  }

  protected errorDe(alumnoId: number): string | undefined {
    return this.errores().get(alumnoId);
  }

  protected reintentar(alumnoId: number): void {
    const estado = this.intentos.get(alumnoId) ?? 'PRESENTE';
    this.limpiarError(alumnoId);
    this.marcar(alumnoId, estado);
  }

  private marcarGuardando(alumnoId: number, activo: boolean): void {
    this.guardando.update((actuales) => {
      const copia = new Set(actuales);
      if (activo) {
        copia.add(alumnoId);
      } else {
        copia.delete(alumnoId);
      }
      return copia;
    });
  }

  private marcarError(alumnoId: number, mensaje: string): void {
    this.errores.update((actuales) => new Map(actuales).set(alumnoId, mensaje));
  }

  private limpiarError(alumnoId: number): void {
    if (!this.errores().has(alumnoId)) {
      return;
    }
    this.errores.update((actuales) => {
      const copia = new Map(actuales);
      copia.delete(alumnoId);
      return copia;
    });
  }
}
