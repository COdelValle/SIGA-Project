import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import {
  HTTP_INTERCEPTORS,
  provideHttpClient,
  withInterceptors,
  withInterceptorsFromDi,
} from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { catchError, lastValueFrom, of } from 'rxjs';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INTERCEPTOR_CONFIG,
  MSAL_INSTANCE,
  MsalBroadcastService,
  MsalGuard,
  MsalInterceptor,
  MsalService,
} from '@azure/msal-angular';
import {
  APP_CONFIG,
  AppConfig,
  AuthErrorService,
  errorInterceptor,
  msalGuardConfigFactory,
  msalInterceptorConfigFactory,
  msalInstanceFactory,
  normalizeAuthError,
} from '@siga/core';
import { InteractionStatus } from '@azure/msal-browser';
import { routes } from './app.routes';
import { limpiarParametrosMsal } from './msal-url.util';

/**
 * Autenticacion simulada para E2E: no hay redireccion a Microsoft y el
 * interceptor sigue adjuntando un JWT de prueba al BFF. Nunca se habilita en
 * el config.json de produccion.
 */
function mockAuthProviders(config: AppConfig): ApplicationConfig['providers'] {
  const token = config.mockAccessToken ?? '';
  const account = {
    homeAccountId: 'e2e-account',
    environment: 'e2e',
    tenantId: 'e2e',
    username: 'e2e@siga.cl',
    localAccountId: 'e2e',
    name: 'E2E SIGA',
  };
  const msalService = {
    instance: {
      getActiveAccount: () => account,
      getAllAccounts: () => [account],
      setActiveAccount: () => undefined,
      acquireTokenSilent: async () => ({
        accessToken: token,
        account,
        scopes: [],
        expiresOn: new Date(Date.now() + 3_600_000),
      }),
      clearCache: async () => undefined,
      logoutRedirect: async () => undefined,
    },
    acquireTokenSilent: () => of({ accessToken: token, account }),
    handleRedirectObservable: () => of(null),
    getLogger: () => ({
      verbose: () => undefined,
      info: () => undefined,
      infoPii: () => undefined,
      error: () => undefined,
      warning: () => undefined,
    }),
  };

  return [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([errorInterceptor]), withInterceptorsFromDi()),
    { provide: APP_CONFIG, useValue: config },
    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useFactory: () => msalInterceptorConfigFactory(config),
    },
    { provide: MsalService, useValue: msalService },
    { provide: MsalGuard, useValue: { canActivate: () => true } },
    {
      provide: MsalBroadcastService,
      useValue: { inProgress$: of(InteractionStatus.None) },
    },
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },
  ];
}

export function appConfig(config: AppConfig): ApplicationConfig {
  if (config.authMode === 'mock') {
    return { providers: mockAuthProviders(config) };
  }

  return {
    providers: [
      provideBrowserGlobalErrorListeners(),
      provideRouter(routes),
      provideHttpClient(withInterceptors([errorInterceptor]), withInterceptorsFromDi()),
      { provide: APP_CONFIG, useValue: config },
      { provide: MSAL_INSTANCE, useFactory: () => msalInstanceFactory(config) },
      { provide: MSAL_GUARD_CONFIG, useFactory: () => msalGuardConfigFactory(config) },
      {
        provide: MSAL_INTERCEPTOR_CONFIG,
        useFactory: () => msalInterceptorConfigFactory(config),
      },
      { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },
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
