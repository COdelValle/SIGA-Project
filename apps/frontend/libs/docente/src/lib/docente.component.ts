import { Component } from '@angular/core';
import { RouterOutlet, Routes } from '@angular/router';
import { DashboardShellComponent, MenuItem } from '@siga/shared-ui';

const MENU: MenuItem[] = [
  { label: 'Inicio', route: '/docente/inicio', icon: 'home' },
  { label: 'Mis cursos', route: '/docente/cursos', icon: 'courses' },
  { label: 'Horarios de clases', route: '/docente/horarios', icon: 'clock' },
  { label: 'Registrar notas', route: '/docente/registrar-notas', icon: 'register-notes' },
  {
    label: 'Registrar asistencias',
    route: '/docente/registrar-asistencias',
    icon: 'register-attendance',
  },
];

@Component({
  selector: 'siga-docente-layout',
  imports: [DashboardShellComponent, RouterOutlet],
  template: `
    <siga-dashboard-shell portal="Portal Docente" [menu]="menu">
      <router-outlet />
    </siga-dashboard-shell>
  `,
})
export class DocenteLayoutComponent {
  protected readonly menu = MENU;
}

export const DOCENTE_ROUTES: Routes = [
  {
    path: '',
    component: DocenteLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./pages/inicio.component').then((m) => m.DocenteInicioComponent),
      },
      {
        path: 'cursos',
        loadComponent: () => import('./pages/cursos.component').then((m) => m.DocenteCursosComponent),
      },
      {
        path: 'horarios',
        loadComponent: () =>
          import('./pages/horarios.component').then((m) => m.DocenteHorariosComponent),
      },
      {
        path: 'registrar-notas',
        loadComponent: () =>
          import('./pages/registrar-notas.component').then((m) => m.DocenteRegistrarNotasComponent),
      },
      {
        path: 'registrar-asistencias',
        loadComponent: () =>
          import('./pages/registrar-asistencias.component').then(
            (m) => m.DocenteRegistrarAsistenciasComponent,
          ),
      },
    ],
  },
];
