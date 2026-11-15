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
  /** Modo de autenticacion. `mock` solo se usa en builds/pruebas E2E. */
  authMode?: 'msal' | 'mock';
  /** JWT de prueba para E2E; nunca debe configurarse en produccion. */
  mockAccessToken?: string;
  msal: MsalClientConfig;
}

export const APP_CONFIG = new InjectionToken<AppConfig>('APP_CONFIG');
