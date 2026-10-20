import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of, switchMap } from 'rxjs';
import { AccessTokenClaimsService } from './access-token-claims.service';
import { AuthErrorService, normalizeAuthError } from './auth-error.service';
import { ROL_HOME, Rol } from '../models/role.enum';
import { MeService } from './me.service';

export function roleGuard(roles: Rol[]): CanActivateFn {
  return () => {
    const meService = inject(MeService);
    const claimsService = inject(AccessTokenClaimsService);
    const router = inject(Router);
    const authError = inject(AuthErrorService);

    return meService.getMe().pipe(
      switchMap((me) => {
        if (me.roles.length === 0) {
          return of(router.createUrlTree(['/sin-acceso']));
        }

        return claimsService.getAccessTokenClaims().pipe(
          map((claims) => {
            const rolesComprobados = me.roles.filter((rol) => claims.roles.includes(rol));
            const scopesFaltantes = claimsService.getMissingConfiguredScopes(claims);

            if (scopesFaltantes.length > 0) {
              authError.set({
                code: 'missing_scope',
                message: 'El access token no contiene todos los scopes configurados para SIGA.',
                detail: scopesFaltantes.join(', '),
              });
              return router.createUrlTree(['/error-acceso']);
            }

            if (rolesComprobados.length === 0) {
              authError.set({
                code: 'role_claim_mismatch',
                message: 'Los roles del perfil SIGA no coinciden con los claims del access token.',
              });
              return router.createUrlTree(['/sin-acceso']);
            }

            const rolPermitido = rolesComprobados.find((rol) => roles.includes(rol));
            if (rolPermitido) {
              return true;
            }

            return router.createUrlTree([ROL_HOME[rolesComprobados[0]]]);
          }),
        );
      }),
      catchError((error: unknown) => {
        authError.set(normalizeAuthError(error));
        return of(router.createUrlTree(['/error-acceso']));
      }),
    );
  };
}
