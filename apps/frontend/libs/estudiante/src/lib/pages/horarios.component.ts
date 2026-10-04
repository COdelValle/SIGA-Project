import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { toObservable } from '@angular/core/rxjs-interop';
import {
  DIAS_SEMANA,
  DiaSemana,
  HorarioTablaComponent,
  PerfilEstudianteService,
  diaActual,
  horarioDePerfil,
} from '@siga/academico';
import { ESTUDIANTE_ACTUAL_ID, horarioDe } from '@siga/mocks';
import { APP_CONFIG, recursoRemoto } from '@siga/core';
import { DayTabsComponent, SeccionCardComponent } from '@siga/shared-ui';
import { switchMap } from 'rxjs';

@Component({
  selector: 'siga-estudiante-horarios',
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
export class EstudianteHorariosComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly perfilService = inject(PerfilEstudianteService);

  private readonly recarga = signal(0);
  private readonly perfil = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.perfilService.getPerfilMe())),
  );

  protected readonly dias = DIAS_SEMANA;
  protected readonly dia = signal<DiaSemana>(
    normalizarDia(this.route.snapshot.queryParamMap.get('dia')),
  );
  protected readonly hayError = computed(() => this.perfil().estado === 'error');
  protected readonly bloques = computed(() => {
    if (this.config.useMocks) {
      return horarioDe(ESTUDIANTE_ACTUAL_ID)[this.dia()];
    }
    const perfil = this.perfil().dato;
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
    this.perfilService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }
}

function normalizarDia(valor: string | null): DiaSemana {
  const dias: readonly string[] = DIAS_SEMANA;
  return valor && dias.includes(valor) ? (valor as DiaSemana) : diaActual();
}
