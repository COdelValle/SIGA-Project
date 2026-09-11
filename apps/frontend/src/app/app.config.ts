import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptors, withInterceptorsFromDi } from '@angular/common/http';
import { provideRouter } from '@angular/router';
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
    ],
  };
}
