import { Component, signal } from '@angular/core';
import { ALUMNOS_MOCK, CURSOS_MOCK } from '../mocks/docente.mock';

type EstadoAsistencia = 'P' | 'A';

@Component({
  selector: 'siga-docente-registrar-asistencias',
  imports: [],
  template: `
    <div class="mx-auto flex max-w-4xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Registrar asistencias</h1>

      <label class="flex w-full max-w-sm flex-col gap-1 text-sm text-muted">
        Curso
        <select
          class="rounded-xl border border-line bg-panel px-4 py-3 text-base text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
        >
          @for (curso of cursos; track curso.id) {
            <option>{{ curso.asignatura }} · {{ curso.nombre }}</option>
          }
        </select>
      </label>

      <ul class="flex flex-col gap-2">
        @for (alumno of alumnos; track alumno.id) {
          <li
            class="flex flex-wrap items-center justify-between gap-3 rounded-xl bg-panel px-4 py-3 shadow-sm"
          >
            <span class="text-sm text-ink">{{ alumno.nombre }}</span>
            <div class="flex gap-2">
              <button
                type="button"
                (click)="marcar(alumno.id, 'P')"
                class="rounded-lg border px-3 py-1.5 text-sm font-semibold transition"
                [class]="clase(alumno.id, 'P')"
              >
                Presente
              </button>
              <button
                type="button"
                (click)="marcar(alumno.id, 'A')"
                class="rounded-lg border px-3 py-1.5 text-sm font-semibold transition"
                [class]="clase(alumno.id, 'A')"
              >
                Ausente
              </button>
            </div>
          </li>
        }
      </ul>
    </div>
  `,
})
export class DocenteRegistrarAsistenciasComponent {
  protected readonly cursos = CURSOS_MOCK;
  protected readonly alumnos = ALUMNOS_MOCK;
  protected readonly estado = signal<Record<number, EstadoAsistencia>>({});

  protected marcar(id: number, valor: EstadoAsistencia): void {
    this.estado.update((actual) => ({ ...actual, [id]: valor }));
  }

  protected clase(id: number, valor: EstadoAsistencia): string {
    const activo = this.estado()[id] === valor;
    if (!activo) {
      return 'border-line text-ink/80 hover:bg-brand/10 hover:text-brand';
    }
    return valor === 'P' ? 'border-ok bg-ok/15 text-ok' : 'border-bad bg-bad/15 text-bad';
  }
}
