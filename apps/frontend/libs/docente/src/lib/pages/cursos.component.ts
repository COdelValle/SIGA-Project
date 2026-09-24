import { Component, inject } from '@angular/core';
import { DocenteAcademicoService } from '../state/docente-academico.service';

@Component({
  selector: 'siga-docente-cursos',
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Mis cursos</h1>

      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        @for (curso of cursos; track curso.id) {
          <article class="rounded-2xl bg-panel p-5 shadow-lg">
            <h3 class="text-lg font-semibold text-heading">{{ curso.nombre }}</h3>
            <p class="mt-1 text-sm text-muted">{{ curso.asignatura }}</p>
            <dl class="mt-3 flex flex-col gap-1 text-sm">
              <div class="flex justify-between">
                <dt class="text-muted">Alumnos</dt>
                <dd class="font-semibold text-ink">{{ curso.alumnos.length }}</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-muted">Clases por semana</dt>
                <dd class="font-semibold text-ink">{{ curso.diasClase.length }} (1h30 c/u)</dd>
              </div>
              <div class="flex justify-between">
                <dt class="text-muted">Sala</dt>
                <dd class="font-semibold text-ink">{{ curso.sala }}</dd>
              </div>
            </dl>
          </article>
        }
      </div>
    </div>
  `,
})
export class DocenteCursosComponent {
  private readonly academico = inject(DocenteAcademicoService);

  protected readonly cursos = this.academico.cursos;
}
