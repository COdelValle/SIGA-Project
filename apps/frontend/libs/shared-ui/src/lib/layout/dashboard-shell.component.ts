import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MenuIconComponent } from './menu-icon.component';
import { MenuItem } from './menu-item.model';
import { PortalHeaderComponent } from './portal-header.component';

/**
 * Layout general de los portales: header sticky arriba, menu lateral fijo a la
 * izquierda y contenido scrolleable. El menu se configura por rol.
 */
@Component({
  selector: 'siga-dashboard-shell',
  imports: [RouterLink, RouterLinkActive, MenuIconComponent, PortalHeaderComponent],
  template: `
    <div class="flex h-dvh flex-col bg-page text-ink">
      <siga-portal-header [portal]="portal" />

      <!-- Navegacion compacta para pantallas pequenas -->
      <nav class="flex gap-2 overflow-x-auto bg-bar-alt px-3 py-2 sm:hidden">
        @for (item of menu; track item.route) {
          <a
            [routerLink]="item.route"
            routerLinkActive="bg-white/15 text-white"
            [routerLinkActiveOptions]="{ exact: true }"
            class="flex shrink-0 items-center gap-2 rounded-lg px-3 py-1.5 text-xs font-medium text-white/90 transition hover:bg-white/10 hover:text-white"
          >
            <siga-menu-icon [name]="item.icon" class="h-4 w-4 shrink-0 text-gold" />
            <span>{{ item.label }}</span>
          </a>
        }
      </nav>

      <div class="flex min-h-0 flex-1">
        <aside class="hidden w-64 shrink-0 overflow-y-auto bg-bar-alt py-4 sm:block">
          <nav class="flex flex-col gap-1 px-3">
            @for (item of menu; track item.route) {
              <a
                [routerLink]="item.route"
                routerLinkActive="border-gold bg-white/12 text-white"
                [routerLinkActiveOptions]="{ exact: true }"
                class="flex items-center gap-3 rounded-lg border border-transparent px-3 py-2.5 text-sm font-medium text-white/90 transition hover:bg-white/10 hover:text-white"
              >
                <siga-menu-icon [name]="item.icon" class="h-6 w-6 shrink-0 text-gold" />
                <span>{{ item.label }}</span>
              </a>
            }
          </nav>
        </aside>

        <main class="min-h-0 flex-1 overflow-y-auto px-4 py-6 sm:px-8">
          <ng-content />
        </main>
      </div>
    </div>
  `,
})
export class DashboardShellComponent {
  @Input() portal = '';
  @Input() menu: MenuItem[] = [];
}
