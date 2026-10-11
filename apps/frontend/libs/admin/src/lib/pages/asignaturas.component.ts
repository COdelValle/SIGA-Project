import { Component, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { APP_CONFIG, recursoRemoto, toPage } from '@siga/core';
import { PaginadorComponent, SeccionCardComponent } from '@siga/shared-ui';
import { ASIGNATURAS_MOCK } from '@siga/mocks';
import { switchMap } from 'rxjs';
import { AdminService } from '../state/admin.service';

@Component({
  selector: 'siga-admin-asignaturas',
  imports: [PaginadorComponent, SeccionCardComponent],
  template: `
    <div class="mx-auto flex max-w-5xl flex-col gap-6">
      <div>
        <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Asignaturas</h1>
        <p class="mt-1 text-sm text-muted">
          Catálogo general de asignaturas. Los cursos, docentes y horarios se gestionan por curso.
        </p>
      </div>

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
                  <th class="px-4 py-3 font-semibold">Nombre</th>
                  <th class="px-4 py-3 font-semibold">Área</th>
                  <th class="px-4 py-3 font-semibold">Calificable</th>
                  <th class="px-4 py-3 font-semibold">Niveles</th>
                  <th class="px-4 py-3 font-semibold">Estado</th>
                </tr>
              </thead>
              <tbody>
                @for (asignatura of paginados().content; track asignatura.id) {
                  <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                    <td class="px-4 py-3 font-medium">{{ asignatura.nombre }}</td>
                    <td class="px-4 py-3">{{ asignatura.area }}</td>
                    <td class="px-4 py-3">{{ asignatura.calificable ? 'Sí' : 'Sin calificación' }}</td>
                    <td class="px-4 py-3 text-xs text-muted">
                      {{ asignatura.niveles.length > 0 ? rangoNiveles(asignatura.niveles) : '—' }}
                    </td>
                    <td class="px-4 py-3">{{ asignatura.activa ? 'Activa' : 'Inactiva' }}</td>
                  </tr>
                } @empty {
                  <tr class="bg-panel text-muted">
                    <td class="px-4 py-6 text-center" colspan="5">Sin asignaturas registradas.</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <siga-paginador
            [total]="asignaturas().length"
            [page]="page()"
            [pageSize]="pageSize"
            (pageChange)="cambiarPagina($event)"
          />
        </section>
      }
    </div>
  `,
})
export class AdminAsignaturasComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly adminService = inject(AdminService);

  private readonly recarga = signal(0);
  private readonly asignaturasRemotas = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.adminService.getAsignaturas())),
  );

  protected readonly hayError = computed(() => this.asignaturasRemotas().estado === 'error');
  protected readonly asignaturas = computed(() =>
    this.config.useMocks ? ASIGNATURAS_MOCK : this.asignaturasRemotas().dato ?? [],
  );
  protected readonly page = signal(1);
  protected readonly pageSize = 10;

  protected readonly paginados = computed(() =>
    toPage(this.asignaturas(), this.page() - 1, this.pageSize),
  );

  /** Muestra "Primer nivel – Último nivel" para no saturar la tabla. */
  protected rangoNiveles(niveles: string[]): string {
    if (niveles.length === 1) {
      return niveles[0];
    }
    return `${niveles[0]} – ${niveles[niveles.length - 1]}`;
  }

  protected cambiarPagina(pagina: number): void {
    this.page.set(pagina);
  }

  protected reintentar(): void {
    this.adminService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }
}
