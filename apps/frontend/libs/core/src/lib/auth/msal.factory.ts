import {
  BrowserCacheLocation,
  InteractionType,
  PublicClientApplication,
} from '@azure/msal-browser';
import { MsalGuardConfiguration, MsalInterceptorConfiguration } from '@azure/msal-angular';
import { AppConfig } from '../config/app-config.model';

export function msalInstanceFactory(config: AppConfig): PublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: config.msal.clientId,
      authority: config.msal.authority,
      redirectUri: config.msal.redirectUri,
      postLogoutRedirectUri: config.msal.postLogoutRedirectUri,
    },
    cache: {
      cacheLocation: BrowserCacheLocation.LocalStorage,
    },
  });
}

export function msalGuardConfigFactory(config: AppConfig): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: { scopes: config.msal.scopes },
  };
}

export function msalInterceptorConfigFactory(config: AppConfig): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, string[]>();
  protectedResourceMap.set(`${config.bffBaseUrl}/*`, config.msal.scopes);

  return {
    interactionType: InteractionType.Redirect,
    protectedResourceMap,
  };
}
