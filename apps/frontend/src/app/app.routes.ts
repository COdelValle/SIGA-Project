import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { roleGuard } from '@siga/core';
import { AuthRedirectComponent } from './auth-redirect/auth-redirect.component';

export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('@siga/public-portal').then((m) => m.PUBLIC_ROUTES),
  },
  {
    path: 'auth',
    component: AuthRedirectComponent,
  },
  {
    path: 'sin-acceso',
    loadComponent: () => import('@siga/public-portal').then((m) => m.SinAccesoComponent),
  },
  {
    path: 'estudiante',
    canActivate: [MsalGuard, roleGuard(['ESTUDIANTE'])],
    loadChildren: () => import('@siga/estudiante').then((m) => m.ESTUDIANTE_ROUTES),
  },
  {
    path: 'apoderado',
    canActivate: [MsalGuard, roleGuard(['APODERADO'])],
    loadChildren: () => import('@siga/apoderado').then((m) => m.APODERADO_ROUTES),
  },
  {
    path: 'docente',
    canActivate: [MsalGuard, roleGuard(['DOCENTE'])],
    loadChildren: () => import('@siga/docente').then((m) => m.DOCENTE_ROUTES),
  },
  {
    path: 'admin',
    canActivate: [MsalGuard, roleGuard(['ADMIN'])],
    loadChildren: () => import('@siga/admin').then((m) => m.ADMIN_ROUTES),
  },
  { path: '**', redirectTo: '' },
];
