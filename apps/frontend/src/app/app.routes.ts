import { Routes } from '@angular/router';
import { InicioSesionComponent } from './autenticacion/inicio-sesion/inicio-sesion';
import { authGuard } from './autenticacion/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    component: InicioSesionComponent
  },
  {
    path: '**',
    redirectTo: ''
  }
];