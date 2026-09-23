import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  ASISTENCIA_RESUMEN_MOCK,
  AsistenciaListaComponent,
  DIAS_SEMANA,
  DiaSemana,
  HORARIO_MOCK,
  HorarioResumenComponent,
  NOTAS_RESUMEN_MOCK,
  NotasListaComponent,
  resumirBloques,
} from '@siga/academico';
import { MeService } from '@siga/core';
import { DayTabsComponent, SeccionCardComponent } from '@siga/shared-ui';
import { PUPILOS_MOCK } from '../mocks/pupilos.mock';

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

      <label class="flex w-full max-w-md flex-col gap-1 text-sm text-muted">
        Pupilo
        <select
          [value]="pupiloId()"
          (change)="cambiarPupilo($event)"
          class="rounded-xl border border-line bg-panel px-4 py-3 text-base text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
        >
          @for (pupilo of pupilos; track pupilo.id) {
            <option [value]="pupilo.id">{{ pupilo.nombre }} · {{ pupilo.curso }}</option>
          }
        </select>
      </label>

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
          <siga-asistencia-lista [items]="asistencia" />
        </siga-seccion-card>

        <siga-seccion-card title="Notas" actionLabel="Ver notas" actionRoute="/apoderado/notas">
          <siga-notas-lista [items]="notas" />
        </siga-seccion-card>
      </div>
    </div>
  `,
})
export class ApoderadoInicioComponent {
  private readonly meService = inject(MeService);

  private readonly me = toSignal(this.meService.getMe(), { initialValue: null });

  protected readonly nombre = computed(() => this.me()?.displayName ?? '');
  protected readonly pupilos = PUPILOS_MOCK;
  protected readonly pupiloId = signal(PUPILOS_MOCK[0]?.id ?? 0);
  protected readonly dias = DIAS_SEMANA;
  protected readonly dia = signal<DiaSemana>('Lunes');
  protected readonly resumen = computed(() => resumirBloques(HORARIO_MOCK[this.dia()]));
  protected readonly asistencia = ASISTENCIA_RESUMEN_MOCK;
  protected readonly notas = NOTAS_RESUMEN_MOCK;

  protected cambiarPupilo(event: Event): void {
    this.pupiloId.set(Number((event.target as HTMLSelectElement).value));
  }

  protected seleccionarDia(dia: string): void {
    this.dia.set(dia as DiaSemana);
  }
}
