import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ServicioAutenticacion } from '../servicios/servicio-autenticacion';

export const authGuard: CanActivateFn = () => {
  const servicioAuth = inject(ServicioAutenticacion);
  const router = inject(Router);

  if (servicioAuth.obtenerUsuarioActual()) {
    return true;
  }

  router.navigate(['/']);
  return false;
};