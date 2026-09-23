import { Component, Input } from '@angular/core';
import { HorarioBloque } from '../models/academico.model';

/** Tabla de bloques de horario: Hora / Asignatura / Profesor(a) / Sala. */
@Component({
  selector: 'siga-horario-tabla',
  template: `
    <div class="overflow-hidden rounded-xl border border-gold/60">
      <table class="w-full border-collapse text-left text-sm">
        <thead>
          <tr class="bg-bar text-gold">
            <th class="px-4 py-3 font-semibold">Hora</th>
            <th class="px-4 py-3 font-semibold">Asignatura</th>
            <th class="px-4 py-3 font-semibold">Profesor(a)</th>
            <th class="px-4 py-3 font-semibold">Sala</th>
          </tr>
        </thead>
        <tbody>
          @for (bloque of bloques; track $index) {
            <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
              <td class="px-4 py-3 align-middle">{{ bloque.hora }}</td>
              <td class="px-4 py-3 align-middle">{{ bloque.asignatura }}</td>
              <td class="px-4 py-3 align-middle">{{ bloque.profesor }}</td>
              <td class="px-4 py-3 align-middle">{{ bloque.sala }}</td>
            </tr>
          } @empty {
            <tr class="bg-panel text-muted">
              <td class="px-4 py-6 text-center" colspan="4">Sin bloques para este día.</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class HorarioTablaComponent {
  @Input() bloques: HorarioBloque[] = [];
}
