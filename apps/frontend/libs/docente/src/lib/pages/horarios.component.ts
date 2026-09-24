import { Component, computed, inject, signal } from '@angular/core';
import { DIAS_SEMANA, DiaSemana } from '@siga/academico';
import { DayTabsComponent } from '@siga/shared-ui';
import { horaDeFranja } from '../mocks/docente.mock';
import { DocenteAcademicoService } from '../state/docente-academico.service';

@Component({
  selector: 'siga-docente-horarios',
  imports: [DayTabsComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Horario de clases</h1>

      <section class="overflow-hidden rounded-2xl bg-bar-alt shadow-lg">
        <header class="flex items-center justify-between border-b border-gold bg-bar px-5 py-3">
          <h2 class="text-lg font-semibold text-gold">Día: {{ dia() }}</h2>
        </header>

        <div class="px-4 pt-3">
          <siga-day-tabs
            [dias]="dias"
            [selected]="dia()"
            (selectedChange)="seleccionarDia($event)"
          />
        </div>

        <div class="px-4 pb-4 pt-2">
          <div class="overflow-hidden rounded-xl border border-gold/60">
            <table class="w-full border-collapse text-left text-sm">
              <thead>
                <tr class="bg-bar text-gold">
                  <th class="px-4 py-3 font-semibold">Hora</th>
                  <th class="px-4 py-3 font-semibold">Curso</th>
                  <th class="px-4 py-3 font-semibold">Asignatura</th>
                  <th class="px-4 py-3 font-semibold">Sala</th>
                </tr>
              </thead>
              <tbody>
                @for (clase of clases(); track clase.franja) {
                  <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                    <td class="px-4 py-3 align-middle">{{ hora(clase.franja) }}</td>
                    <td class="px-4 py-3 align-middle">{{ clase.curso }}</td>
                    <td class="px-4 py-3 align-middle">{{ clase.asignatura }}</td>
                    <td class="px-4 py-3 align-middle">{{ clase.sala }}</td>
                  </tr>
                } @empty {
                  <tr class="bg-panel text-muted">
                    <td class="px-4 py-6 text-center" colspan="4">
                      Sin clases para este día.
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </div>
  `,
})
export class DocenteHorariosComponent {
  private readonly academico = inject(DocenteAcademicoService);

  protected readonly dias = DIAS_SEMANA;
  protected readonly dia = signal<DiaSemana>('Lunes');
  protected readonly clases = computed(() => this.academico.horario[this.dia()]);

  protected seleccionarDia(valor: string): void {
    const dias: readonly string[] = DIAS_SEMANA;
    if (dias.includes(valor)) {
      this.dia.set(valor as DiaSemana);
    }
  }

  protected hora(franja: number): string {
    return horaDeFranja(franja);
  }
}
