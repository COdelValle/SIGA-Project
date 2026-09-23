import { Component, Input } from '@angular/core';
import { AsistenciaResumen } from '../models/academico.model';

/** Lista compacta de asistencia (Inicio): asignatura, barra y porcentaje. */
@Component({
  selector: 'siga-asistencia-lista',
  template: `
    <div class="overflow-hidden rounded-xl border border-gold/60">
      @for (item of items; track item.id) {
        <div
          class="flex items-center gap-3 px-4 py-3 text-sm text-ink"
          [class.bg-surface]="$odd"
          [class.bg-panel]="!$odd"
        >
          <span class="w-40 shrink-0">{{ item.asignatura }}</span>
          <div class="h-3 flex-1 overflow-hidden rounded-full bg-ink/15">
            <div
              [class]="'h-full rounded-full ' + barClass(item.porcentaje)"
              [style.width.%]="item.porcentaje"
            ></div>
          </div>
          <span class="w-12 shrink-0 text-right font-semibold">{{ item.porcentaje }}%</span>
        </div>
      } @empty {
        <p class="bg-panel px-4 py-6 text-center text-sm text-muted">Sin datos de asistencia.</p>
      }
    </div>
  `,
})
export class AsistenciaListaComponent {
  @Input() items: AsistenciaResumen[] = [];

  protected barClass(porcentaje: number): string {
    if (porcentaje >= 85) {
      return 'bg-ok';
    }
    if (porcentaje >= 70) {
      return 'bg-warn';
    }
    return 'bg-bad';
  }
}
