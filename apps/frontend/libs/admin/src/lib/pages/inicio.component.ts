import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { APP_CONFIG, MeService, recursoRemoto } from '@siga/core';
import { SeccionCardComponent } from '@siga/shared-ui';
import { RolAdmin, USUARIOS_MOCK } from '@siga/mocks';
import { switchMap } from 'rxjs';
import { AdminService } from '../state/admin.service';

const ROLES: RolAdmin[] = ['ADMIN', 'DOCENTE', 'APODERADO', 'ESTUDIANTE'];

@Component({
  selector: 'siga-admin-inicio',
  imports: [SeccionCardComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">
        Bienvenido(a), <span class="text-heading">{{ nombre() }}</span>
      </h1>

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
      }
    </div>
  `,
})
export class AdminInicioComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly meService = inject(MeService);
  private readonly adminService = inject(AdminService);

  private readonly recarga = signal(0);
  private readonly me = toSignal(this.meService.getMe(), { initialValue: null });
  private readonly usuariosRemotos = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.adminService.getUsuarios())),
  );

  protected readonly hayError = computed(() => this.usuariosRemotos().estado === 'error');
  protected readonly nombre = computed(() => this.me()?.displayName ?? '');
  protected readonly resumenes = computed(() => {
    const usuarios = this.config.useMocks ? USUARIOS_MOCK : this.usuariosRemotos().dato ?? [];
    return ROLES.map((rol) => ({
      rol,
      total: usuarios.filter((usuario) => usuario.rol === rol).length,
    }));
  });

  protected reintentar(): void {
    this.adminService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }
}
