import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptors, withInterceptorsFromDi } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { catchError, lastValueFrom, of } from 'rxjs';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MSAL_INTERCEPTOR_CONFIG,
  MsalBroadcastService,
  MsalGuard,
  MsalInterceptor,
  MsalService,
} from '@azure/msal-angular';
import {
  APP_CONFIG,
  AppConfig,
  errorInterceptor,
  msalGuardConfigFactory,
  msalInstanceFactory,
  msalInterceptorConfigFactory,
} from '@siga/core';
import { routes } from './app.routes';
import { limpiarParametrosMsal } from './msal-url.util';

export function appConfig(config: AppConfig): ApplicationConfig {
  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes),
      provideHttpClient(withInterceptors([errorInterceptor]), withInterceptorsFromDi()),
      { provide: APP_CONFIG, useValue: config },
      { provide: MSAL_INSTANCE, useFactory: () => msalInstanceFactory(config) },
      { provide: MSAL_GUARD_CONFIG, useFactory: () => msalGuardConfigFactory(config) },
      { provide: MSAL_INTERCEPTOR_CONFIG, useFactory: () => msalInterceptorConfigFactory(config) },
      MsalService,
      MsalGuard,
      MsalBroadcastService,
      { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },
      // Procesa el retorno de Microsoft (login Y logout) y limpia la URL ANTES
      // de que el router haga su navegacion inicial (que corre en el bootstrap
      // listener). Asi el `?state=` del logout no queda pegado en la URL.
      provideAppInitializer(() => {
        const msal = inject(MsalService);
        return lastValueFrom(
          msal
            .handleRedirectObservable({ navigateToLoginRequestUrl: false })
            .pipe(catchError(() => of(null))),
        ).then(() => {
          // Fija una cuenta activa para que el MsalInterceptor sepa a quien pedir
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
