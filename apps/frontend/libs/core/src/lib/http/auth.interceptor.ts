import { HttpEvent, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { Observable, catchError, from, switchMap, throwError } from 'rxjs';
import { AuthErrorService, normalizeAuthError } from '../auth/auth-error.service';
import { APP_CONFIG } from '../config/app-config.model';

/**
 * Adjunta el token de acceso a las llamadas al BFF.
 *
 * A diferencia del MsalInterceptor por defecto, ante un fallo de adquisicion
 * NO dispara un `loginRedirect`: registra el error y lo re-lanza. Asi se evita
 * el loop de login y la pantalla `/error-acceso` puede mostrar el motivo.
 */
export function authInterceptor(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
): Observable<HttpEvent<unknown>> {
  const config = inject(APP_CONFIG);
  const msal = inject(MsalService);
  const authError = inject(AuthErrorService);

  if (!req.url.startsWith(config.bffBaseUrl)) {
    return next(req);
  }

  const account = msal.instance.getActiveAccount() ?? msal.instance.getAllAccounts()[0];
  if (!account) {
    return next(req);
  }

  return from(msal.instance.acquireTokenSilent({ scopes: config.msal.scopes, account })).pipe(
    switchMap((result) =>
      next(
        req.clone({
          setHeaders: { Authorization: `Bearer ${result.accessToken}` },
        }),
      ),
    ),
    catchError((error: unknown) => {
      const normalized = normalizeAuthError(error);
      console.error('[SIGA] No se pudo adquirir el token de acceso', normalized);
      authError.set(normalized);
      return throwError(() => error);
    }),
  );
}
