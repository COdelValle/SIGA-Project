import { Component, computed, inject, signal } from '@angular/core';
import { formatearFecha } from '@siga/academico';
import { SeccionCardComponent, SelectComponent, SelectOption } from '@siga/shared-ui';
import { Alumno, nombreCompleto } from '../mocks/docente.mock';
import {
  DocenteAcademicoService,
  EstadoAsistencia,
  RegistroAsistencia,
} from '../state/docente-academico.service';

@Component({
  selector: 'siga-docente-registrar-asistencias',
  imports: [SeccionCardComponent, SelectComponent],
  template: `
    <div class="mx-auto flex max-w-5xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Registrar asistencias</h1>

      <p class="text-sm text-muted">
        Hoy es {{ diaHoy() }} {{ fechaHoy }} · Semestre {{ semestre() }}
      </p>

      @if (cursos().length > 0) {
        <div class="w-full max-w-md">
          <siga-select
            [options]="opcionesCurso()"
            [value]="cursoId()"
            ariaLabel="Seleccionar curso"
            (valueChange)="cambiarCurso($event)"
          />
        </div>

        <siga-seccion-card [title]="(curso()?.nombre ?? '') + ' · ' + (curso()?.asignatura ?? '')">
          <ul class="flex flex-col gap-2">
            @for (alumno of alumnos(); track alumno.id) {
              <li
                class="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-surface px-4 py-3"
              >
                <div>
                  <p class="text-sm font-medium text-ink">{{ nombre(alumno) }}</p>
                  @if (registro(alumno.id); as reg) {
                    <p class="text-xs text-muted">Justificación: {{ detalle(reg) }}</p>
                  } @else {
                    <p class="text-xs text-muted">Sin marcar</p>
                  }
                </div>

                <div class="flex gap-2">
                  <button
                    type="button"
                    (click)="marcar(alumno.id, 'Presente')"
                    class="rounded-lg border px-3 py-1.5 text-sm font-semibold transition"
                    [class]="clase(alumno.id, 'Presente')"
                  >
                    Presente
                  </button>
                  <button
                    type="button"
                    (click)="marcar(alumno.id, 'Ausente')"
                    class="rounded-lg border px-3 py-1.5 text-sm font-semibold transition"
                    [class]="clase(alumno.id, 'Ausente')"
                  >
                    Ausente
                  </button>
                </div>
              </li>
            }
          </ul>

          <div class="mt-4 flex flex-wrap items-center justify-end gap-3">
            @if (pendiente()) {
              <p class="text-sm text-warn">Borrador sin guardar</p>
            } @else {
              <p class="text-sm text-ok">Guardado</p>
            }
            <button
              type="button"
              (click)="guardar()"
              class="rounded-lg border border-brand px-4 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
            >
              Guardar
            </button>
          </div>
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

  protected readonly cursos = this.academico.cursosConClaseHoy;
  protected readonly opcionesCurso = computed<SelectOption[]>(() =>
    this.cursos().map((curso) => ({
      value: curso.id,
      label: `${curso.nombre} · ${curso.asignatura}`,
    })),
  );
  protected readonly cursoId = signal(this.academico.cursosConClaseHoy()[0]?.id ?? 0);
  protected readonly curso = computed(() =>
    this.cursos().find((item) => item.id === this.cursoId()),
  );
  protected readonly alumnos = computed(() => this.curso()?.alumnos ?? []);
  protected readonly diaHoy = this.academico.diaHoy;
  protected readonly fechaHoy = this.academico.fechaHoy;
  protected readonly semestre = this.academico.semestreActual;
  protected readonly pendiente = computed(() =>
    this.academico.pendiente(`asistencia|${this.cursoId()}|${this.fechaHoy}`),
  );

  protected nombre(alumno: Alumno): string {
    return nombreCompleto(alumno);
  }

  protected registro(alumnoId: number): RegistroAsistencia | undefined {
    return this.academico.registroDe(this.cursoId(), this.fechaHoy, alumnoId);
  }

  protected detalle(registro: RegistroAsistencia): string {
    const justificacion = this.academico.justificacionDe(registro, this.fechaHoy);
    if (justificacion === 'Pendiente' && registro.limite) {
      return `Pendiente (hasta ${formatearFecha(registro.limite)})`;
    }
    return justificacion;
  }

  protected marcar(alumnoId: number, estado: EstadoAsistencia): void {
    this.academico.marcar(this.cursoId(), this.fechaHoy, alumnoId, estado);
  }

  protected clase(alumnoId: number, estado: EstadoAsistencia): string {
    const registro = this.registro(alumnoId);
    const activo = registro?.estado === estado;
    if (!activo) {
      return 'border-line text-ink/80 hover:bg-brand/10 hover:text-brand';
    }
    return estado === 'Presente' ? 'border-ok bg-ok/15 text-ok' : 'border-bad bg-bad/15 text-bad';
  }

  protected guardar(): void {
    this.academico.guardarAsistenciaCurso(this.cursoId(), this.fechaHoy);
  }

  protected cambiarCurso(value: string | number): void {
    this.cursoId.set(Number(value));
  }
}
