import { AsyncPipe } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { AuthService, MeService } from '@siga/core';

@Component({
  selector: 'siga-portal-shell',
  imports: [AsyncPipe],
  template: `
    <div class="flex min-h-dvh flex-col bg-slate-50">
      <header
        class="flex items-center justify-between gap-4 bg-gradient-to-r from-slate-900 to-blue-800 px-6 py-4 text-white shadow-md"
      >
        <span class="text-lg font-bold tracking-widest">SIGA</span>
        <div class="flex items-center gap-4">
          <div class="text-right leading-tight">
            <p class="text-sm font-medium text-blue-100">{{ portal }}</p>
            @if (usuario$ | async; as me) {
              <p class="text-xs text-blue-200">{{ me.displayName || me.email }}</p>
            }
          </div>
          <button
            type="button"
            (click)="cerrarSesion()"
            class="shrink-0 rounded-full border border-white/30 px-4 py-1.5 text-sm font-semibold text-white transition hover:bg-white/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-300"
          >
            Cerrar Sesión
          </button>
        </div>
      </header>
      <main class="mx-auto w-full max-w-6xl flex-1 px-6 py-8">
        <ng-content />
      </main>
    </div>
  `,
})
export class PortalShellComponent {
  @Input() portal = '';

  private readonly auth = inject(AuthService);
  private readonly meService = inject(MeService);

  readonly usuario$ = this.meService.getMe();

  cerrarSesion(): void {
    this.auth.logout();
  }
}
