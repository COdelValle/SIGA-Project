import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { AuthErrorService, MeService, ROL_HOME, normalizeAuthError } from '@siga/core';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App implements OnInit {
  private readonly msal = inject(MsalService);
  private readonly meService = inject(MeService);
  private readonly router = inject(Router);
  private readonly authError = inject(AuthErrorService);

  ngOnInit(): void {
    // El retorno de Microsoft ya se proceso en el APP_INITIALIZER (app.config.ts).
    // Si fallo, se muestra la pantalla de error en vez de reintentar en silencio.
    if (this.authError.error()) {
      void this.router.navigate(['/error-acceso']);
      return;
    }

    // Si hay sesion activa, resolvemos el rol y llevamos al portal.
    if (this.msal.instance.getAllAccounts().length > 0) {
      this.redirigirSegunRol();
    }
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
      error: (error: unknown) => {
        // Error de token u otro: se muestra la pantalla de error (sin loop).
        const normalized = normalizeAuthError(error);
        console.error('[SIGA] No se pudo resolver el perfil del usuario', normalized);
        this.authError.set(normalized);
        void this.router.navigate(['/error-acceso']);
      },
    });
  }
}
