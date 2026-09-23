import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { HORARIO_MOCK, HorarioTablaComponent } from '@siga/academico';
import { MeService } from '@siga/core';
import { SeccionCardComponent } from '@siga/shared-ui';
import { CURSOS_MOCK } from '../mocks/docente.mock';

@Component({
  selector: 'siga-docente-inicio',
  imports: [SeccionCardComponent, HorarioTablaComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">
        Bienvenido(a), <span class="text-heading">{{ nombre() }}</span>
      </h1>

      <div class="grid gap-6 lg:grid-cols-2">
        <siga-seccion-card title="Mis cursos" actionLabel="Ver cursos" actionRoute="/docente/cursos">
          <ul class="flex flex-col gap-2">
            @for (curso of cursos; track curso.id) {
              <li class="flex items-center justify-between rounded-lg bg-surface px-4 py-3 text-sm">
                <span class="text-ink">{{ curso.asignatura }}</span>
                <span class="text-muted">{{ curso.nombre }} · {{ curso.alumnos }} alumnos</span>
              </li>
            }
          </ul>
        </siga-seccion-card>

        <siga-seccion-card
          title="Clases de hoy (Lunes)"
          actionLabel="Ver horarios"
          actionRoute="/docente/horarios"
        >
          <siga-horario-tabla [bloques]="clasesHoy" />
        </siga-seccion-card>
      </div>
    </div>
  `,
})
export class DocenteInicioComponent {
  private readonly meService = inject(MeService);

  private readonly me = toSignal(this.meService.getMe(), { initialValue: null });

  protected readonly nombre = computed(() => this.me()?.displayName ?? '');
  protected readonly cursos = CURSOS_MOCK;
  protected readonly clasesHoy = HORARIO_MOCK['Lunes'];
}
