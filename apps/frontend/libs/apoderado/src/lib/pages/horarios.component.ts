import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DIAS_SEMANA, DiaSemana, HorarioTablaComponent, horarioDe } from '@siga/academico';
import { DayTabsComponent } from '@siga/shared-ui';
import { ApoderadoStateService } from '../state/apoderado-state.service';

@Component({
  selector: 'siga-apoderado-horarios',
  imports: [DayTabsComponent, HorarioTablaComponent],
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
          <siga-horario-tabla [bloques]="bloques()" />
        </div>
      </section>
    </div>
  `,
})
export class ApoderadoHorariosComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly state = inject(ApoderadoStateService);

  protected readonly dias = DIAS_SEMANA;
  protected readonly dia = signal<DiaSemana>(
    normalizarDia(this.route.snapshot.queryParamMap.get('dia')),
  );
  protected readonly bloques = computed(() => horarioDe(this.state.pupiloId())[this.dia()]);

  protected seleccionarDia(valor: string): void {
    const dia = normalizarDia(valor);
    this.dia.set(dia);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { dia },
      queryParamsHandling: 'merge',
    });
  }
}

function normalizarDia(valor: string | null): DiaSemana {
  const dias: readonly string[] = DIAS_SEMANA;
  return valor && dias.includes(valor) ? (valor as DiaSemana) : 'Lunes';
}
