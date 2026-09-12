import { Injectable } from '@angular/core';
import { PublicClientApplication, AuthenticationResult, AccountInfo } from '@azure/msal-browser';
import { entorno } from '../../../entornos/entorno.desarrollo';
import { from, Observable, tap } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ServicioAutenticacion {
  private pca: PublicClientApplication;
  private inicializacionPromise: Promise<void>;

  constructor() {
    this.pca = new PublicClientApplication({
      auth: {
        clientId: entorno.azure.idCliente,
        authority: `https://login.microsoftonline.com/${entorno.azure.idInquilino}`,
        redirectUri: entorno.azure.uriRedireccion
      },
      cache: {
        cacheLocation: 'localStorage'
      }
    });

    this.inicializacionPromise = this.pca.initialize();
  }

  iniciarSesion(): Observable<AuthenticationResult> {
    return from(
      this.inicializacionPromise.then(() =>
        this.pca.loginPopup({
          scopes: ['user.read'],
          prompt: 'select_account'
        })
      )
    ).pipe(
      tap((resultado: AuthenticationResult) => {
        if (resultado.account) {
          this.pca.setActiveAccount(resultado.account);
        }
      })
    );
  }

  cerrarSesion(): Observable<void> {
    return from(
      this.inicializacionPromise.then(() => this.pca.logoutPopup())
    );
  }

  obtenerUsuarioActual(): AccountInfo | null {
    return this.pca.getActiveAccount();
  }
}