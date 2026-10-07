import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { APP_CONFIG, recursoRemoto, toPage } from '@siga/core';
import { PaginadorComponent, SeccionCardComponent, SelectComponent, SelectOption } from '@siga/shared-ui';
import { CredencialTemporal, EstadoAdmin, RolAdmin, USUARIOS_MOCK, UsuarioAdmin } from '@siga/mocks';
import { switchMap } from 'rxjs';
import { AdminService } from '../state/admin.service';
import { CredencialTemporalComponent } from '../components/credencial-temporal.component';
import { DetalleUsuarioComponent } from '../components/detalle-usuario.component';
import { NuevoUsuarioComponent } from './nuevo-usuario.component';

@Component({
  selector: 'siga-admin-usuarios',
  imports: [
    PaginadorComponent,
    SelectComponent,
    SeccionCardComponent,
    NuevoUsuarioComponent,
    CredencialTemporalComponent,
    DetalleUsuarioComponent,
  ],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Usuarios</h1>
        @if (!usarMocks) {
          <button
            type="button"
            (click)="alternarFormulario()"
            class="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-page transition hover:brightness-110"
          >
            {{ mostrarFormulario() ? 'Ocultar registro' : 'Nuevo usuario' }}
          </button>
        }
      </div>

      @if (mensaje()) {
        <p class="rounded-xl border border-gold/60 bg-panel px-4 py-3 text-sm text-ink">{{ mensaje() }}</p>
      }

      @if (mostrarFormulario()) {
        <siga-admin-nuevo-usuario (cerrar)="mostrarFormulario.set(false)" (finalizado)="refrescar()" />
      }

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
                  @if (!usarMocks) {
                    <th class="whitespace-nowrap px-4 py-3 font-semibold">Acciones</th>
                  }
                </tr>
              </thead>
              <tbody>
                @for (usuario of paginados().content; track usuario.id) {
                  <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                    <td class="px-4 py-3">
                      <span class="block max-w-[220px] truncate" [title]="usuario.nombre">
                        {{ usuario.nombre }}
                      </span>
                    </td>
                    <td class="px-4 py-3">
                      <span class="block max-w-[240px] truncate" [title]="usuario.email">
                        {{ usuario.email }}
                      </span>
                    </td>
                    <td class="px-4 py-3">{{ usuario.rol }}</td>
                    <td class="px-4 py-3">{{ usuario.estado }}</td>
                    @if (!usarMocks) {
                      <td class="whitespace-nowrap px-4 py-3">
                        <div class="flex items-center gap-2">
                          <button
                            type="button"
                            (click)="ver(usuario)"
                            class="rounded-lg border border-line px-3 py-1.5 text-xs font-semibold text-ink/80 transition hover:bg-brand/10 hover:text-brand"
                          >
                            Ver
                          </button>
                          @if (usuario.estado === 'ACTIVO') {
                            <button
                              type="button"
                              (click)="restablecer(usuario)"
                              [disabled]="restableciendo() === usuario.id"
                              class="rounded-lg border border-brand px-3 py-1.5 text-xs font-semibold text-brand transition hover:bg-brand/20 disabled:opacity-60"
                            >
                              {{ restableciendo() === usuario.id ? 'Generando…' : 'Restablecer' }}
                            </button>
                            <button
                              type="button"
                              (click)="eliminar(usuario)"
                              [disabled]="eliminando() === usuario.id"
                              class="rounded-lg border border-bad/70 px-3 py-1.5 text-xs font-semibold text-bad transition hover:bg-bad/10 disabled:opacity-60"
                            >
                              {{ eliminando() === usuario.id ? 'Eliminando…' : 'Eliminar' }}
                            </button>
                          }
                        </div>
                      </td>
                    }
                  </tr>
                } @empty {
                  <tr class="bg-panel text-muted">
                    <td class="px-4 py-6 text-center" [attr.colspan]="usarMocks ? 4 : 5">Sin resultados.</td>
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

    @if (credencial(); as credencialTemporal) {
      <siga-credencial-temporal [credencial]="credencialTemporal" (cerrar)="credencial.set(null)" />
    }

    @if (detalleId(); as idUsuario) {
      <siga-detalle-usuario [usuarioId]="idUsuario" (cerrar)="detalleId.set(null)" />
    }
  `,
})
export class AdminUsuariosComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly adminService = inject(AdminService);

  private readonly recarga = signal(0);
  private readonly usuariosRemotos = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.adminService.getUsuarios())),
  );

  protected readonly usarMocks = this.config.useMocks;
  protected readonly hayError = computed(() => this.usuariosRemotos().estado === 'error');
  protected readonly usuarios = computed(() =>
    this.config.useMocks ? USUARIOS_MOCK : this.usuariosRemotos().dato ?? [],
  );
  protected readonly busqueda = signal('');
  protected readonly rol = signal('');
  protected readonly estado = signal('');
  protected readonly page = signal(1);
  protected readonly pageSize = 10;

  protected readonly mostrarFormulario = signal(false);
  protected readonly credencial = signal<CredencialTemporal | null>(null);
  protected readonly restableciendo = signal<string | null>(null);
  protected readonly eliminando = signal<string | null>(null);
  protected readonly detalleId = signal<string | null>(null);
  protected readonly mensaje = signal<string | null>(null);

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

  protected alternarFormulario(): void {
    this.mensaje.set(null);
    this.mostrarFormulario.update((valor) => !valor);
  }

  protected refrescar(): void {
    this.adminService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }

  protected reintentar(): void {
    this.refrescar();
  }

  protected ver(usuario: UsuarioAdmin): void {
    this.detalleId.set(usuario.id);
  }

  protected restablecer(usuario: UsuarioAdmin): void {
    if (this.restableciendo()) {
      return;
    }
    this.restableciendo.set(usuario.id);
    this.mensaje.set(null);
    this.adminService.resetPassword(usuario.id).subscribe({
      next: (credencial) => {
        this.restableciendo.set(null);
        this.credencial.set(credencial);
      },
      error: (error: HttpErrorResponse) => {
        this.restableciendo.set(null);
        this.mensaje.set(this.textoError(error));
      },
    });
  }

  protected eliminar(usuario: UsuarioAdmin): void {
    if (this.eliminando()) {
      return;
    }
    const confirmado = window.confirm(
      `¿Eliminar a ${usuario.email}? Se desactivará su acceso y su cuenta en Entra ID (los datos se conservan).`,
    );
    if (!confirmado) {
      return;
    }
    this.eliminando.set(usuario.id);
    this.mensaje.set(null);
    this.adminService.eliminarUsuario(usuario.id).subscribe({
      next: () => {
        this.eliminando.set(null);
        this.mensaje.set(`Usuario ${usuario.email} eliminado.`);
        this.refrescar();
      },
      error: (error: HttpErrorResponse) => {
        this.eliminando.set(null);
        this.mensaje.set(this.textoError(error));
      },
    });
  }

  private textoError(error: HttpErrorResponse): string {
    const cuerpo = error.error as { message?: string; errors?: Record<string, string> } | null;
    const detalleCampos = cuerpo?.errors ? Object.values(cuerpo.errors)[0] : undefined;
    return detalleCampos
      ?? cuerpo?.message
      ?? 'No se pudo completar la operación. Revisa la conexión con el BFF.';
  }
}
