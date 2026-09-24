import { Component, signal } from '@angular/core';
import { SelectComponent, SelectOption } from '@siga/shared-ui';
import { ALUMNOS_MOCK, CURSOS_MOCK } from '../mocks/docente.mock';

type EstadoAsistencia = 'P' | 'A';

@Component({
  selector: 'siga-docente-registrar-asistencias',
  imports: [SelectComponent],
  template: `
    <div class="mx-auto flex max-w-4xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Registrar asistencias</h1>

      <div class="w-full max-w-md">
        <siga-select
          [options]="opcionesCurso"
          [value]="cursoSeleccionado()"
          ariaLabel="Seleccionar curso"
          (valueChange)="seleccionarCurso($event)"
        />
      </div>

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

  protected readonly opcionesCurso: SelectOption[] = CURSOS_MOCK.map((curso) => ({
    value: curso.id,
    label: `${curso.asignatura} · ${curso.nombre}`,
  }));
  protected readonly cursoSeleccionado = signal(CURSOS_MOCK[0]?.id ?? 0);

  protected seleccionarCurso(value: string | number): void {
    this.cursoSeleccionado.set(Number(value));
  }

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
