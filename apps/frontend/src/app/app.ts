import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { MeService, ROL_HOME } from '@siga/core';
import { Subject, takeUntil } from 'rxjs';

const MSAL_URL_PARAMS = [
  'state',
  'code',
  'error',
  'error_description',
  'session_state',
  'client_info',
  'ear_jwe',
];

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App implements OnInit, OnDestroy {
  private readonly msal = inject(MsalService);
  private readonly meService = inject(MeService);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Procesa el retorno de Microsoft (login Y logout) en cualquier ruta.
    // Si no se procesa, el estado SIGNOUT queda "en progreso" en sessionStorage
    // y bloquea los siguientes inicios de sesion (interaction_in_progress).
    this.msal
      .handleRedirectObservable({ navigateToLoginRequestUrl: false })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.limpiarParametrosMsal(),
        error: (error) => {
          console.error('Error procesando el redirect de MSAL', error);
          this.limpiarParametrosMsal();
        },
        complete: () => {
          if (this.msal.instance.getAllAccounts().length > 0) {
            this.redirigirSegunRol();
          }
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private redirigirSegunRol(): void {
    this.meService.getMe(true).subscribe({
      next: (me) => {
        if (me.roles.length > 0) {
          void this.router.navigate([ROL_HOME[me.roles[0]]]);
          return;
        }
        // Autenticado en Microsoft pero sin registro/rol en SIGA.
        this.meService.clear();
        void this.router.navigate(['/sin-acceso']);
      },
      error: () => {
        // 401 u otro error: se mantiene en el portal publico.
      },
    });
  }

  /** Quita de la URL los parametros de respuesta de MSAL (state, code, ...). */
  private limpiarParametrosMsal(): void {
    const url = new URL(window.location.href);
    let cambio = false;

    MSAL_URL_PARAMS.forEach((param) => {
      if (url.searchParams.has(param)) {
        url.searchParams.delete(param);
        cambio = true;
      }
    });

    const hash = url.hash.startsWith('#') ? url.hash.slice(1) : '';
    if (hash.includes('=')) {
      const hashParams = new URLSearchParams(hash);
      let hashCambio = false;
      MSAL_URL_PARAMS.forEach((param) => {
        if (hashParams.has(param)) {
          hashParams.delete(param);
          hashCambio = true;
        }
      });
      if (hashCambio) {
        const restante = hashParams.toString();
        url.hash = restante ? `#${restante}` : '';
        cambio = true;
      }
    }

    if (cambio) {
      window.history.replaceState({}, document.title, `${url.pathname}${url.search}${url.hash}`);
    }
  }
}
