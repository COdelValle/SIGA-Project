import { Component, OnInit, inject } from '@angular/core';
import { MsalService } from '@azure/msal-angular';

/**
 * Componente de la ruta /auth. Procesa el redirect de Azure AD sin volver a la
 * pagina de origen (navigateToLoginRequestUrl: false), evitando la recarga que
 * impedia que se disparara LOGIN_SUCCESS y la resolucion del rol.
 */
@Component({
  selector: 'siga-auth-redirect',
  template: '',
})
export class AuthRedirectComponent implements OnInit {
  private readonly msal = inject(MsalService);

  ngOnInit(): void {
    this.msal.handleRedirectObservable({ navigateToLoginRequestUrl: false }).subscribe({
      error: (error) => console.error('Error procesando el redirect de MSAL', error),
    });
  }
}
