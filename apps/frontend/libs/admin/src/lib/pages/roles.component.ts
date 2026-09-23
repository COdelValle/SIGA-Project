import { Component } from '@angular/core';
import { ROLES_MOCK } from '../mocks/admin.mock';

@Component({
  selector: 'siga-admin-roles',
  template: `
    <div class="mx-auto flex max-w-4xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Roles</h1>

      <ul class="grid gap-4 sm:grid-cols-2">
        @for (rol of roles; track rol.nombre) {
          <li class="rounded-2xl bg-panel p-5 shadow-lg">
            <h3 class="text-lg font-semibold text-heading">{{ rol.nombre }}</h3>
            <p class="mt-1 text-sm text-muted">{{ rol.descripcion }}</p>
          </li>
        }
      </ul>
    </div>
  `,
})
export class AdminRolesComponent {
  protected readonly roles = ROLES_MOCK;
}
