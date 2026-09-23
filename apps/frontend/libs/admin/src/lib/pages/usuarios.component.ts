import { Component, computed, signal } from '@angular/core';
import { EstadoAdmin, RolAdmin, USUARIOS_MOCK } from '../mocks/admin.mock';

@Component({
  selector: 'siga-admin-usuarios',
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Usuarios</h1>

      <div class="grid gap-3 sm:grid-cols-[1fr_auto_auto]">
        <input
          type="search"
          [value]="busqueda()"
          (input)="buscar($event)"
          placeholder="Buscar por nombre o correo"
          class="rounded-xl border border-line bg-panel px-4 py-3 text-base text-ink placeholder:text-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
        />
        <select
          [value]="rol()"
          (change)="cambiarRol($event)"
          class="rounded-xl border border-line bg-panel px-4 py-3 text-base text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
        >
          <option value="">Todos los roles</option>
          @for (r of roles; track r) {
            <option [value]="r">{{ r }}</option>
          }
        </select>
        <select
          [value]="estado()"
          (change)="cambiarEstado($event)"
          class="rounded-xl border border-line bg-panel px-4 py-3 text-base text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
        >
          <option value="">Todos los estados</option>
          @for (e of estados; track e) {
            <option [value]="e">{{ e }}</option>
          }
        </select>
      </div>

      <section class="rounded-2xl bg-panel p-4 shadow-lg sm:p-5">
        <div class="overflow-x-auto rounded-xl border border-dashed border-line">
          <table class="w-full border-collapse text-left text-sm">
            <thead>
              <tr class="bg-panel text-heading">
                <th class="px-4 py-3 font-semibold">Nombre</th>
                <th class="px-4 py-3 font-semibold">Correo</th>
                <th class="px-4 py-3 font-semibold">Rol</th>
                <th class="px-4 py-3 font-semibold">Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (usuario of filtrados(); track usuario.id) {
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
        <p class="mt-3 text-xs text-muted">
          {{ filtrados().length }} de {{ usuarios.length }} usuarios (demo, pendiente backend).
        </p>
      </section>
    </div>
  `,
})
export class AdminUsuariosComponent {
  protected readonly usuarios = USUARIOS_MOCK;
  protected readonly roles: RolAdmin[] = ['ADMIN', 'DOCENTE', 'APODERADO', 'ESTUDIANTE'];
  protected readonly estados: EstadoAdmin[] = ['ACTIVO', 'INACTIVO'];
  protected readonly busqueda = signal('');
  protected readonly rol = signal('');
  protected readonly estado = signal('');

  protected readonly filtrados = computed(() => {
    const q = this.busqueda().trim().toLowerCase();
    return this.usuarios.filter((usuario) => {
      const coincide =
        !q ||
        usuario.nombre.toLowerCase().includes(q) ||
        usuario.email.toLowerCase().includes(q);
      const porRol = !this.rol() || usuario.rol === this.rol();
      const porEstado = !this.estado() || usuario.estado === this.estado();
      return coincide && porRol && porEstado;
    });
  });

  protected buscar(event: Event): void {
    this.busqueda.set((event.target as HTMLInputElement).value);
  }

  protected cambiarRol(event: Event): void {
    this.rol.set((event.target as HTMLSelectElement).value);
  }

  protected cambiarEstado(event: Event): void {
    this.estado.set((event.target as HTMLSelectElement).value);
  }
}
