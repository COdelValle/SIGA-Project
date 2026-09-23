import { Component, Input, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService, MeService, ThemeService } from '@siga/core';

@Component({
  selector: 'siga-portal-header',
  template: `
    <header
      class="flex items-center justify-between gap-4 border-b-2 border-gold bg-bar px-6 py-3 text-onbar shadow-md"
    >
      <div class="flex items-baseline gap-3">
        <span class="text-lg font-bold tracking-wide text-gold">SIGA Virtual</span>
        @if (portal) {
          <span class="hidden text-sm text-onbar sm:inline">{{ portal }}</span>
        }
      </div>

      <div class="flex items-center gap-3">
        <button
          type="button"
          (click)="theme.toggle()"
          class="flex h-9 w-9 items-center justify-center rounded-full border border-gold/50 text-gold transition hover:bg-gold/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-gold"
          [attr.aria-label]="theme.theme() === 'dark' ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'"
        >
          <svg
            class="h-5 w-5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            @if (theme.theme() === 'dark') {
              <circle cx="12" cy="12" r="4" />
              <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
            } @else {
              <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
            }
          </svg>
        </button>

        <div class="hidden text-right leading-tight sm:block">
          <p class="text-sm font-medium text-gold">{{ displayName() }}</p>
          <p class="text-xs text-onbar">{{ email() }}</p>
        </div>

        <span
          class="flex h-10 w-10 items-center justify-center rounded-full border-2 border-gold text-sm font-bold text-gold"
          [attr.aria-label]="'Usuario ' + displayName()"
        >
          {{ initials() }}
        </span>

        <button
          type="button"
          (click)="cerrarSesion()"
          class="shrink-0 rounded-full border border-gold/50 px-4 py-1.5 text-sm font-semibold text-gold transition hover:bg-gold/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-gold"
        >
          Cerrar Sesion
        </button>
      </div>
    </header>
  `,
})
export class PortalHeaderComponent {
  @Input() portal = '';

  private readonly auth = inject(AuthService);
  private readonly meService = inject(MeService);
  protected readonly theme = inject(ThemeService);

  private readonly me = toSignal(this.meService.getMe(), { initialValue: null });

  protected readonly displayName = computed(
    () => this.me()?.displayName || this.me()?.email || '',
  );
  protected readonly email = computed(() => this.me()?.email ?? '');
  protected readonly initials = computed(() => {
    const parts = this.displayName().split(/\s+/).filter(Boolean);
    const letters = parts.slice(0, 2).map((part) => part.charAt(0).toUpperCase());
    return letters.join('') || '?';
  });

  cerrarSesion(): void {
    this.auth.logout();
  }
}
