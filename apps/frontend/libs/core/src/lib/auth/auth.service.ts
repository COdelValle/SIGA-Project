import { Injectable, inject } from '@angular/core';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly msal = inject(MsalService);
  private readonly broadcast = inject(MsalBroadcastService);

  login(): void {
    this.msal.loginRedirect();
  }

  /**
   * Cierra la sesion de Microsoft y redirige al destino indicado.
   * Por defecto vuelve al portal publico ("/"); el rechazo de cuenta usa
   * "/sin-acceso". Ambos deben estar registrados como redirect URIs en Azure.
   *
   * Limpia las cuentas/tokens cacheados antes de redirigir para que no quede
   * sesion residual (sin esto, cambiar de usuario requeria borrar el storage).
   */
  logout(postLogoutRedirectUri = '/'): void {
    const instance = this.msal.instance;
    instance.setActiveAccount(null);
    void instance
      .clearCache()
      .then(() => instance.logoutRedirect({ postLogoutRedirectUri }));
  }

  isAuthenticated(): boolean {
    return this.msal.instance.getAllAccounts().length > 0;
  }
}
