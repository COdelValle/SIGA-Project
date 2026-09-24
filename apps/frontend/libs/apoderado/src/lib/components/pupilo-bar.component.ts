import { Component, computed, inject } from '@angular/core';
import { SelectComponent, SelectOption } from '@siga/shared-ui';
import { PUPILOS_MOCK } from '../mocks/pupilos.mock';
import { ApoderadoStateService } from '../state/apoderado-state.service';

/** Barra de contexto del apoderado: seleccion de pupilo, visible en todo el portal. */
@Component({
  selector: 'siga-pupilo-bar',
  imports: [SelectComponent],
  template: `
    <div
      class="mb-6 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-gold/60 bg-panel px-4 py-3"
    >
      <div>
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">Pupilo</p>
        <p class="text-sm font-semibold text-ink">
          {{ pupilo()?.nombre }} · {{ pupilo()?.curso }}
        </p>
      </div>

      <div class="w-full max-w-md">
        <siga-select
          [options]="opciones"
          [value]="pupiloId()"
          ariaLabel="Seleccionar pupilo"
          (valueChange)="seleccionar($event)"
        />
      </div>
    </div>
  `,
})
export class PupiloBarComponent {
  private readonly state = inject(ApoderadoStateService);

  protected readonly opciones: SelectOption[] = PUPILOS_MOCK.map((pupilo) => ({
    value: pupilo.id,
    label: `${pupilo.nombre} · ${pupilo.curso}`,
  }));

  protected readonly pupiloId = this.state.pupiloId;
  protected readonly pupilo = computed(() =>
    PUPILOS_MOCK.find((item) => item.id === this.pupiloId()),
  );

  protected seleccionar(value: string | number): void {
    this.state.seleccionar(Number(value));
  }
}
