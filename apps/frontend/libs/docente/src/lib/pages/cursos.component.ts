import { Component } from '@angular/core';
import { CURSOS_MOCK } from '../mocks/docente.mock';

@Component({
  selector: 'siga-docente-cursos',
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Mis cursos</h1>

      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        @for (curso of cursos; track curso.id) {
          <article class="rounded-2xl bg-panel p-5 shadow-lg">
            <h3 class="text-lg font-semibold text-ink">{{ curso.asignatura }}</h3>
            <p class="mt-1 text-sm text-muted">{{ curso.nombre }} · {{ curso.profesor }}</p>
            <p class="mt-3 text-sm text-brand">{{ curso.alumnos }} alumnos</p>
          </article>
        }
      </div>
    </div>
  `,
})
export class DocenteCursosComponent {
  protected readonly cursos = CURSOS_MOCK;
}
