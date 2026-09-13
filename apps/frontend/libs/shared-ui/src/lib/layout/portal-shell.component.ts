import { Component, Input } from '@angular/core';

@Component({
  selector: 'siga-portal-shell',
  template: `
    <div class="flex min-h-dvh flex-col bg-slate-50">
      <header
        class="flex items-center justify-between bg-gradient-to-r from-slate-900 to-blue-800 px-6 py-4 text-white shadow-md"
      >
        <span class="text-lg font-bold tracking-widest">SIGA</span>
        <span class="text-sm font-medium text-blue-100">{{ portal }}</span>
      </header>
      <main class="mx-auto w-full max-w-6xl flex-1 px-6 py-8">
        <ng-content />
      </main>
    </div>
  `,
})
export class PortalShellComponent {
  @Input() portal = '';
}
