import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MeService } from '@siga/core';
import { SeccionCardComponent } from '@siga/shared-ui';
import { RolAdmin, USUARIOS_MOCK } from '../mocks/admin.mock';

const ROLES: RolAdmin[] = ['ADMIN', 'DOCENTE', 'APODERADO', 'ESTUDIANTE'];

@Component({
  selector: 'siga-admin-inicio',
  imports: [SeccionCardComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">
        Bienvenido(a), <span class="text-heading">{{ nombre() }}</span>
      </h1>

      <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        @for (resumen of resumenes(); track resumen.rol) {
          <article class="rounded-2xl bg-panel p-5 shadow-lg">
            <p class="text-sm text-muted">{{ resumen.rol }}</p>
            <p class="mt-1 text-3xl font-bold text-brand">{{ resumen.total }}</p>
          </article>
        }
      </div>

      <siga-seccion-card title="Usuarios" actionLabel="Ver usuarios" actionRoute="/admin/usuarios">
        <p class="text-sm text-muted">
          Gestiona cuentas, roles y estado de acceso de la institución.
        </p>
      </siga-seccion-card>
    </div>
  `,
})
export class AdminInicioComponent {
  private readonly meService = inject(MeService);

  private readonly me = toSignal(this.meService.getMe(), { initialValue: null });

  protected readonly nombre = computed(() => this.me()?.displayName ?? '');
  protected readonly resumenes = computed(() =>
    ROLES.map((rol) => ({
      rol,
      total: USUARIOS_MOCK.filter((usuario) => usuario.rol === rol).length,
    })),
  );
}
