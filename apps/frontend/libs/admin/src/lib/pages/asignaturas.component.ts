import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { toPage } from '@siga/core';
import { PaginadorComponent } from '@siga/shared-ui';
import { ASIGNATURAS_MOCK } from '@siga/mocks';
import { AdminService } from '../state/admin.service';

@Component({
  selector: 'siga-admin-asignaturas',
  imports: [PaginadorComponent],
  template: `
    <div class="mx-auto flex max-w-4xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Asignaturas</h1>

      <section class="rounded-2xl bg-panel p-4 shadow-lg sm:p-5">
        <div class="overflow-x-auto rounded-xl border border-dashed border-line">
          <table class="w-full border-collapse text-left text-sm">
            <thead>
              <tr class="bg-panel text-heading">
                <th class="px-4 py-3 font-semibold">Nombre</th>
                <th class="px-4 py-3 font-semibold">Descripción</th>
                <th class="px-4 py-3 font-semibold">Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (asignatura of paginados().content; track asignatura.id) {
                <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                  <td class="px-4 py-3">{{ asignatura.nombre }}</td>
                  <td class="px-4 py-3">{{ asignatura.descripcion }}</td>
                  <td class="px-4 py-3">{{ asignatura.activa ? 'Activa' : 'Inactiva' }}</td>
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
    </div>
  `,
})
export class AdminAsignaturasComponent {
  private readonly adminService = inject(AdminService);
  private readonly asignaturasRemotas = toSignal(this.adminService.getAsignaturas(), {
    initialValue: null,
  });

  protected readonly asignaturas = computed(() => this.asignaturasRemotas() ?? ASIGNATURAS_MOCK);
  protected readonly page = signal(1);
  protected readonly pageSize = 5;

  protected readonly paginados = computed(() =>
    toPage(this.asignaturas(), this.page() - 1, this.pageSize),
  );

  protected cambiarPagina(pagina: number): void {
    this.page.set(pagina);
  }
}
