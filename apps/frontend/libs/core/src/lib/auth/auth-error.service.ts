import { Injectable, signal } from '@angular/core';

export interface AuthError {
  code?: string;
  message: string;
  detail?: string;
}

/**
 * Guarda el ultimo error de autenticacion (MSAL/token) para que la pantalla
 * `/error-acceso` pueda mostrarlo. Evita que un fallo quede silencioso.
 */
@Injectable({ providedIn: 'root' })
export class AuthErrorService {
  private readonly current = signal<AuthError | null>(null);

  readonly error = this.current.asReadonly();

  set(error: AuthError): void {
    this.current.set(error);
  }

  clear(): void {
    this.current.set(null);
  }
}

/**
 * Normaliza errores de MSAL y de HttpErrorResponse a un mensaje legible.
 * MSAL expone `errorCode`/`errorMessage`; axios/Http expone `status`/`message`.
 */
export function normalizeAuthError(error: unknown): AuthError {
  const value = error as {
    errorCode?: string;
    errorMessage?: string;
    subError?: string;
    correlationId?: string;
    message?: string;
    name?: string;
    status?: number;
    error?: { error?: string; error_description?: string };
  } | null;

  const code = value?.errorCode ?? value?.error?.error ?? value?.name;
  const message =
    value?.error?.error_description ??
    value?.errorMessage ??
    value?.message ??
    'No se pudo completar la autenticacion con Microsoft.';
  const detail = [value?.subError, value?.correlationId].filter((part) => !!part).join(' · ');

  return { code, message, detail: detail || undefined };
}
