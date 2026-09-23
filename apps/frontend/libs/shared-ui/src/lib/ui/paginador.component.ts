import { Component, EventEmitter, Input, Output } from '@angular/core';

/**
 * Paginador reutilizable: "Mostrando X–Y de Z", botones Anterior/Siguiente y
 * numeros de pagina. Emite la pagina seleccionada (1-indexada).
 */
@Component({
  selector: 'siga-paginador',
  template: `
    @if (total > 0) {
      <div class="flex flex-wrap items-center justify-between gap-3 px-4 py-3 text-sm">
        <p class="text-muted">Mostrando {{ desde }}–{{ hasta }} de {{ total }}</p>

        <div class="flex flex-wrap items-center gap-1">
          <button
            type="button"
            (click)="ir(page - 1)"
            [disabled]="page <= 1"
            class="rounded-lg border border-gold/60 px-3 py-1.5 text-xs font-semibold text-gold transition hover:bg-gold/10 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Anterior
          </button>

          @for (numero of paginas; track numero) {
            <button
              type="button"
              (click)="ir(numero)"
              class="h-8 w-8 rounded-lg border text-xs font-semibold transition"
              [class.border-gold]="numero === page"
              [class.bg-gold/15]="numero === page"
              [class.text-gold]="numero === page"
              [class.border-transparent]="numero !== page"
              [class.text-ink]="numero !== page"
              [class.hover:border-gold]="numero !== page"
              [class.hover:text-gold]="numero !== page"
            >
              {{ numero }}
            </button>
          }

          <button
            type="button"
            (click)="ir(page + 1)"
            [disabled]="page >= totalPaginas"
            class="rounded-lg border border-gold/60 px-3 py-1.5 text-xs font-semibold text-gold transition hover:bg-gold/10 disabled:cursor-not-allowed disabled:opacity-40"
          >
            Siguiente
          </button>
        </div>
      </div>
    }
  `,
})
export class PaginadorComponent {
  @Input() total = 0;
  @Input() page = 1;
  @Input() pageSize = 10;
  @Output() readonly pageChange = new EventEmitter<number>();

  get totalPaginas(): number {
    return Math.max(1, Math.ceil(this.total / this.pageSize));
  }

  get paginas(): number[] {
    return Array.from({ length: this.totalPaginas }, (_, index) => index + 1);
  }

  get desde(): number {
    return this.total === 0 ? 0 : (this.page - 1) * this.pageSize + 1;
  }

  get hasta(): number {
    return Math.min(this.page * this.pageSize, this.total);
  }

  ir(pagina: number): void {
    if (pagina < 1 || pagina > this.totalPaginas || pagina === this.page) {
      return;
    }
    this.pageChange.emit(pagina);
  }
}
