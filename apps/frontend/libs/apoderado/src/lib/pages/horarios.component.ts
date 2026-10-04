import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  DIAS_SEMANA,
  DiaSemana,
  HorarioTablaComponent,
  diaActual,
  horarioDePerfil,
} from '@siga/academico';
import { horarioDe } from '@siga/mocks';
import { APP_CONFIG } from '@siga/core';
import { DayTabsComponent, SeccionCardComponent } from '@siga/shared-ui';
import { ApoderadoStateService } from '../state/apoderado-state.service';

@Component({
  selector: 'siga-apoderado-horarios',
  imports: [DayTabsComponent, HorarioTablaComponent, SeccionCardComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Horario de clases</h1>

      @if (hayError()) {
        <siga-seccion-card title="No se pudieron cargar los datos">
          <div class="flex flex-col items-center gap-3 py-4 text-center">
            <p class="text-sm text-muted">Revisa la conexión con el BFF e inténtalo nuevamente.</p>
            <button
              type="button"
              (click)="reintentar()"
              class="rounded-lg border border-brand px-4 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
            >
              Reintentar
            </button>
          </div>
        </siga-seccion-card>
      } @else {
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
      }
    </div>
  `,
})
export class ApoderadoHorariosComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly state = inject(ApoderadoStateService);

  protected readonly dias = DIAS_SEMANA;
  protected readonly dia = signal<DiaSemana>(
    normalizarDia(this.route.snapshot.queryParamMap.get('dia')),
  );
  protected readonly hayError = computed(
    () => this.state.estadoPupilos() === 'error' || this.state.estadoPerfil() === 'error',
  );
  protected readonly bloques = computed(() => {
    if (this.config.useMocks) {
      return horarioDe(this.state.pupiloId())[this.dia()];
    }
    const perfil = this.state.perfil();
    return perfil ? horarioDePerfil(perfil)[this.dia()] : [];
  });

  protected seleccionarDia(valor: string): void {
    const dia = normalizarDia(valor);
    this.dia.set(dia);
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { dia },
      queryParamsHandling: 'merge',
    });
  }

  protected reintentar(): void {
    this.state.reintentar();
  }
}

function normalizarDia(valor: string | null): DiaSemana {
  const dias: readonly string[] = DIAS_SEMANA;
  return valor && dias.includes(valor) ? (valor as DiaSemana) : diaActual();
}
