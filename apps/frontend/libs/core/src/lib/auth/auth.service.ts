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
   */
  logout(postLogoutRedirectUri = '/'): void {
    this.msal.logoutRedirect({ postLogoutRedirectUri });
  }

  isAuthenticated(): boolean {
    return this.msal.instance.getAllAccounts().length > 0;
  }
}
