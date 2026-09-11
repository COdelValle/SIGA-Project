import { InjectionToken } from '@angular/core';

export interface MsalClientConfig {
  clientId: string;
  authority: string;
  redirectUri: string;
  scopes: string[];
}

export interface AppConfig {
  bffBaseUrl: string;
  msal: MsalClientConfig;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');
