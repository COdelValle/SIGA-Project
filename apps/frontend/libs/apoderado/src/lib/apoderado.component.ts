import { Component } from '@angular/core';
import { RouterOutlet, Routes } from '@angular/router';
import { DashboardShellComponent, MenuItem } from '@siga/shared-ui';

const MENU: MenuItem[] = [
  { label: 'Inicio', route: '/apoderado/inicio', icon: 'home' },
  { label: 'Mis pupilos', route: '/apoderado/pupilos', icon: 'users' },
  { label: 'Horarios de clases', route: '/apoderado/horarios', icon: 'clock' },
  { label: 'Notas', route: '/apoderado/notas', icon: 'notes' },
  { label: 'Asistencias', route: '/apoderado/asistencias', icon: 'attendance' },
  { label: 'Progreso Académico', route: '/apoderado/progreso', icon: 'progress' },
  { label: 'Solicitudes', route: '/apoderado/solicitudes', icon: 'requests' },
];

@Component({
  selector: 'siga-apoderado-layout',
  imports: [DashboardShellComponent, RouterOutlet],
  template: `
    <siga-dashboard-shell portal="Portal Apoderado" [menu]="menu">
      <router-outlet />
    </siga-dashboard-shell>
  `,
})
export class ApoderadoLayoutComponent {
  protected readonly menu = MENU;
}

export const APODERADO_ROUTES: Routes = [
  {
    path: '',
    component: ApoderadoLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./pages/inicio.component').then((m) => m.ApoderadoInicioComponent),
      },
      {
        path: 'pupilos',
        loadComponent: () => import('./pages/pupilos.component').then((m) => m.ApoderadoPupilosComponent),
      },
      {
        path: 'horarios',
        loadComponent: () =>
          import('./pages/horarios.component').then((m) => m.ApoderadoHorariosComponent),
      },
      {
        path: 'notas',
        loadComponent: () => import('./pages/notas.component').then((m) => m.ApoderadoNotasComponent),
      },
      {
        path: 'asistencias',
        loadComponent: () =>
          import('./pages/asistencias.component').then((m) => m.ApoderadoAsistenciasComponent),
      },
      {
        path: 'asistencias/:id',
        loadComponent: () =>
          import('./pages/asistencia-historial.component').then(
            (m) => m.ApoderadoAsistenciaHistorialComponent,
          ),
      },
      {
        path: 'progreso',
        loadComponent: () =>
          import('./pages/progreso.component').then((m) => m.ApoderadoProgresoComponent),
      },
      {
        path: 'solicitudes',
        loadComponent: () =>
          import('./pages/solicitudes.component').then((m) => m.ApoderadoSolicitudesComponent),
      },
    ],
  },
];
