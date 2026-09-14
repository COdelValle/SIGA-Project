import { Component, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { MeService, ROL_HOME } from '@siga/core';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App implements OnInit {
  private readonly msal = inject(MsalService);
  private readonly meService = inject(MeService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    // El retorno de Microsoft (login y logout) ya se proceso y limpio en el
    // APP_INITIALIZER (app.config.ts), antes de la navegacion inicial del router.
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
      error: () => {
        // 401 u otro error: se mantiene en el portal publico.
      },
    });
  }
}
