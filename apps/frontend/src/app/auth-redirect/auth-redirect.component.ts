import { Component } from '@angular/core';

/**
 * Componente de la ruta /auth. Muestra el indicador de carga mientras el
 * componente raiz (App) procesa el redirect de Azure AD y resuelve el rol.
 */
@Component({
  selector: 'siga-auth-redirect',
  template: `
    <div
      class="flex min-h-dvh flex-col items-center justify-center gap-4 bg-slate-50 text-slate-600"
    >
      <span
        class="h-10 w-10 animate-spin rounded-full border-4 border-slate-200 border-t-blue-600"
        aria-hidden="true"
      ></span>
      <p class="text-sm font-medium">Iniciando sesión…</p>
    </div>
  `,
})
export class AuthRedirectComponent {}
