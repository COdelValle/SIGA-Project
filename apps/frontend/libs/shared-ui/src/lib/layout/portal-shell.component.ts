import { Component, Input } from '@angular/core';

@Component({
  selector: 'siga-portal-shell',
  template: `
    <header class="siga-header">
      <span class="siga-brand">SIGA</span>
      <span class="siga-portal">{{ portal }}</span>
    </header>
    <main class="siga-main">
      <ng-content />
    </main>
  `,
  styles: [
    `
      :host {
        display: block;
        min-height: 100dvh;
        font-family: 'Inter', system-ui, -apple-system, 'Segoe UI', sans-serif;
      }
      .siga-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 1rem 1.5rem;
        background: #1f3a5f;
        color: #ffffff;
      }
      .siga-brand {
        font-weight: 700;
        letter-spacing: 0.08em;
      }
      .siga-portal {
        opacity: 0.85;
      }
      .siga-main {
        padding: 1.5rem;
      }
    `,
  ],
})
export class PortalShellComponent {
  @Input() portal = '';
}
