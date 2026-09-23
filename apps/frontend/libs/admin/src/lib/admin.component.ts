import { Component } from '@angular/core';
import { RouterOutlet, Routes } from '@angular/router';
import { DashboardShellComponent, MenuItem } from '@siga/shared-ui';

const MENU: MenuItem[] = [
  { label: 'Inicio', route: '/admin/inicio', icon: 'home' },
  { label: 'Usuarios', route: '/admin/usuarios', icon: 'users' },
  { label: 'Roles', route: '/admin/roles', icon: 'roles' },
  { label: 'Asignaturas', route: '/admin/asignaturas', icon: 'subjects' },
];

@Component({
  selector: 'siga-admin-layout',
  imports: [DashboardShellComponent, RouterOutlet],
  template: `
    <siga-dashboard-shell portal="Portal Administración" [menu]="menu">
      <router-outlet />
    </siga-dashboard-shell>
  `,
})
export class AdminLayoutComponent {
  protected readonly menu = MENU;
}

export const ADMIN_ROUTES: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./pages/inicio.component').then((m) => m.AdminInicioComponent),
      },
      {
        path: 'usuarios',
        loadComponent: () => import('./pages/usuarios.component').then((m) => m.AdminUsuariosComponent),
      },
      {
        path: 'roles',
        loadComponent: () => import('./pages/roles.component').then((m) => m.AdminRolesComponent),
      },
      {
        path: 'asignaturas',
        loadComponent: () =>
          import('./pages/asignaturas.component').then((m) => m.AdminAsignaturasComponent),
      },
    ],
  },
];
