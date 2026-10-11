import { Component, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { APP_CONFIG, recursoRemoto } from '@siga/core';
import { SeccionCardComponent } from '@siga/shared-ui';
import { MALLA_MOCK, NIVELES_MOCK } from '@siga/mocks';
import { switchMap } from 'rxjs';
import { AdminService } from '../state/admin.service';

@Component({
  selector: 'siga-admin-malla',
  imports: [SeccionCardComponent],
  template: `
    <div class="mx-auto flex max-w-5xl flex-col gap-6">
      <div>
        <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Malla curricular</h1>
        <p class="mt-1 text-sm text-muted">
          Asignaturas que aplican a cada nivel, con su carácter y calificación.
        </p>
      </div>

      <label class="flex max-w-xs flex-col gap-1 text-sm text-muted">
        Nivel
        <select
          class="rounded-lg border border-line bg-panel px-3 py-2 text-sm text-ink"
          [value]="nivel()"
          (change)="cambiarNivel($event)"
        >
          @for (opcion of niveles; track opcion) {
            <option [value]="opcion">{{ opcion }}</option>
          }
        </select>
      </label>

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
        <section class="rounded-2xl bg-panel p-4 shadow-lg sm:p-5">
          <div class="overflow-x-auto rounded-xl border border-dashed border-line">
            <table class="w-full border-collapse text-left text-sm">
              <thead>
                <tr class="bg-panel text-heading">
                  <th class="px-4 py-3 font-semibold">Asignatura</th>
                  <th class="px-4 py-3 font-semibold">Área</th>
                  <th class="px-4 py-3 font-semibold">Carácter</th>
                  <th class="px-4 py-3 font-semibold">Horas</th>
                  <th class="px-4 py-3 font-semibold">Calificación</th>
                </tr>
              </thead>
              <tbody>
                @for (fila of filasDelNivel(); track fila.id) {
                  <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                    <td class="px-4 py-3 font-medium">{{ fila.nombre }}</td>
                    <td class="px-4 py-3">{{ fila.area }}</td>
                    <td class="px-4 py-3">{{ etiquetaCaracter(fila.caracter) }}</td>
                    <td class="px-4 py-3">{{ fila.horasSemanales ?? '—' }}</td>
                    <td class="px-4 py-3">
                      {{ fila.calificable ? 'Calificable' : 'Sin calificación' }}
                    </td>
                  </tr>
                } @empty {
                  <tr class="bg-panel text-muted">
                    <td class="px-4 py-6 text-center" colspan="5">Sin asignaturas para este nivel.</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </section>
      }
    </div>
  `,
})
export class AdminMallaComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly adminService = inject(AdminService);

  protected readonly niveles = NIVELES_MOCK;
  protected readonly nivel = signal(NIVELES_MOCK[0]);

  private readonly recarga = signal(0);
  private readonly mallaRemota = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.adminService.getMalla())),
  );

  protected readonly hayError = computed(() => this.mallaRemota().estado === 'error');
  private readonly malla = computed(() =>
    this.config.useMocks ? MALLA_MOCK : this.mallaRemota().dato ?? [],
  );
  protected readonly filasDelNivel = computed(() =>
    this.malla().filter((fila) => fila.nivel === this.nivel()),
  );

  protected cambiarNivel(evento: Event): void {
    this.nivel.set((evento.target as HTMLSelectElement).value);
  }

  protected etiquetaCaracter(caracter: string): string {
    switch (caracter) {
      case 'OBLIGATORIA':
        return 'Obligatoria';
      case 'OPTATIVA':
        return 'Optativa';
      default:
        return 'Electiva';
    }
  }

  protected reintentar(): void {
    this.adminService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }
}
