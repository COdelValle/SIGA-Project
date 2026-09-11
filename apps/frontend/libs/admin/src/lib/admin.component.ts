import { Component } from '@angular/core';
import { Routes } from '@angular/router';
import { PortalShellComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-admin-home',
  imports: [PortalShellComponent],
  template: `
    <siga-portal-shell portal="Portal Administracion">
      <h2>Gestion institucional</h2>
      <p>Administracion de usuarios, roles y datos generales de la institucion.</p>
    </siga-portal-shell>
  `,
})
export class AdminHomeComponent {}

export const ADMIN_ROUTES: Routes = [{ path: '', component: AdminHomeComponent }];
