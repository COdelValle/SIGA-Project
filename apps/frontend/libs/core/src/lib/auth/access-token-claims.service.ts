import { Injectable, inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo } from '@azure/msal-browser';
import { Observable, from, map, throwError } from 'rxjs';
import { APP_CONFIG } from '../config/app-config.model';
import { Rol } from '../models/role.enum';

const ROLES: readonly Rol[] = ['ADMIN', 'DOCENTE', 'APODERADO', 'ESTUDIANTE'];

export interface AccessTokenClaims {
  roles: Rol[];
  scopes: string[];
}

/**
 * Lee los claims del access token emitido para la API y solo los usa para la
 * navegación/diagnóstico del cliente. La firma, issuer, audience, vigencia y
 * autorización de cada operación se validan siempre en Gateway/backend.
 */
@Injectable({ providedIn: 'root' })
export class AccessTokenClaimsService {
  private readonly msal = inject(MsalService);
  private readonly config = inject(APP_CONFIG);

  getAccessTokenClaims(): Observable<AccessTokenClaims> {
    const instance = this.msal.instance;
    const account = instance.getActiveAccount() ?? instance.getAllAccounts()[0];
    if (!account) {
      return throwError(() => new Error('No existe una cuenta MSAL activa.'));
    }

    return from(
      instance.acquireTokenSilent({ scopes: this.config.msal.scopes, account }),
    ).pipe(map((result) => decodeAccessTokenClaims(result.accessToken)));
  }

  getMissingConfiguredScopes(claims: AccessTokenClaims): string[] {
    const granted = new Set(claims.scopes.map(normalizarScope));
    return this.config.msal.scopes
      .map(normalizarScope)
      .filter((scope) => scope.length > 0 && !granted.has(scope));
  }
}

/**
 * Decodifica el payload para navegación. Decodificar no verifica el token;
 * nunca debe sustituir la validación criptográfica del servidor.
 */
export function decodeAccessTokenClaims(accessToken: string): AccessTokenClaims {
  const segmentos = accessToken.split('.');
  if (segmentos.length !== 3 || !segmentos[1]) {
    throw new Error('El access token no tiene formato JWT.');
  }

  const base64 = segmentos[1].replace(/-/g, '+').replace(/_/g, '/');
  const base64ConPadding = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
  let payload: unknown;
  try {
    const binario = globalThis.atob(base64ConPadding);
    const bytes = Uint8Array.from(binario, (caracter) => caracter.charCodeAt(0));
    payload = JSON.parse(new TextDecoder().decode(bytes));
  } catch {
    throw new Error('No se pudieron leer los claims del access token.');
  }

  if (typeof payload !== 'object' || payload === null) {
    throw new Error('El payload del access token no es un objeto JSON.');
  }

  const claims = payload as { roles?: unknown; scp?: unknown };
  const roles = Array.isArray(claims.roles)
    ? claims.roles.filter((role): role is Rol =>
        typeof role === 'string' && ROLES.includes(role as Rol),
      )
    : [];
  const scopes = typeof claims.scp === 'string' ? claims.scp.split(/\s+/).filter(Boolean) : [];

  return { roles, scopes };
}

function normalizarScope(scope: string): string {
  return scope.trim().split('/').pop() ?? '';
}
