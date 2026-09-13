import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { ROL_HOME, Rol } from '../models/role.enum';
import { MeService } from './me.service';

export function roleGuard(roles: Rol[]): CanActivateFn {
  return () => {
    const meService = inject(MeService);
    const router = inject(Router);

    return meService.getMe().pipe(
      map((me) => {
        if (meService.hasRole(me, roles)) {
          return true;
        }
        if (me.roles.length === 0) {
          return router.createUrlTree(['/sin-acceso']);
        }
        const fallback = ROL_HOME[me.roles[0]];
        return router.createUrlTree([fallback]);
      }),
    );
  };
}
