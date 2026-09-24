import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MeService } from '@siga/core';
import { SeccionCardComponent } from '@siga/shared-ui';
import { horaDeFranja } from '../mocks/docente.mock';
import { DocenteAcademicoService } from '../state/docente-academico.service';

@Component({
  selector: 'siga-docente-inicio',
  imports: [SeccionCardComponent],
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
                <span class="text-ink">{{ curso.nombre }}</span>
                <span class="text-muted">{{ curso.alumnos.length }} alumnos</span>
              </li>
            }
          </ul>
        </siga-seccion-card>

        <siga-seccion-card
          [title]="'Clases de hoy (' + (diaHoy() ?? 'sin clases') + ')'"
          actionLabel="Ver horarios"
          actionRoute="/docente/horarios"
        >
          @if (clasesHoy().length > 0) {
            <div class="overflow-hidden rounded-xl border border-gold/60">
              <table class="w-full border-collapse text-left text-sm">
                <thead>
                  <tr class="bg-bar text-gold">
                    <th class="px-4 py-3 font-semibold">Hora</th>
                    <th class="px-4 py-3 font-semibold">Curso</th>
                    <th class="px-4 py-3 font-semibold">Sala</th>
                  </tr>
                </thead>
                <tbody>
                  @for (clase of clasesHoy(); track clase.franja) {
                    <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                      <td class="px-4 py-3 align-middle">{{ hora(clase.franja) }}</td>
                      <td class="px-4 py-3 align-middle">{{ clase.curso }}</td>
                      <td class="px-4 py-3 align-middle">{{ clase.sala }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <p class="rounded-xl bg-panel px-4 py-6 text-center text-sm text-muted">
              Hoy no hay clases programadas.
            </p>
          }
        </siga-seccion-card>
      </div>

      <siga-seccion-card title="Carga horaria">
        <div class="grid gap-4 sm:grid-cols-3">
          <div class="rounded-xl bg-surface px-4 py-3">
            <p class="text-xs font-semibold uppercase tracking-wide text-muted">Lectivas</p>
            <p class="mt-1 text-2xl font-bold text-heading">{{ lectivas }}h</p>
          </div>
          <div class="rounded-xl bg-surface px-4 py-3">
            <p class="text-xs font-semibold uppercase tracking-wide text-muted">No lectivas</p>
            <p class="mt-1 text-2xl font-bold text-ink">{{ noLectivas }}h</p>
          </div>
          <div class="rounded-xl bg-surface px-4 py-3">
            <p class="text-xs font-semibold uppercase tracking-wide text-muted">Total contrato</p>
            <p class="mt-1 text-2xl font-bold text-ok">{{ total }}h / 40h</p>
          </div>
        </div>
        <p class="mt-3 text-xs text-muted">
          Cálculo: 18 franjas lectivas de 90 min (27h) + 2 franjas libres de planificación (3h) + 2h
          diarias de permanencia (10h) = 40h.
        </p>
      </siga-seccion-card>
    </div>
  `,
})
export class DocenteInicioComponent {
  private readonly meService = inject(MeService);
  private readonly academico = inject(DocenteAcademicoService);

  private readonly me = toSignal(this.meService.getMe(), { initialValue: null });

  protected readonly nombre = computed(() => this.me()?.displayName ?? '');
  protected readonly cursos = this.academico.cursos;
  protected readonly diaHoy = this.academico.diaHoy;
  protected readonly clasesHoy = this.academico.clasesDeHoy;

  protected readonly lectivas = this.cursos.length * 3 * 1.5;
  protected readonly noLectivas = 40 - this.lectivas;
  protected readonly total = this.lectivas + this.noLectivas;

  protected hora(franja: number): string {
    return horaDeFranja(franja);
  }
}
