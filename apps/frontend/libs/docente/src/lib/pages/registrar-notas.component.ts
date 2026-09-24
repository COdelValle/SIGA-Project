import { Component, signal } from '@angular/core';
import { SelectComponent, SelectOption } from '@siga/shared-ui';
import { ALUMNOS_MOCK, CURSOS_MOCK } from '../mocks/docente.mock';

@Component({
  selector: 'siga-docente-registrar-notas',
  imports: [SelectComponent],
  template: `
    <div class="mx-auto flex max-w-4xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Registrar notas</h1>

      <div class="w-full max-w-md">
        <siga-select
          [options]="opcionesCurso"
          [value]="cursoSeleccionado()"
          ariaLabel="Seleccionar curso"
          (valueChange)="seleccionarCurso($event)"
        />
      </div>

      <section class="rounded-2xl bg-panel p-4 shadow-lg sm:p-5">
        <div class="overflow-hidden rounded-xl border border-gold/60">
          <table class="w-full border-collapse text-left text-sm">
            <thead>
              <tr class="bg-bar text-gold">
                <th class="px-4 py-3 font-semibold">Alumno</th>
                <th class="w-32 px-4 py-3 font-semibold">Nota (1.0 - 7.0)</th>
              </tr>
            </thead>
            <tbody>
              @for (alumno of alumnos; track alumno.id) {
                <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                  <td class="px-4 py-3">{{ alumno.nombre }}</td>
                  <td class="px-4 py-3">
                    <input
                      type="number"
                      min="1"
                      max="7"
                      step="0.1"
                      class="w-24 rounded-lg border border-line bg-surface px-3 py-1.5 text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
                    />
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <div class="mt-4 flex flex-wrap items-center justify-end gap-3">
          @if (mensaje()) {
            <p class="text-sm text-brand">{{ mensaje() }}</p>
          }
          <button
            type="button"
            (click)="guardar()"
            class="rounded-lg border border-brand px-4 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
          >
            Guardar
          </button>
        </div>
      </section>
    </div>
  `,
})
export class DocenteRegistrarNotasComponent {
  protected readonly cursos = CURSOS_MOCK;
  protected readonly alumnos = ALUMNOS_MOCK;
  protected readonly mensaje = signal('');

  protected readonly opcionesCurso: SelectOption[] = CURSOS_MOCK.map((curso) => ({
    value: curso.id,
    label: `${curso.asignatura} · ${curso.nombre}`,
  }));
  protected readonly cursoSeleccionado = signal(CURSOS_MOCK[0]?.id ?? 0);

  protected seleccionarCurso(value: string | number): void {
    this.cursoSeleccionado.set(Number(value));
  }

  protected guardar(): void {
    this.mensaje.set('Demo: la persistencia se habilitará con el backend de notas.');
  }
}
