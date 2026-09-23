import { Component, Input } from '@angular/core';
import { NotaItem } from '../models/academico.model';

/** Lista de notas por asignatura con color por calificacion. */
@Component({
  selector: 'siga-notas-lista',
  template: `
    <div class="overflow-hidden rounded-xl border border-gold/60">
      @for (item of items; track item.asignatura) {
        <div
          class="flex items-center justify-between px-4 py-3 text-sm text-ink"
          [class.bg-surface]="$odd"
          [class.bg-panel]="!$odd"
        >
          <span>{{ item.asignatura }}</span>
          <span [class]="'font-semibold ' + notaClass(item.nota)">{{ item.nota.toFixed(1) }}</span>
        </div>
      } @empty {
        <p class="bg-panel px-4 py-6 text-center text-sm text-muted">Sin notas registradas.</p>
      }
    </div>
  `,
})
export class NotasListaComponent {
  @Input() items: NotaItem[] = [];

  protected notaClass(nota: number): string {
    if (nota >= 6) {
      return 'text-ok';
    }
    if (nota < 4) {
      return 'text-bad';
    }
    return 'text-warn';
  }
}
