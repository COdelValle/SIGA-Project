import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  AsistenciaHistorialComponent,
  ESTUDIANTE_ACTUAL_ID,
  asistenciaRegistrosDe,
  asistenciaResumenDe,
} from '@siga/academico';
import { SeccionCardComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-estudiante-asistencia-historial',
  imports: [RouterLink, SeccionCardComponent, AsistenciaHistorialComponent],
  template: `
    <div class="mx-auto flex max-w-5xl flex-col gap-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Historial de asistencia</h1>
        <a
          routerLink="/estudiante/asistencias"
          class="rounded-lg border border-gold/60 px-4 py-1.5 text-sm font-semibold text-gold transition hover:bg-gold/10"
        >
          Volver
        </a>
      </div>

      <siga-seccion-card [title]="asignatura()">
        <siga-asistencia-historial [registros]="registros()" />
      </siga-seccion-card>
    </div>
  `,
})
export class EstudianteAsistenciaHistorialComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly idParam = toSignal(this.route.paramMap, {
    initialValue: this.route.snapshot.paramMap,
  });

  private readonly id = computed(() => Number(this.idParam().get('id') ?? 0));

  protected readonly asignatura = computed(
    () =>
      asistenciaResumenDe(ESTUDIANTE_ACTUAL_ID).find((item) => item.id === this.id())?.asignatura ??
      'Asignatura',
  );
  protected readonly registros = computed(
    () => asistenciaRegistrosDe(ESTUDIANTE_ACTUAL_ID)[this.id()] ?? [],
  );
}
