import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { formatearNota, parseNota } from '@siga/academico';
import { Alumno, nombreCompleto } from '@siga/mocks';
import { SeccionCardComponent, SelectComponent, SelectOption } from '@siga/shared-ui';
import { Observable, finalize, map, of } from 'rxjs';
import { DocenteAcademicoService } from '../state/docente-academico.service';
import {
  CursoNotas,
  DocenteNotasService,
  NotaCurso,
  TipoEvaluacion,
} from '../state/docente-notas.service';

interface NuevaEvaluacion {
  nombre: string;
  tipo: TipoEvaluacion;
  ponderacion: number;
}

interface EvaluacionEditable {
  id: number;
  nombre: string;
  tipo: TipoEvaluacion;
  ponderacion: number;
}

const TIPOS: TipoEvaluacion[] = ['FORMATIVA', 'SUMATIVA', 'DIAGNOSTICO'];
const DEBOUNCE_MS = 500;

function claveDe(alumnoId: number, evaluacionId: number): string {
  return `${alumnoId}|${evaluacionId}`;
}

function mensajeDeError(error: unknown, porDefecto: string): string {
  if (error instanceof HttpErrorResponse) {
    const cuerpo = error.error as { message?: string } | null;
    if (cuerpo?.message) {
      return cuerpo.message;
    }
  }
  return porDefecto;
}

@Component({
  selector: 'siga-docente-registrar-notas',
  imports: [SeccionCardComponent, SelectComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Registrar notas</h1>

      @if (estadoCursos() === 'error') {
        <siga-seccion-card title="No se pudieron cargar los cursos">
          <div class="flex flex-col items-center gap-3 py-4 text-center">
            <p class="text-sm text-muted">Revisa la conexión con el BFF e inténtalo nuevamente.</p>
            <button
              type="button"
              (click)="reintentarCursos()"
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

        @if (hayPendientes()) {
          <p class="text-sm text-warn">
            Guardando cambios… espera a que terminen antes de cambiar de curso.
          </p>
        }

        @if (errorCarga()) {
          <siga-seccion-card title="No se pudieron cargar las notas">
            <div class="flex flex-col items-center gap-3 py-4 text-center">
              <p class="text-sm text-muted">El curso no está disponible en este momento.</p>
              <button
                type="button"
                (click)="recargar()"
                class="rounded-lg border border-brand px-4 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
              >
                Reintentar
              </button>
            </div>
          </siga-seccion-card>
        } @else if (cargando()) {
          <siga-seccion-card title="Cargando notas…">
            <p class="py-6 text-center text-sm text-muted">Obteniendo evaluaciones y notas.</p>
          </siga-seccion-card>
        } @else if (cursoNotas(); as cn) {
          <siga-seccion-card [title]="cn.curso + ' · ' + cn.asignatura">
            <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
              <p class="text-sm text-muted">
                Ponderación total:
                <span [class]="sumaPonderaciones() > 100 ? 'text-bad' : 'text-ok'">
                  {{ sumaPonderaciones() }}%
                </span>
              </p>
              <button
                type="button"
                (click)="mostrarNueva.set(!mostrarNueva())"
                class="rounded-lg border border-gold/60 px-3 py-1.5 text-sm font-semibold text-gold transition hover:bg-gold/10"
              >
                Agregar evaluación
              </button>
            </div>

            @if (errorEvaluacion()) {
              <p class="mb-3 rounded-lg bg-bad/10 px-3 py-2 text-sm text-bad">{{ errorEvaluacion() }}</p>
            }

            @if (mostrarNueva()) {
              <div class="mb-4 grid items-end gap-3 rounded-xl bg-surface p-3 sm:grid-cols-[1fr_auto_auto_auto]">
                <label class="flex flex-col gap-1 text-xs text-muted">
                  Nombre
                  <input
                    type="text"
                    [value]="nueva().nombre"
                    (input)="setNombreNueva($event)"
                    class="rounded-lg border border-line bg-panel px-2 py-1.5 text-sm text-ink focus:outline-none"
                  />
                </label>
                <label class="flex flex-col gap-1 text-xs text-muted">
                  Tipo
                  <select
                    [value]="nueva().tipo"
                    (change)="setTipoNueva($event)"
                    class="rounded-lg border border-line bg-panel px-2 py-1.5 text-sm text-ink focus:outline-none"
                  >
                    @for (tipo of tipos; track tipo) {
                      <option [value]="tipo">{{ tipo }}</option>
                    }
                  </select>
                </label>
                <label class="flex flex-col gap-1 text-xs text-muted">
                  Ponderación %
                  <input
                    type="number"
                    min="0"
                    max="100"
                    [disabled]="nueva().tipo !== 'SUMATIVA'"
                    [value]="nueva().tipo === 'SUMATIVA' ? nueva().ponderacion : 0"
                    (input)="setPonderacionNueva($event)"
                    class="w-24 rounded-lg border border-line bg-panel px-2 py-1.5 text-sm text-ink focus:outline-none disabled:opacity-60"
                  />
                  @if (nueva().tipo !== 'SUMATIVA') {
                    <span class="text-[11px] text-muted">Solo SUMATIVA pondera en la nota final.</span>
                  }
                </label>
                <div class="flex gap-2">
                  <button
                    type="button"
                    (click)="agregarEvaluacion()"
                    class="rounded-lg border border-brand px-3 py-1.5 text-sm font-semibold text-brand transition hover:bg-brand/20"
                  >
                    Agregar
                  </button>
                  <button
                    type="button"
                    (click)="cerrarNueva()"
                    class="rounded-lg border border-line px-3 py-1.5 text-sm font-semibold text-ink/80 transition hover:bg-brand/10"
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            }

            @if (cn.evaluaciones.length === 0) {
              <p class="rounded-xl bg-panel px-4 py-6 text-center text-sm text-muted">
                Este curso aún no tiene evaluaciones. Agrega la primera para registrar notas.
              </p>
            } @else {
              <div class="overflow-x-auto">
                <table class="w-full border-collapse text-sm">
                  <thead>
                    <tr>
                      <th
                        class="sticky left-0 z-10 bg-panel px-3 py-2 text-left text-xs font-semibold text-muted"
                      >
                        Alumno
                      </th>
                      @for (evaluacion of cn.evaluaciones; track evaluacion.id) {
                        <th class="min-w-44 border-b border-line px-3 py-2 text-left align-top">
                          <div class="flex flex-col gap-1">
                            <div class="flex items-center gap-1">
                              <input
                                type="text"
                                [value]="evaluacion.nombre"
                                (blur)="editarNombre(evaluacion, $event)"
                                class="w-full rounded-lg border border-line bg-panel px-2 py-1 text-xs font-semibold text-ink focus:outline-none"
                              />
                              <button
                                type="button"
                                (click)="eliminarEvaluacion(evaluacion)"
                                class="text-bad transition hover:text-bad/70"
                                title="Eliminar evaluación"
                              >
                                ×
                              </button>
                            </div>
                            <div class="flex items-center gap-1">
                              <select
                                [value]="evaluacion.tipo"
                                (change)="editarTipo(evaluacion, $event)"
                                class="rounded-lg border border-line bg-panel px-1 py-1 text-xs text-ink focus:outline-none"
                              >
                                @for (tipo of tipos; track tipo) {
                                  <option [value]="tipo">{{ tipo }}</option>
                                }
                              </select>
                              <input
                                type="number"
                                min="0"
                                max="100"
                                [disabled]="evaluacion.tipo !== 'SUMATIVA'"
                                [value]="evaluacion.tipo === 'SUMATIVA' ? evaluacion.ponderacion : 0"
                                (change)="editarPonderacion(evaluacion, $event)"
                                class="w-16 rounded-lg border border-line bg-panel px-1 py-1 text-xs text-ink focus:outline-none disabled:opacity-60"
                              />
                              <span class="text-xs text-muted">%</span>
                            </div>
                            @if (evaluacion.tipo !== 'SUMATIVA') {
                              <span class="text-[11px] text-muted">No pondera en la nota final.</span>
                            }
                            @if (errorDeEvaluacion(evaluacion.id); as mensaje) {
                              <p class="text-xs text-bad">{{ mensaje }}</p>
                            }
                          </div>
                        </th>
                      }
                    </tr>
                  </thead>
                  <tbody>
                    @for (alumno of cn.alumnos; track alumno.id) {
                      <tr class="border-b border-line/60">
                        <td class="sticky left-0 z-10 bg-panel px-3 py-2 font-medium text-ink">
                          {{ nombre(alumno) }}
                        </td>
                        @for (evaluacion of cn.evaluaciones; track evaluacion.id) {
                          <td class="px-3 py-2 align-top">
                            <input
                              type="text"
                              inputmode="decimal"
                              [value]="valorCelda(alumno.id, evaluacion.id)"
                              (input)="onInput(alumno.id, evaluacion.id, $event)"
                              (blur)="onBlur(alumno.id, evaluacion.id)"
                              [class]="claseCelda(alumno.id, evaluacion.id)"
                            />
                            @if (errorDe(alumno.id, evaluacion.id); as mensaje) {
                              <p class="mt-1 text-xs text-bad">
                                {{ mensaje }}
                                <button
                                  type="button"
                                  (click)="reintentar(alumno.id, evaluacion.id)"
                                  class="underline"
                                >
                                  Reintentar
                                </button>
                              </p>
                            }
                            @if (guardandoCelda(alumno.id, evaluacion.id)) {
                              <p class="mt-1 text-xs text-muted">Guardando…</p>
                            }
                          </td>
                        }
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </siga-seccion-card>
        }
      } @else {
        <siga-seccion-card title="Sin cursos">
          <p class="rounded-xl bg-panel px-4 py-6 text-center text-sm text-muted">
            No tienes asignaturas asignadas por el momento.
          </p>
        </siga-seccion-card>
      }
    </div>
  `,
})
export class DocenteRegistrarNotasComponent {
  private readonly academico = inject(DocenteAcademicoService);
  private readonly notasService = inject(DocenteNotasService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly tipos = TIPOS;
  protected readonly cursos = this.academico.cursos;
  protected readonly estadoCursos = this.academico.estadoCursos;
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

  private readonly recarga = signal(0);
  protected readonly cursoNotas = signal<CursoNotas | null>(null);
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal(false);

  protected readonly guardando = signal<ReadonlySet<string>>(new Set());
  protected readonly errores = signal<ReadonlyMap<string, string>>(new Map());
  protected readonly hayPendientes = computed(() => this.guardando().size > 0);

  protected readonly mostrarNueva = signal(false);
  protected readonly nueva = signal<NuevaEvaluacion>({
    nombre: '',
    tipo: 'FORMATIVA',
    ponderacion: 0,
  });
  protected readonly errorEvaluacion = signal('');
  private readonly erroresEvaluacion = signal<ReadonlyMap<number, string>>(new Map());

  private readonly deseado = new Map<string, number | null>();
  private readonly enVuelo = new Set<string>();
  private readonly timers = new Map<string, ReturnType<typeof setTimeout>>();

  constructor() {
    effect(() => {
      const cursos = this.cursos();
      if (cursos.length > 0 && !cursos.some((curso) => curso.id === this.cursoId())) {
        this.cursoId.set(cursos[0].id);
      }
    });

    effect((onCleanup) => {
      const id = this.cursoId();
      this.recarga();
      if (!id) {
        this.cursoNotas.set(null);
        return;
      }
      this.cargando.set(true);
      this.errorCarga.set(false);
      this.deseado.clear();
      this.errores.set(new Map());
      const sub = this.notasService.getCursoNotas(id).subscribe({
        next: (curso) => {
          this.cursoNotas.set(curso);
          this.cargando.set(false);
        },
        error: () => {
          this.errorCarga.set(true);
          this.cargando.set(false);
        },
      });
      onCleanup(() => sub.unsubscribe());
    });

    const antesDeSalir = (event: BeforeUnloadEvent) => {
      if (this.enVuelo.size > 0 || this.deseado.size > 0) {
        event.preventDefault();
      }
    };
    window.addEventListener('beforeunload', antesDeSalir);
    this.destroyRef.onDestroy(() => {
      window.removeEventListener('beforeunload', antesDeSalir);
      this.timers.forEach((timer) => clearTimeout(timer));
    });
  }

  protected nombre(alumno: Alumno): string {
    return nombreCompleto(alumno);
  }

  protected recargar(): void {
    this.recarga.update((valor) => valor + 1);
  }

  protected reintentarCursos(): void {
    this.academico.recargarDatos();
  }

  protected cambiarCurso(value: string | number): void {
    if (this.hayPendientes()) {
      return;
    }
    this.cursoId.set(Number(value));
  }

  // --- Celdas de notas ---

  protected valorCelda(alumnoId: number, evaluacionId: number): string {
    const clave = claveDe(alumnoId, evaluacionId);
    const pendiente = this.deseado.get(clave);
    if (pendiente !== undefined) {
      return pendiente === null ? '' : formatearNota(pendiente);
    }
    const nota = this.nota(alumnoId, evaluacionId);
    return nota ? formatearNota(nota.score) : '';
  }

  protected claseCelda(alumnoId: number, evaluacionId: number): string {
    const clave = claveDe(alumnoId, evaluacionId);
    const base =
      'w-20 rounded-lg border bg-panel px-2 py-1 text-sm text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand';
    if (this.errores().has(clave)) {
      return `${base} border-bad`;
    }
    return `${base} border-line`;
  }

  protected guardandoCelda(alumnoId: number, evaluacionId: number): boolean {
    return this.guardando().has(claveDe(alumnoId, evaluacionId));
  }

  protected errorDe(alumnoId: number, evaluacionId: number): string | undefined {
    return this.errores().get(claveDe(alumnoId, evaluacionId));
  }

  protected onInput(alumnoId: number, evaluacionId: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const clave = claveDe(alumnoId, evaluacionId);
    if (input.value.includes('.')) {
      input.value = input.value.replace('.', ',');
    }
    const texto = input.value.trim();
    this.limpiarError(clave);

    const valor = texto === '' ? null : parseNota(texto);
    if (texto !== '' && valor === null) {
      this.marcarError(clave, 'Nota entre 1,0 y 7,0');
      return;
    }

    this.deseado.set(clave, valor);
    const timer = this.timers.get(clave);
    if (timer) {
      clearTimeout(timer);
    }
    this.timers.set(
      clave,
      setTimeout(() => {
        this.timers.delete(clave);
        this.guardarCelda(clave);
      }, DEBOUNCE_MS),
    );
  }

  protected onBlur(alumnoId: number, evaluacionId: number): void {
    const clave = claveDe(alumnoId, evaluacionId);
    const timer = this.timers.get(clave);
    if (timer) {
      clearTimeout(timer);
      this.timers.delete(clave);
    }
    // Si la celda quedo en error, el reintento es manual (boton Reintentar)
    // para no duplicar la peticion al salir del input.
    if (this.errores().has(clave)) {
      return;
    }
    this.guardarCelda(clave);
  }

  protected reintentar(alumnoId: number, evaluacionId: number): void {
    this.guardarCelda(claveDe(alumnoId, evaluacionId));
  }

  private guardarCelda(clave: string): void {
    if (!this.deseado.has(clave) || this.enVuelo.has(clave)) {
      return;
    }
    const [alumnoId, evaluacionId] = clave.split('|').map(Number);
    const valor = this.deseado.get(clave) ?? null;
    const actual = this.nota(alumnoId, evaluacionId);
    if ((actual?.score ?? null) === valor) {
      this.deseado.delete(clave);
      return;
    }
    if (valor === null && !actual) {
      this.deseado.delete(clave);
      return;
    }

    const operacion$: Observable<NotaCurso | null> = actual
      ? valor === null
        ? this.notasService.eliminarNota(actual.id).pipe(map(() => null))
        : this.notasService.editarNota(actual.id, valor)
      : this.notasService.crearNota(alumnoId, evaluacionId, valor as number);

    this.enVuelo.add(clave);
    this.marcarGuardando(clave, true);
    let fallo = false;
    operacion$
      .pipe(
        finalize(() => {
          this.enVuelo.delete(clave);
          this.marcarGuardando(clave, false);
          if (fallo) {
            return;
          }
          const pendiente = this.deseado.get(clave);
          if (pendiente !== undefined && pendiente !== valor) {
            this.guardarCelda(clave);
          } else {
            this.deseado.delete(clave);
          }
        }),
      )
      .subscribe({
        next: (nota) => {
          this.aplicarNota(alumnoId, evaluacionId, nota);
          this.limpiarError(clave);
        },
        error: (error: unknown) => {
          if (error instanceof HttpErrorResponse && error.status === 409) {
            // Otro cliente ya creo la nota: se recarga el curso para reflejarlo.
            this.deseado.delete(clave);
            this.limpiarError(clave);
            this.recargar();
            return;
          }
          fallo = true;
          this.marcarError(clave, mensajeDeError(error, 'No se pudo guardar'));
        },
      });
  }

  private nota(alumnoId: number, evaluacionId: number): NotaCurso | undefined {
    return this.cursoNotas()
      ?.alumnos.find((alumno) => alumno.id === alumnoId)
      ?.notas.find((nota) => nota.idEvaluacion === evaluacionId);
  }

  private aplicarNota(alumnoId: number, evaluacionId: number, nota: NotaCurso | null): void {
    const curso = this.cursoNotas();
    if (!curso) {
      return;
    }
    this.cursoNotas.set({
      ...curso,
      alumnos: curso.alumnos.map((alumno) =>
        alumno.id !== alumnoId
          ? alumno
          : {
              ...alumno,
              notas: nota
                ? [...alumno.notas.filter((item) => item.idEvaluacion !== evaluacionId), nota]
                : alumno.notas.filter((item) => item.idEvaluacion !== evaluacionId),
            },
      ),
    });
  }

  private marcarGuardando(clave: string, activo: boolean): void {
    this.guardando.update((actual) => {
      const copia = new Set(actual);
      if (activo) {
        copia.add(clave);
      } else {
        copia.delete(clave);
      }
      return copia;
    });
  }

  private marcarError(clave: string, mensaje: string): void {
    this.errores.update((actual) => new Map(actual).set(clave, mensaje));
  }

  private limpiarError(clave: string): void {
    if (!this.errores().has(clave)) {
      return;
    }
    this.errores.update((actual) => {
      const copia = new Map(actual);
      copia.delete(clave);
      return copia;
    });
  }

  // --- Evaluaciones ---

  protected sumaPonderaciones(exceptoId?: number): number {
    return (this.cursoNotas()?.evaluaciones ?? [])
      .filter((evaluacion) => evaluacion.tipo === 'SUMATIVA')
      .filter((evaluacion) => evaluacion.id !== exceptoId)
      .reduce((total, evaluacion) => total + (evaluacion.ponderacion ?? 0), 0);
  }

  protected errorDeEvaluacion(id: number): string | undefined {
    return this.erroresEvaluacion().get(id);
  }

  protected editarNombre(evaluacion: EvaluacionEditable, event: Event): void {
    const input = event.target as HTMLInputElement;
    const nombre = input.value.trim();
    this.limpiarErrorEvaluacion(evaluacion.id);
    if (nombre.length < 2) {
      this.marcarErrorEvaluacion(evaluacion.id, 'El nombre debe tener al menos 2 caracteres.');
      input.value = evaluacion.nombre;
      return;
    }
    if (nombre === evaluacion.nombre) {
      input.value = evaluacion.nombre;
      return;
    }
    this.actualizarEvaluacion(evaluacion, { ...this.requestDe(evaluacion), nombre }, () => {
      input.value = evaluacion.nombre;
    });
  }

  protected editarTipo(evaluacion: EvaluacionEditable, event: Event): void {
    const select = event.target as HTMLSelectElement;
    const tipo = select.value as TipoEvaluacion;
    this.limpiarErrorEvaluacion(evaluacion.id);
    if (tipo === evaluacion.tipo) {
      return;
    }
    if (tipo === 'SUMATIVA' && (evaluacion.ponderacion ?? 0) <= 0) {
      // El backend exige ponderación > 0: se habilita el % y el cambio se
      // persiste cuando el docente asigne la ponderación.
      this.marcarErrorEvaluacion(
        evaluacion.id,
        'Asigna una ponderación mayor que 0% para guardar la evaluación SUMATIVA.',
      );
      this.reemplazarEvaluacionLocal({ ...evaluacion, tipo, ponderacion: 0 });
      return;
    }
    this.actualizarEvaluacion(evaluacion, { ...this.requestDe(evaluacion), tipo }, () => {
      select.value = evaluacion.tipo;
    });
  }

  protected editarPonderacion(evaluacion: EvaluacionEditable, event: Event): void {
    const input = event.target as HTMLInputElement;
    if (evaluacion.tipo !== 'SUMATIVA') {
      input.value = '0';
      return;
    }
    const ponderacion = Number(input.value);
    this.limpiarErrorEvaluacion(evaluacion.id);
    if (Number.isNaN(ponderacion) || ponderacion <= 0 || ponderacion > 100) {
      this.marcarErrorEvaluacion(evaluacion.id, 'La ponderación de una SUMATIVA debe ser mayor que 0 y hasta 100.');
      input.value = String(evaluacion.ponderacion);
      return;
    }
    if (this.sumaPonderaciones(evaluacion.id) + ponderacion > 100) {
      this.marcarErrorEvaluacion(evaluacion.id, 'La suma de ponderaciones no puede superar 100%.');
      input.value = String(evaluacion.ponderacion);
      return;
    }
    if (ponderacion === evaluacion.ponderacion) {
      input.value = String(evaluacion.ponderacion);
      return;
    }
    this.actualizarEvaluacion(evaluacion, { ...this.requestDe(evaluacion), ponderacion }, () => {
      input.value = String(evaluacion.ponderacion);
    });
  }

  protected eliminarEvaluacion(evaluacion: { id: number; nombre: string }): void {
    if (!window.confirm(`¿Eliminar la evaluación "${evaluacion.nombre}" y sus notas?`)) {
      return;
    }
    this.limpiarErrorEvaluacion(evaluacion.id);
    this.notasService.eliminarEvaluacion(evaluacion.id).subscribe({
      next: () => {
        const curso = this.cursoNotas();
        if (!curso) {
          return;
        }
        this.cursoNotas.set({
          ...curso,
          evaluaciones: curso.evaluaciones.filter((item) => item.id !== evaluacion.id),
          alumnos: curso.alumnos.map((alumno) => ({
            ...alumno,
            notas: alumno.notas.filter((nota) => nota.idEvaluacion !== evaluacion.id),
          })),
        });
      },
      error: (error: unknown) =>
        this.marcarErrorEvaluacion(
          evaluacion.id,
          mensajeDeError(error, 'No se pudo eliminar la evaluación.'),
        ),
    });
  }

  protected setNombreNueva(event: Event): void {
    this.nueva.update((actual) => ({ ...actual, nombre: (event.target as HTMLInputElement).value }));
  }

  protected setTipoNueva(event: Event): void {
    const tipo = (event.target as HTMLSelectElement).value as TipoEvaluacion;
    this.nueva.update((actual) => ({
      ...actual,
      tipo,
      ponderacion: tipo === 'SUMATIVA' ? actual.ponderacion : 0,
    }));
  }

  protected setPonderacionNueva(event: Event): void {
    const valor = Number((event.target as HTMLInputElement).value);
    this.nueva.update((actual) => ({
      ...actual,
      ponderacion: actual.tipo === 'SUMATIVA' ? valor : 0,
    }));
  }

  protected cerrarNueva(): void {
    this.mostrarNueva.set(false);
    this.errorEvaluacion.set('');
    this.nueva.set({ nombre: '', tipo: 'FORMATIVA', ponderacion: 0 });
  }

  protected agregarEvaluacion(): void {
    const nueva = this.nueva();
    const nombre = nueva.nombre.trim();
    if (nombre.length < 2) {
      this.errorEvaluacion.set('El nombre debe tener al menos 2 caracteres.');
      return;
    }
    const ponderacion = nueva.tipo === 'SUMATIVA' ? nueva.ponderacion : 0;
    if (nueva.tipo === 'SUMATIVA' && (Number.isNaN(ponderacion) || ponderacion <= 0 || ponderacion > 100)) {
      this.errorEvaluacion.set('La ponderación de una SUMATIVA debe ser mayor que 0 y hasta 100.');
      return;
    }
    if (this.sumaPonderaciones() + ponderacion > 100) {
      this.errorEvaluacion.set('La suma de ponderaciones no puede superar 100%.');
      return;
    }
    this.errorEvaluacion.set('');
    this.notasService
      .crearEvaluacion(this.cursoId(), { nombre, tipo: nueva.tipo, ponderacion })
      .subscribe({
        next: (evaluacion) => {
          const curso = this.cursoNotas();
          if (curso) {
            this.cursoNotas.set({ ...curso, evaluaciones: [...curso.evaluaciones, evaluacion] });
          }
          this.cerrarNueva();
        },
        error: (error: unknown) =>
          this.errorEvaluacion.set(mensajeDeError(error, 'No se pudo crear la evaluación.')),
      });
  }

  private requestDe(evaluacion: {
    nombre: string;
    tipo: TipoEvaluacion;
    ponderacion: number;
  }): { nombre: string; tipo: TipoEvaluacion; ponderacion: number } {
    return {
      nombre: evaluacion.nombre,
      tipo: evaluacion.tipo,
      ponderacion: evaluacion.ponderacion,
    };
  }

  private reemplazarEvaluacionLocal(evaluacion: EvaluacionEditable): void {
    const curso = this.cursoNotas();
    if (!curso) {
      return;
    }
    this.cursoNotas.set({
      ...curso,
      evaluaciones: curso.evaluaciones.map((item) =>
        item.id === evaluacion.id ? { ...item, ...evaluacion } : item,
      ),
    });
  }

  private actualizarEvaluacion(
    evaluacion: { id: number },
    request: { nombre: string; tipo: TipoEvaluacion; ponderacion: number },
    revertir: () => void = () => undefined,
  ): void {
    this.limpiarErrorEvaluacion(evaluacion.id);
    this.notasService.editarEvaluacion(evaluacion.id, request).subscribe({
      next: (actualizada) => {
        const curso = this.cursoNotas();
        if (!curso) {
          return;
        }
        this.cursoNotas.set({
          ...curso,
          evaluaciones: curso.evaluaciones.map((item) =>
            item.id === actualizada.id ? { ...item, ...actualizada } : item,
          ),
        });
      },
      error: (error: unknown) => {
        this.marcarErrorEvaluacion(
          evaluacion.id,
          mensajeDeError(error, 'No se pudo actualizar la evaluación.'),
        );
        revertir();
      },
    });
  }

  private marcarErrorEvaluacion(id: number, mensaje: string): void {
    this.erroresEvaluacion.update((actual) => new Map(actual).set(id, mensaje));
  }

  private limpiarErrorEvaluacion(id: number): void {
    if (!this.erroresEvaluacion().has(id)) {
      return;
    }
    this.erroresEvaluacion.update((actual) => {
      const copia = new Map(actual);
      copia.delete(id);
      return copia;
    });
  }
}
