import { Component, Input } from '@angular/core';
import { HorarioResumen } from '../models/academico.model';

/**
 * Resumen de horario para Inicio: cada fila muestra Horario, Asignatura,
 * Profesor(a) y Ubicacion, con franjas alternadas (mismo estilo que el detalle).
 */
@Component({
  selector: 'siga-horario-resumen',
  template: `
    <div class="overflow-hidden rounded-xl border border-gold/60">
      @for (bloque of bloques; track $index) {
        <div
          class="grid grid-cols-1 gap-x-4 gap-y-1 px-4 py-3 sm:grid-cols-4"
          [class.bg-surface]="$odd"
          [class.bg-panel]="!$odd"
        >
          <div>
            <p class="text-xs font-semibold text-muted">Horario</p>
            <p class="text-sm text-ink">{{ bloque.hora }}</p>
          </div>
          <div>
            <p class="text-xs font-semibold text-muted">Asignatura</p>
            <p class="text-sm text-ink">{{ bloque.asignatura }}</p>
          </div>
          <div>
            <p class="text-xs font-semibold text-muted">Profesor(a)</p>
            <p class="text-sm text-ink">{{ bloque.profesor }}</p>
          </div>
          <div>
            <p class="text-xs font-semibold text-muted">Ubicación</p>
            <p class="text-sm text-ink">{{ bloque.sala }}</p>
          </div>
        </div>
      } @empty {
        <p class="bg-panel px-4 py-6 text-center text-sm text-muted">
          Sin bloques para este día.
        </p>
      }
    </div>
  `,
})
export class HorarioResumenComponent {
  @Input() bloques: HorarioResumen[] = [];
}
