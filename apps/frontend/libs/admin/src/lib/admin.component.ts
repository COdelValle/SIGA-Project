import { Component } from '@angular/core';
import { Routes } from '@angular/router';
import { PortalShellComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-admin-home',
  imports: [PortalShellComponent],
  template: `
    <siga-portal-shell portal="Portal Administracion">
      <h2 class="text-2xl font-semibold text-slate-900">Gestion institucional</h2>
      <p class="mt-1 text-sm text-slate-500">
        Administracion de usuarios, roles y datos generales de la institucion.
      </p>
    </siga-portal-shell>
  `,
})
export class AdminHomeComponent {}

export const ADMIN_ROUTES: Routes = [{ path: '', component: AdminHomeComponent }];
