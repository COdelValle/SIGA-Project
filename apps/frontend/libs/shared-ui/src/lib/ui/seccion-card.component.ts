import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Tarjeta de seccion reutilizable: banda de titulo + accion opcional ("Ver ...")
 * en magenta oscuro con linea dorada, y el contenido debajo.
 */
@Component({
  selector: 'siga-seccion-card',
  imports: [RouterLink],
  template: `
    <section class="overflow-hidden rounded-2xl bg-panel shadow-lg">
      <header
        class="flex flex-wrap items-center justify-between gap-3 border-b border-gold bg-bar px-4 py-3"
      >
        <h3 class="text-lg font-semibold text-gold">{{ title }}</h3>
        @if (actionLabel && actionRoute) {
          <a
            [routerLink]="actionRoute"
            [queryParams]="actionQueryParams ?? {}"
            class="rounded-lg border border-gold/60 px-4 py-1.5 text-sm font-semibold text-gold transition hover:bg-gold/10"
          >
            {{ actionLabel }}
          </a>
        }
      </header>
      <div class="p-4">
        <ng-content />
      </div>
    </section>
  `,
})
export class SeccionCardComponent {
  @Input() title = '';
  @Input() actionLabel?: string;
  @Input() actionRoute?: string;
  @Input() actionQueryParams?: Record<string, string>;
}
