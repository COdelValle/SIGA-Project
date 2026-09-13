import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { EventType } from '@azure/msal-browser';
import { MeService, ROL_HOME } from '@siga/core';
import { Subject, filter, takeUntil } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App implements OnInit, OnDestroy {
  private readonly broadcast = inject(MsalBroadcastService);
  private readonly msal = inject(MsalService);
  private readonly meService = inject(MeService);
  private readonly router = inject(Router);
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Tras volver de Azure AD (/auth) resolvemos el rol y llevamos al portal.
    this.broadcast.msalSubject$
      .pipe(
        filter((event) => event.eventType === EventType.LOGIN_SUCCESS),
        takeUntil(this.destroy$),
      )
      .subscribe(() => this.redirigirSegunRol());

    // Refuerzo: si ya hay una sesion activa (por ejemplo, una recarga de la
    // pagina), se vuelve a resolver el rol para no dejar al usuario en el portal.
    if (this.msal.instance.getAllAccounts().length > 0) {
      this.redirigirSegunRol();
    }
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
}
