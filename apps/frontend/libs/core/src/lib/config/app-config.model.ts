import { InjectionToken } from '@angular/core';

export interface MsalClientConfig {
  clientId: string;
  authority: string;
  redirectUri: string;
  postLogoutRedirectUri: string;
  scopes: string[];
}

export interface AppConfig {
  bffBaseUrl: string;
  /** Fuerza el uso de datos mock en los servicios que soportan fallback. */
  useMocks: boolean;
  msal: MsalClientConfig;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');
