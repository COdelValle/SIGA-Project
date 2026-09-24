import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AsistenciaListaComponent,
  DIAS_SEMANA,
  DiaSemana,
  HorarioResumenComponent,
  NotasListaComponent,
  asistenciaResumenDe,
  horarioDe,
  notasResumenDe,
  resumirBloques,
} from '@siga/academico';
import { MeService } from '@siga/core';
import { DayTabsComponent, SeccionCardComponent } from '@siga/shared-ui';
import { ApoderadoStateService } from '../state/apoderado-state.service';

@Component({
  selector: 'siga-apoderado-inicio',
  imports: [
    DayTabsComponent,
    SeccionCardComponent,
    HorarioResumenComponent,
    AsistenciaListaComponent,
    NotasListaComponent,
  ],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">
        Bienvenido(a), <span class="text-heading">{{ nombre() }}</span>
      </h1>

      <siga-seccion-card
        title="Horarios"
        actionLabel="Ver horarios"
        actionRoute="/apoderado/horarios"
        [actionQueryParams]="{ dia: dia() }"
      >
        <siga-day-tabs [dias]="dias" [selected]="dia()" (selectedChange)="seleccionarDia($event)" />
        <div class="mt-3">
          <siga-horario-resumen [bloques]="resumen()" />
        </div>
      </siga-seccion-card>

      <div class="grid gap-6 lg:grid-cols-2">
        <siga-seccion-card
          title="Asistencia"
          actionLabel="Ver asistencias"
          actionRoute="/apoderado/asistencias"
        >
          <siga-asistencia-lista [items]="asistencia()" />
        </siga-seccion-card>

        <siga-seccion-card title="Notas" actionLabel="Ver notas" actionRoute="/apoderado/notas">
          <siga-notas-lista [items]="notas()" />
        </siga-seccion-card>
      </div>
    </div>
  `,
})
export class ApoderadoInicioComponent {
  private readonly meService = inject(MeService);
  private readonly state = inject(ApoderadoStateService);

  private readonly me = toSignal(this.meService.getMe(), { initialValue: null });

  protected readonly nombre = computed(() => this.me()?.displayName ?? '');
  protected readonly dias = DIAS_SEMANA;
  protected readonly dia = signal<DiaSemana>('Lunes');
  protected readonly resumen = computed(() =>
    resumirBloques(horarioDe(this.state.pupiloId())[this.dia()]),
  );
  protected readonly asistencia = computed(() => asistenciaResumenDe(this.state.pupiloId()));
  protected readonly notas = computed(() => notasResumenDe(this.state.pupiloId()));

  protected seleccionarDia(dia: string): void {
    this.dia.set(dia as DiaSemana);
  }
}
