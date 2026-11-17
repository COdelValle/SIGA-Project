import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { SeccionCardComponent, SelectComponent, SelectOption } from '@siga/shared-ui';
import { Observable, Subscription, catchError, forkJoin, map, of, switchMap, take, tap, timer } from 'rxjs';
import {
  CursoReporte,
  EstudianteReporte,
  FiltrosReporte,
  IndicadorAsistencia,
  IndicadorRendimiento,
  NivelRiesgo,
} from '../models/reporte.model';
import { ReporteService } from '../state/reporte.service';

interface ResultadoCarga {
  asistencia: IndicadorAsistencia[];
  rendimiento: IndicadorRendimiento[];
  errorAsistencia: boolean;
  errorRendimiento: boolean;
  errorRango: boolean;
}

const MES_ACTUAL = (): { desde: string; hasta: string } => {
  const hoy = new Date();
  const desde = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
  return { desde: iso(desde), hasta: iso(hoy) };
};

function iso(fecha: Date): string {
  const anio = fecha.getFullYear();
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${anio}-${mes}-${dia}`;
}

@Component({
  selector: 'siga-reportes-dashboard',
  imports: [SeccionCardComponent, SelectComponent, DatePipe, DecimalPipe],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <div class="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Reportes académicos</h1>
          <p class="mt-1 text-sm text-muted">
            Indicadores de asistencia y rendimiento calculados por el BFF.
          </p>
        </div>
        <span class="rounded-full border border-gold/60 px-3 py-1 text-xs font-semibold text-gold">
          Actualización automática cada 15 s
        </span>
      </div>

      <siga-seccion-card title="Filtros">
        <div class="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <label class="flex flex-col gap-1 text-sm text-muted">
            Curso / asignatura
            <siga-select
              [options]="cursosOpciones()"
              [value]="idCurso()"
              ariaLabel="Seleccionar curso"
              (valueChange)="cambiarCurso($event)"
            />
          </label>
          <label class="flex flex-col gap-1 text-sm text-muted">
            Estudiante
            <siga-select
              [options]="estudiantesOpciones()"
              [value]="idEstudiante() ?? 0"
              ariaLabel="Seleccionar estudiante"
              [disabled]="idCurso() === null"
              (valueChange)="cambiarEstudiante($event)"
            />
          </label>
          <label class="flex flex-col gap-1 text-sm text-muted">
            Desde
            <input
              type="date"
              class="rounded-xl border border-line bg-panel px-4 py-3 text-base text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
              [value]="desde()"
              (change)="cambiarDesde($any($event.target).value)"
            />
          </label>
          <label class="flex flex-col gap-1 text-sm text-muted">
            Hasta
            <input
              type="date"
              class="rounded-xl border border-line bg-panel px-4 py-3 text-base text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
              [value]="hasta()"
              (change)="cambiarHasta($any($event.target).value)"
            />
          </label>
        </div>

        <div class="mt-4 flex flex-wrap items-center gap-3 text-sm">
          <button
            type="button"
            class="rounded-lg border border-brand px-4 py-2 font-semibold text-brand transition hover:bg-brand/20"
            (click)="recargar()"
          >
            Actualizar ahora
          </button>
          <button
            type="button"
            class="rounded-lg border border-line px-4 py-2 font-semibold text-ink transition hover:border-gold hover:text-gold"
            (click)="limpiar()"
          >
            Limpiar
          </button>
          @if (actualizando()) {
            <span class="text-muted" aria-live="polite">Actualizando…</span>
          }
          @if (ultimaActualizacion(); as ultima) {
            <span class="text-muted" aria-live="polite">
              Última actualización: {{ ultima | date: 'dd/MM/yyyy HH:mm:ss' }}
            </span>
          }
        </div>

        @if (rangoInvalido()) {
          <p class="mt-3 text-sm text-bad">La fecha “desde” no puede ser posterior a “hasta”.</p>
        }
      </siga-seccion-card>

      @if (cargando()) {
        <div class="rounded-2xl bg-panel p-8 shadow-lg">
          <div class="flex items-center gap-3 text-muted" aria-live="polite">
            <span class="h-5 w-5 animate-spin rounded-full border-2 border-gold border-t-transparent"></span>
            Cargando indicadores…
          </div>
        </div>
      } @else if (error(); as mensaje) {
        <siga-seccion-card title="No se pudieron cargar los reportes">
          <div class="flex flex-col items-center gap-3 py-4 text-center">
            <p class="text-sm text-muted">{{ mensaje }}</p>
            <button
              type="button"
              class="rounded-lg border border-brand px-4 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
              (click)="recargar()"
            >
              Reintentar
            </button>
          </div>
        </siga-seccion-card>
      } @else {
        @if (errorParcial(); as mensaje) {
          <p class="rounded-xl border border-warn/60 bg-warn/10 px-4 py-3 text-sm text-warn" aria-live="polite">
            {{ mensaje }}
          </p>
        }

        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <article class="rounded-2xl bg-panel p-5 shadow-lg">
            <p class="text-sm text-muted">Estudiantes con datos</p>
            <p class="mt-1 text-3xl font-bold text-brand">{{ totalEstudiantes() }}</p>
          </article>
          <article class="rounded-2xl bg-panel p-5 shadow-lg">
            <p class="text-sm text-muted">Inasistencias acumuladas</p>
            <p class="mt-1 text-3xl font-bold text-bad">{{ totalInasistencias() }}</p>
          </article>
          <article class="rounded-2xl bg-panel p-5 shadow-lg">
            <p class="text-sm text-muted">Alertas críticas</p>
            <p class="mt-1 text-3xl font-bold text-warn">{{ alertasCriticas() }}</p>
          </article>
          <article class="rounded-2xl bg-panel p-5 shadow-lg">
            <p class="text-sm text-muted">Promedio general</p>
            <p class="mt-1 text-3xl font-bold text-ok">{{ promedioGeneral() | number: '1.1-1' }}</p>
          </article>
        </div>

        @if (!hayDatos()) {
          <siga-seccion-card title="Sin datos">
            <p class="rounded-xl bg-panel px-4 py-6 text-center text-sm text-muted">
              No hay indicadores para los filtros seleccionados.
            </p>
          </siga-seccion-card>
        } @else {
          <siga-seccion-card title="Asistencia">
            <div class="overflow-x-auto rounded-xl border border-gold/60">
              <table class="w-full border-collapse text-left text-sm">
                <thead>
                  <tr class="bg-bar text-gold">
                    <th class="px-4 py-3 font-semibold">Estudiante</th>
                    <th class="px-4 py-3 font-semibold">Curso / asignatura</th>
                    <th class="px-4 py-3 font-semibold">Fecha</th>
                    <th class="px-4 py-3 text-center font-semibold">% inasistencia</th>
                    <th class="px-4 py-3 text-center font-semibold">Inasistencias</th>
                    <th class="px-4 py-3 text-center font-semibold">Riesgo</th>
                    <th class="px-4 py-3 font-semibold">Actualizado</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of asistenciaVisible(); track item.idEstudiante + '-' + item.idCursoAsignatura + '-' + item.fecha) {
                    <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                      <td class="px-4 py-3">{{ nombreEstudiante(item.idEstudiante) }}</td>
                      <td class="px-4 py-3">{{ nombreCurso(item.idCursoAsignatura) }}</td>
                      <td class="px-4 py-3">{{ item.fecha | date: 'dd/MM/yyyy' }}</td>
                      <td [class]="'px-4 py-3 text-center font-semibold ' + clasePorcentaje(item.porcentajeInasistencia)">
                        {{ item.porcentajeInasistencia | number: '1.1-1' }}%
                      </td>
                      <td class="px-4 py-3 text-center">{{ item.totalInasistencias }} / {{ item.totalRegistros }}</td>
                      <td class="px-4 py-3 text-center">
                        <span [class]="'rounded-full px-2 py-1 text-xs font-semibold ' + claseRiesgo(item.nivelRiesgo)">
                          {{ item.nivelRiesgo }}
                        </span>
                      </td>
                      <td class="px-4 py-3 text-muted">{{ item.actualizadoEn | date: 'dd/MM/yyyy HH:mm' }}</td>
                    </tr>
                  } @empty {
                    <tr class="bg-panel text-muted">
                      <td class="px-4 py-6 text-center" colspan="7">Sin registros de asistencia.</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            @if (asistencia().length > asistenciaVisible().length) {
              <p class="mt-3 text-xs text-muted">
                Mostrando {{ asistenciaVisible().length }} de {{ asistencia().length }} registros.
              </p>
            }
          </siga-seccion-card>

          <siga-seccion-card title="Rendimiento">
            <div class="overflow-x-auto rounded-xl border border-gold/60">
              <table class="w-full border-collapse text-left text-sm">
                <thead>
                  <tr class="bg-bar text-gold">
                    <th class="px-4 py-3 font-semibold">Estudiante</th>
                    <th class="px-4 py-3 font-semibold">Curso / asignatura</th>
                    <th class="px-4 py-3 text-center font-semibold">Evaluación</th>
                    <th class="px-4 py-3 text-center font-semibold">Puntaje</th>
                    <th class="px-4 py-3 text-center font-semibold">Promedio</th>
                    <th class="px-4 py-3 text-center font-semibold">Insuficientes</th>
                    <th class="px-4 py-3 text-center font-semibold">Riesgo</th>
                    <th class="px-4 py-3 font-semibold">Actualizado</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of rendimientoVisible(); track item.idEstudiante + '-' + item.idEvaluacion) {
                    <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                      <td class="px-4 py-3">{{ nombreEstudiante(item.idEstudiante) }}</td>
                      <td class="px-4 py-3">{{ nombreCurso(item.idCursoAsignatura) }}</td>
                      <td class="px-4 py-3 text-center">#{{ item.idEvaluacion }}</td>
                      <td class="px-4 py-3 text-center">{{ item.puntaje ?? '—' }}</td>
                      <td class="px-4 py-3 text-center font-semibold">{{ item.promedio ?? '—' }}</td>
                      <td class="px-4 py-3 text-center">{{ item.evaluacionesInsuficientes }}</td>
                      <td class="px-4 py-3 text-center">
                        <span [class]="'rounded-full px-2 py-1 text-xs font-semibold ' + claseRiesgo(item.nivelRiesgo)">
                          {{ item.nivelRiesgo }}
                        </span>
                      </td>
                      <td class="px-4 py-3 text-muted">{{ item.actualizadoEn | date: 'dd/MM/yyyy HH:mm' }}</td>
                    </tr>
                  } @empty {
                    <tr class="bg-panel text-muted">
                      <td class="px-4 py-6 text-center" colspan="8">Sin registros de rendimiento.</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
            @if (rendimiento().length > rendimientoVisible().length) {
              <p class="mt-3 text-xs text-muted">
                Mostrando {{ rendimientoVisible().length }} de {{ rendimiento().length }} registros.
              </p>
            }
          </siga-seccion-card>
        }
      }
    </div>
  `,
})
export class ReportesDashboardComponent implements OnInit {
  protected static readonly POLL_MS = 15_000;
  private static readonly LIMITE_FILAS = 100;

  private readonly reporteService = inject(ReporteService);
  private readonly destroyRef = inject(DestroyRef);
  private polling?: Subscription;

  protected readonly cursos = signal<CursoReporte[]>([]);
  protected readonly estudiantes = signal<EstudianteReporte[]>([]);
  protected readonly idCurso = signal<number | null>(null);
  protected readonly idEstudiante = signal<number | null>(null);
  private readonly inicial = MES_ACTUAL();
  protected readonly desde = signal(this.inicial.desde);
  protected readonly hasta = signal(this.inicial.hasta);
  protected readonly cargando = signal(true);
  protected readonly actualizando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly errorParcial = signal<string | null>(null);
  protected readonly asistencia = signal<IndicadorAsistencia[]>([]);
  protected readonly rendimiento = signal<IndicadorRendimiento[]>([]);

  protected readonly cursosOpciones = computed<SelectOption[]>(() =>
    this.cursos().map((curso) => ({
      value: curso.id,
      label: `${curso.curso} · ${curso.nombre}`,
    })),
  );
  protected readonly estudiantesOpciones = computed<SelectOption[]>(() => [
    { value: 0, label: 'Todos' },
    ...this.estudiantes().map((estudiante) => ({
      value: estudiante.id,
      label: estudiante.nombre,
    })),
  ]);
  protected readonly filtros = computed<FiltrosReporte>(() => ({
    idCursoAsignatura: this.idCurso(),
    idEstudiante: this.idEstudiante(),
    from: this.desde() || null,
    to: this.hasta() || null,
  }));
  protected readonly rangoInvalido = computed(() => {
    const desde = this.desde();
    const hasta = this.hasta();
    return Boolean(desde && hasta && desde > hasta);
  });
  protected readonly asistenciaVisible = computed(() =>
    this.asistencia().slice(0, ReportesDashboardComponent.LIMITE_FILAS),
  );
  protected readonly rendimientoVisible = computed(() =>
    this.rendimiento().slice(0, ReportesDashboardComponent.LIMITE_FILAS),
  );
  protected readonly hayDatos = computed(
    () => this.asistencia().length > 0 || this.rendimiento().length > 0,
  );
  protected readonly totalEstudiantes = computed(() => {
    const ids = new Set<number>();
    this.asistencia().forEach((item) => ids.add(item.idEstudiante));
    this.rendimiento().forEach((item) => ids.add(item.idEstudiante));
    return ids.size;
  });
  protected readonly totalInasistencias = computed(() =>
    this.asistencia().reduce((total, item) => total + item.totalInasistencias, 0),
  );
  protected readonly alertasCriticas = computed(
    () =>
      this.asistencia().filter((item) => item.nivelRiesgo === 'CRITICO').length +
      this.rendimiento().filter((item) => item.nivelRiesgo === 'CRITICO').length,
  );
  protected readonly promedioGeneral = computed(() => {
    const valores = this.rendimiento()
      .map((item) => item.promedio)
      .filter((valor): valor is number => valor != null);
    if (valores.length === 0) {
      return 0;
    }
    return valores.reduce((total, valor) => total + valor, 0) / valores.length;
  });
  protected readonly ultimaActualizacion = computed(() => {
    const fechas = [...this.asistencia(), ...this.rendimiento()]
      .map((item) => item.actualizadoEn)
      .filter(Boolean)
      .sort();
    return fechas.length > 0 ? fechas[fechas.length - 1] : null;
  });

  constructor() {
    this.destroyRef.onDestroy(() => this.polling?.unsubscribe());
  }

  ngOnInit(): void {
    this.reporteService
      .getFiltros()
      .pipe(take(1), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (filtros) => {
          this.cursos.set(filtros.cursos);
          if (filtros.cursos.length > 0) {
            this.cambiarCurso(filtros.cursos[0].id);
          } else {
            this.cargando.set(false);
          }
        },
        error: () => {
          this.cargando.set(false);
          this.error.set('No se pudieron cargar los filtros de reportes.');
        },
      });
  }

  protected cambiarCurso(valor: string | number): void {
    const id = Number(valor);
    this.idCurso.set(Number.isFinite(id) && id > 0 ? id : null);
    this.idEstudiante.set(null);
    this.estudiantes.set([]);
    if (this.idCurso() === null) {
      return;
    }
    this.reporteService
      .getEstudiantes(this.idCurso() as number)
      .pipe(take(1), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (estudiantes) => this.estudiantes.set(estudiantes),
        error: () => {
          this.estudiantes.set([]);
          this.errorParcial.set('No se pudieron cargar los estudiantes del curso.');
        },
      });
    this.reiniciarPolling();
  }

  protected cambiarEstudiante(valor: string | number): void {
    const id = Number(valor);
    this.idEstudiante.set(Number.isFinite(id) && id > 0 ? id : null);
    this.reiniciarPolling();
  }

  protected cambiarDesde(valor: string): void {
    this.desde.set(valor);
    this.reiniciarPolling();
  }

  protected cambiarHasta(valor: string): void {
    this.hasta.set(valor);
    this.reiniciarPolling();
  }

  protected limpiar(): void {
    const inicial = MES_ACTUAL();
    this.idEstudiante.set(null);
    this.desde.set(inicial.desde);
    this.hasta.set(inicial.hasta);
    this.error.set(null);
    this.errorParcial.set(null);
    this.reiniciarPolling();
  }

  private reiniciarPolling(): void {
    this.polling?.unsubscribe();
    const filtros = this.filtros();
    if (filtros.idCursoAsignatura === null) {
      return;
    }
    if (this.rangoInvalido()) {
      this.aplicar(this.resultadoVacioConErrorDeRango());
      return;
    }
    this.polling = timer(0, ReportesDashboardComponent.POLL_MS)
      .pipe(
        tap(() => {
          if (this.hayDatos()) {
            this.actualizando.set(true);
          } else {
            this.cargando.set(true);
          }
        }),
        switchMap(() => this.cargarIndicadores(filtros)),
      )
      .subscribe((resultado) => this.aplicar(resultado));
  }

  protected recargar(): void {
    if (this.idCurso() === null) {
      return;
    }
    this.error.set(null);
    this.errorParcial.set(null);
    if (this.hayDatos()) {
      this.actualizando.set(true);
    } else {
      this.cargando.set(true);
    }
    this.cargarIndicadores(this.filtros())
      .pipe(take(1), takeUntilDestroyed(this.destroyRef))
      .subscribe((resultado) => this.aplicar(resultado));
  }

  protected nombreEstudiante(id: number): string {
    return this.estudiantes().find((estudiante) => estudiante.id === id)?.nombre ?? `Estudiante #${id}`;
  }

  protected nombreCurso(id: number): string {
    const curso = this.cursos().find((item) => item.id === id);
    return curso ? `${curso.curso} · ${curso.nombre}` : `Curso #${id}`;
  }

  protected claseRiesgo(nivel: NivelRiesgo): string {
    if (nivel === 'CRITICO') {
      return 'bg-bad/15 text-bad';
    }
    if (nivel === 'PREVENTIVO') {
      return 'bg-warn/15 text-warn';
    }
    return 'bg-ok/15 text-ok';
  }

  protected clasePorcentaje(porcentaje: number): string {
    if (porcentaje >= 40) {
      return 'text-bad';
    }
    if (porcentaje >= 20) {
      return 'text-warn';
    }
    return 'text-ok';
  }

  private cargarIndicadores(filtros: FiltrosReporte): Observable<ResultadoCarga> {
    let errorAsistencia = false;
    let errorRendimiento = false;
    return forkJoin({
      asistencia: this.reporteService.getAsistencia(filtros).pipe(
        catchError(() => {
          errorAsistencia = true;
          return of<IndicadorAsistencia[]>([]);
        }),
      ),
      rendimiento: this.reporteService.getRendimiento(filtros).pipe(
        catchError(() => {
          errorRendimiento = true;
          return of<IndicadorRendimiento[]>([]);
        }),
      ),
    }).pipe(
      map(({ asistencia, rendimiento }) => ({
        asistencia,
        rendimiento,
        errorAsistencia,
        errorRendimiento,
        errorRango: false,
      })),
    );
  }

  private resultadoVacioConErrorDeRango(): ResultadoCarga {
    return {
      asistencia: [],
      rendimiento: [],
      errorAsistencia: false,
      errorRendimiento: false,
      errorRango: true,
    };
  }

  private aplicar(resultado: ResultadoCarga | null): void {
    if (resultado === null) {
      this.asistencia.set([]);
      this.rendimiento.set([]);
      this.error.set(null);
      this.errorParcial.set(null);
      return;
    }
    this.cargando.set(false);
    this.actualizando.set(false);
    this.asistencia.set(resultado.asistencia);
    this.rendimiento.set(resultado.rendimiento);
    if (resultado.errorRango) {
      this.error.set('La fecha “desde” no puede ser posterior a “hasta”.');
      return;
    }
    if (resultado.errorAsistencia && resultado.errorRendimiento) {
      this.error.set('No se pudieron cargar los indicadores de asistencia y rendimiento.');
      this.errorParcial.set(null);
      return;
    }
    this.error.set(null);
    if (resultado.errorAsistencia) {
      this.errorParcial.set('No se pudo actualizar la asistencia; se mantienen los datos disponibles.');
    } else if (resultado.errorRendimiento) {
      this.errorParcial.set('No se pudo actualizar el rendimiento; se mantienen los datos disponibles.');
    } else {
      this.errorParcial.set(null);
    }
  }
}
