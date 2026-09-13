import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { MsalBroadcastService } from '@azure/msal-angular';
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
      .subscribe(() => {
        this.meService.getMe(true).subscribe((me) => {
          if (me.roles.length > 0) {
            void this.router.navigate([ROL_HOME[me.roles[0]]]);
            return;
          }
          // Autenticado en Microsoft pero sin registro/rol en SIGA.
          this.meService.clear();
          void this.router.navigate(['/sin-acceso']);
        });
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
