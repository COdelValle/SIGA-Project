import { Component } from '@angular/core';
import { RouterOutlet, Routes } from '@angular/router';
import { DashboardShellComponent, MenuItem } from '@siga/shared-ui';

const MENU: MenuItem[] = [
  { label: 'Inicio', route: '/estudiante/inicio', icon: 'home' },
  { label: 'Horarios de clases', route: '/estudiante/horarios', icon: 'clock' },
  { label: 'Notas', route: '/estudiante/notas', icon: 'notes' },
  { label: 'Asistencias', route: '/estudiante/asistencias', icon: 'attendance' },
  { label: 'Progreso Académico', route: '/estudiante/progreso', icon: 'progress' },
];

@Component({
  selector: 'siga-estudiante-layout',
  imports: [DashboardShellComponent, RouterOutlet],
  template: `
    <siga-dashboard-shell portal="Portal Estudiante" [menu]="menu">
      <router-outlet />
    </siga-dashboard-shell>
  `,
})
export class EstudianteLayoutComponent {
  protected readonly menu = MENU;
}

export const ESTUDIANTE_ROUTES: Routes = [
  {
    path: '',
    component: EstudianteLayoutComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./pages/inicio.component').then((m) => m.EstudianteInicioComponent),
      },
      {
        path: 'horarios',
        loadComponent: () =>
          import('./pages/horarios.component').then((m) => m.EstudianteHorariosComponent),
      },
      {
        path: 'notas',
        loadComponent: () => import('./pages/notas.component').then((m) => m.EstudianteNotasComponent),
      },
      {
        path: 'asistencias',
        loadComponent: () =>
          import('./pages/asistencias.component').then((m) => m.EstudianteAsistenciasComponent),
      },
      {
        path: 'asistencias/:id',
        loadComponent: () =>
          import('./pages/asistencia-historial.component').then(
            (m) => m.EstudianteAsistenciaHistorialComponent,
          ),
      },
      {
        path: 'progreso',
        loadComponent: () =>
          import('./pages/progreso.component').then((m) => m.EstudianteProgresoComponent),
      },
    ],
  },
];
