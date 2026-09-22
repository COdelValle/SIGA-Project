import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { catchError, lastValueFrom, of } from 'rxjs';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MsalBroadcastService,
  MsalGuard,
  MsalService,
} from '@azure/msal-angular';
import {
  APP_CONFIG,
  AppConfig,
  AuthErrorService,
  authInterceptor,
  errorInterceptor,
  msalGuardConfigFactory,
  msalInstanceFactory,
  normalizeAuthError,
} from '@siga/core';
import { routes } from './app.routes';
import { limpiarParametrosMsal } from './msal-url.util';

export function appConfig(config: AppConfig): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes),
      provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
      { provide: APP_CONFIG, useValue: config },
      { provide: MSAL_INSTANCE, useFactory: () => msalInstanceFactory(config) },
      { provide: MSAL_GUARD_CONFIG, useFactory: () => msalGuardConfigFactory(config) },
      MsalService,
      MsalGuard,
      MsalBroadcastService,
      // Procesa el retorno de Microsoft (login Y logout) y limpia la URL ANTES
      // de que el router haga su navegacion inicial (que corre en el bootstrap
      // listener). Asi el `?state=` del logout no queda pegado en la URL.
      provideAppInitializer(() => {
        const msal = inject(MsalService);
        const authError = inject(AuthErrorService);
        return lastValueFrom(
          msal.handleRedirectObservable({ navigateToLoginRequestUrl: false }).pipe(
            catchError((error: unknown) => {
              const normalized = normalizeAuthError(error);
              console.error('[SIGA] Error al procesar el retorno de Microsoft', normalized);
              authError.set(normalized);
              return of(null);
            }),
          ),
        ).then(() => {
          // Fija una cuenta activa para que el interceptor sepa a quien pedir
          // el token (evita depender de getAllAccounts()[0]).
          if (msal.instance.getActiveAccount() === null) {
            const [account] = msal.instance.getAllAccounts();
            if (account) {
              msal.instance.setActiveAccount(account);
            }
          }
          limpiarParametrosMsal();
        });
      }),
    ],
  };
}
