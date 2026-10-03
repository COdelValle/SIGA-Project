import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  AsistenciaHistorialComponent,
  asistenciaRegistrosDePerfil,
  asistenciaResumenDePerfil,
} from '@siga/academico';
import { asistenciaRegistrosDe, asistenciaResumenDe } from '@siga/mocks';
import { SeccionCardComponent } from '@siga/shared-ui';
import { ApoderadoStateService } from '../state/apoderado-state.service';

@Component({
  selector: 'siga-apoderado-asistencia-historial',
  imports: [RouterLink, SeccionCardComponent, AsistenciaHistorialComponent],
  template: `
    <div class="mx-auto flex max-w-5xl flex-col gap-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Historial de asistencia</h1>
        <a
          routerLink="/apoderado/asistencias"
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
export class ApoderadoAsistenciaHistorialComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly state = inject(ApoderadoStateService);
  private readonly idParam = toSignal(this.route.paramMap, {
    initialValue: this.route.snapshot.paramMap,
  });

  private readonly id = computed(() => Number(this.idParam().get('id') ?? 0));

  private readonly resumen = computed(() => {
    const perfil = this.state.perfil();
    const asistencias = this.state.asistencias();
    return perfil && asistencias
      ? asistenciaResumenDePerfil(perfil, asistencias)
      : asistenciaResumenDe(this.state.pupiloId());
  });
  private readonly todosLosRegistros = computed(() => {
    const perfil = this.state.perfil();
    const asistencias = this.state.asistencias();
    return perfil && asistencias
      ? asistenciaRegistrosDePerfil(perfil, asistencias)
      : asistenciaRegistrosDe(this.state.pupiloId());
  });

  protected readonly asignatura = computed(
    () => this.resumen().find((item) => item.id === this.id())?.asignatura ?? 'Asignatura',
  );
  protected readonly registros = computed(() => this.todosLosRegistros()[this.id()] ?? []);
}
