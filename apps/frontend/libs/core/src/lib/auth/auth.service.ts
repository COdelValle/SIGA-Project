import { Injectable, inject } from '@angular/core';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly msal = inject(MsalService);
  private readonly broadcast = inject(MsalBroadcastService);

  login(): void {
    this.msal.loginRedirect();
  }

  logout(): void {
    this.msal.logoutRedirect();
  }

  isAuthenticated(): boolean {
    return this.msal.instance.getAllAccounts().length > 0;
  }
}
