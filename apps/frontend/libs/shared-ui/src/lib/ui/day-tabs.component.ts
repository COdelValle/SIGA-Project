import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Selector de dias (Lunes..Viernes) como pestanas. El dia activo se resalta y
 * muestra una flecha (cuadrado rotado) apuntando hacia abajo.
 */
@Component({
  selector: 'siga-day-tabs',
  template: `
    <div class="flex flex-wrap gap-1 border-b border-gold/40 sm:gap-2">
      @for (dia of dias; track dia) {
        <button
          type="button"
          (click)="seleccionar(dia)"
          class="relative rounded-t-lg px-3 py-1.5 text-sm font-medium transition"
          [class.text-brand]="dia === selected"
          [class.font-semibold]="dia === selected"
          [class.text-ink]="dia !== selected"
          [class.hover:text-gold]="dia !== selected"
        >
          {{ dia }}
          @if (dia === selected) {
            <span
              class="absolute -bottom-[5px] left-1/2 h-2.5 w-2.5 -translate-x-1/2 rotate-45 border-r border-b border-gold bg-page"
              aria-hidden="true"
            ></span>
          }
        </button>
      }
    </div>
  `,
})
export class DayTabsComponent {
  @Input() dias: readonly string[] = [];
  @Input() selected = '';
  @Output() readonly selectedChange = new EventEmitter<string>();

  seleccionar(dia: string): void {
    this.selectedChange.emit(dia);
  }
}
