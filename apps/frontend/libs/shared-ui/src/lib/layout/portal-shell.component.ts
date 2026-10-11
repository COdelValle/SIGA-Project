import { Component, Input } from '@angular/core';
import { PortalHeaderComponent } from './portal-header.component';

/**
 * Layout simple (header + contenido centrado). Se mantiene para vistas que no
 * usan el dashboard (por ejemplo, admin en fase previa).
 */
@Component({
  selector: 'siga-portal-shell',
  imports: [PortalHeaderComponent],
  template: `
    <div class="flex min-h-dvh flex-col bg-page text-ink">
      <siga-portal-header [portal]="portal" />
      <main class="mx-auto w-full max-w-6xl flex-1 px-6 py-8">
        <ng-content />
      </main>
    </div>
  `,
})
export class PortalShellComponent {
  @Input() portal = '';
}
