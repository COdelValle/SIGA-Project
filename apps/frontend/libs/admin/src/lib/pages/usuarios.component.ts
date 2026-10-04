import { Component, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { APP_CONFIG, recursoRemoto, toPage } from '@siga/core';
import { PaginadorComponent, SeccionCardComponent, SelectComponent, SelectOption } from '@siga/shared-ui';
import { EstadoAdmin, RolAdmin, USUARIOS_MOCK } from '@siga/mocks';
import { switchMap } from 'rxjs';
import { AdminService } from '../state/admin.service';

@Component({
  selector: 'siga-admin-usuarios',
  imports: [PaginadorComponent, SelectComponent, SeccionCardComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Usuarios</h1>

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
        <div class="grid gap-3 sm:grid-cols-[1fr_auto_auto]">
          <input
            type="search"
            [value]="busqueda()"
            (input)="buscar($event)"
            placeholder="Buscar por nombre o correo"
            class="rounded-xl border border-line bg-panel px-4 py-3 text-base text-ink placeholder:text-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
          />

          <div class="min-w-56">
            <siga-select
              [options]="opcionesRol"
              [value]="rol()"
              ariaLabel="Filtrar por rol"
              (valueChange)="cambiarRol($event)"
            />
          </div>

          <div class="min-w-56">
            <siga-select
              [options]="opcionesEstado"
              [value]="estado()"
              ariaLabel="Filtrar por estado"
              (valueChange)="cambiarEstado($event)"
            />
          </div>
        </div>

        <section class="rounded-2xl bg-panel p-4 shadow-lg sm:p-5">
          <div class="overflow-x-auto rounded-xl border border-gold/60">
            <table class="w-full border-collapse text-left text-sm">
              <thead>
                <tr class="bg-bar text-gold">
                  <th class="px-4 py-3 font-semibold">Nombre</th>
                  <th class="px-4 py-3 font-semibold">Correo</th>
                  <th class="px-4 py-3 font-semibold">Rol</th>
                  <th class="px-4 py-3 font-semibold">Estado</th>
                </tr>
              </thead>
              <tbody>
                @for (usuario of paginados().content; track usuario.id) {
                  <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                    <td class="px-4 py-3">{{ usuario.nombre }}</td>
                    <td class="px-4 py-3">{{ usuario.email }}</td>
                    <td class="px-4 py-3">{{ usuario.rol }}</td>
                    <td class="px-4 py-3">{{ usuario.estado }}</td>
                  </tr>
                } @empty {
                  <tr class="bg-panel text-muted">
                    <td class="px-4 py-6 text-center" colspan="4">Sin resultados.</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          <siga-paginador
            [total]="filtrados().length"
            [page]="page()"
            [pageSize]="pageSize"
            (pageChange)="cambiarPagina($event)"
          />
        </section>
      }
    </div>
  `,
})
export class AdminUsuariosComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly adminService = inject(AdminService);

  private readonly recarga = signal(0);
  private readonly usuariosRemotos = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.adminService.getUsuarios())),
  );

  protected readonly hayError = computed(() => this.usuariosRemotos().estado === 'error');
  protected readonly usuarios = computed(() =>
    this.config.useMocks ? USUARIOS_MOCK : this.usuariosRemotos().dato ?? [],
  );
  protected readonly busqueda = signal('');
  protected readonly rol = signal('');
  protected readonly estado = signal('');
  protected readonly page = signal(1);
  protected readonly pageSize = 10;

  protected readonly opcionesRol: SelectOption[] = [
    { value: '', label: 'Todos los roles' },
    ...(['ADMIN', 'DOCENTE', 'APODERADO', 'ESTUDIANTE'] as RolAdmin[]).map((rol) => ({
      value: rol,
      label: rol,
    })),
  ];
  protected readonly opcionesEstado: SelectOption[] = [
    { value: '', label: 'Todos los estados' },
    ...(['ACTIVO', 'INACTIVO'] as EstadoAdmin[]).map((estado) => ({
      value: estado,
      label: estado,
    })),
  ];

  protected readonly filtrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    return this.usuarios().filter((usuario) => {
      const coincide =
        !q ||
        usuario.nombre.toLowerCase().includes(q) ||
        usuario.email.toLowerCase().includes(q);
      const porRol = !this.rol() || usuario.rol === this.rol();
      const porEstado = !this.estado() || usuario.estado === this.estado();
      return coincide && porRol && porEstado;
    });
  });

  protected readonly paginados = computed(() =>
    toPage(this.filtrados(), this.page() - 1, this.pageSize),
  );

  protected buscar(event: Event): void {
    this.busqueda.set((event.target as HTMLInputElement).value);
    this.page.set(1);
  }

  protected cambiarRol(value: string | number): void {
    this.rol.set(String(value));
    this.page.set(1);
  }

  protected cambiarEstado(value: string | number): void {
    this.estado.set(String(value));
    this.page.set(1);
  }

  protected cambiarPagina(pagina: number): void {
    this.page.set(pagina);
  }

  protected reintentar(): void {
    this.adminService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }
}
