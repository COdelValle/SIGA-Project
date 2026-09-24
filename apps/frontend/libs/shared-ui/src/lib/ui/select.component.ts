import { Component, ElementRef, EventEmitter, HostListener, Input, Output, inject, signal } from '@angular/core';
import { SelectOption } from './select.model';

/**
 * Selector propio (reemplaza al <select> nativo). Al abrirse, el disparador y el
 * panel se fusionan (mismo borde/fondo, sin separacion) para verse como un solo
 * objeto expandido.
 */
@Component({
  selector: 'siga-select',
  template: `
    <div class="relative">
      <button
        type="button"
        (click)="alternar($event)"
        [attr.aria-expanded]="abierto()"
        [attr.aria-label]="ariaLabel || null"
        [attr.title]="etiquetaSeleccionada()"
        class="flex w-full items-center justify-between gap-3 border border-line bg-panel py-3 pl-4 pr-10 text-left text-base text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
        [class.rounded-xl]="!abierto()"
        [class.rounded-t-xl]="abierto()"
        [class.border-b-0]="abierto()"
      >
        <span class="break-words whitespace-normal">{{ etiquetaSeleccionada() }}</span>
        <svg
          class="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gold transition-transform"
          [class.rotate-180]="abierto()"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="m6 9 6 6 6-6" />
        </svg>
      </button>

      @if (abierto()) {
        <ul
          role="listbox"
          class="absolute left-0 right-0 top-full z-30 max-h-60 overflow-auto rounded-b-xl border border-t-0 border-line bg-panel"
        >
          @for (opcion of options; track opcion.value) {
            <li role="option" [attr.aria-selected]="opcion.value === value">
              <button
                type="button"
                (click)="seleccionar(opcion, $event)"
                [attr.title]="opcion.label"
                class="block w-full break-words border border-transparent px-4 py-2.5 text-left text-sm text-ink transition hover:border-gold hover:bg-gold/10"
                [class.border-gold]="opcion.value === value"
              >
                {{ opcion.label }}
              </button>
            </li>
          }
        </ul>
      }
    </div>
  `,
})
export class SelectComponent {
  @Input() options: SelectOption[] = [];
  @Input() value: string | number | null = null;
  @Input() ariaLabel = '';
  @Output() readonly valueChange = new EventEmitter<string | number>();

  protected readonly abierto = signal(false);

  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  @HostListener('document:click', ['$event'])
  protected onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.abierto.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  protected onEscape(): void {
    this.abierto.set(false);
  }

  protected etiquetaSeleccionada(): string {
    const opcion = this.options.find((item) => item.value === this.value);
    return opcion?.label ?? '';
  }

  protected alternar(event: MouseEvent): void {
    event.stopPropagation();
    this.abierto.update((valor) => !valor);
  }

  protected seleccionar(opcion: SelectOption, event: MouseEvent): void {
    event.stopPropagation();
    this.valueChange.emit(opcion.value);
    this.abierto.set(false);
  }
}
