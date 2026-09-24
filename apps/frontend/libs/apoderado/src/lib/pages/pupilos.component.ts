import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { PUPILOS_MOCK, Pupilo } from '../mocks/pupilos.mock';
import { ApoderadoStateService } from '../state/apoderado-state.service';

@Component({
  selector: 'siga-apoderado-pupilos',
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Mis pupilos</h1>

      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        @for (pupilo of pupilos; track pupilo.id) {
          <article class="rounded-2xl bg-panel p-5 shadow-lg">
            <div class="flex items-center gap-3">
              <span
                class="flex h-12 w-12 items-center justify-center rounded-full border-2 border-brand text-lg font-bold text-brand"
              >
                {{ pupilo.nombre.charAt(0) }}
              </span>
              <div>
                <h3 class="font-semibold text-ink">{{ pupilo.nombre }}</h3>
                <p class="text-sm text-muted">{{ pupilo.relacion }} · {{ pupilo.curso }}</p>
              </div>
            </div>
            <button
              type="button"
              (click)="verInformacion(pupilo)"
              class="mt-4 rounded-lg border border-gold/60 px-4 py-1.5 text-sm font-semibold text-gold transition hover:bg-gold/10"
            >
              Ver información
            </button>
          </article>
        }
      </div>
    </div>
  `,
})
export class ApoderadoPupilosComponent {
  private readonly state = inject(ApoderadoStateService);
  private readonly router = inject(Router);

  protected readonly pupilos = PUPILOS_MOCK;

  protected verInformacion(pupilo: Pupilo): void {
    this.state.seleccionar(pupilo.id);
    void this.router.navigate(['/apoderado/inicio']);
  }
}
