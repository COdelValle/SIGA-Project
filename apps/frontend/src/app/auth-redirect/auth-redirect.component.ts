import { Component, OnInit, inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';

/**
 * Componente de la ruta /auth. Procesa el redirect de Azure AD sin volver a la
 * pagina de origen (navigateToLoginRequestUrl: false), evitando la recarga que
 * impedia que se disparara LOGIN_SUCCESS y la resolucion del rol. Mientras se
 * procesa el redirect y se resuelve el rol se muestra un indicador de carga.
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
export class AuthRedirectComponent implements OnInit {
  private readonly msal = inject(MsalService);

  ngOnInit(): void {
    this.msal.handleRedirectObservable({ navigateToLoginRequestUrl: false }).subscribe({
      error: (error) => console.error('Error procesando el redirect de MSAL', error),
    });
  }
}
