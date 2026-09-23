import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AsistenciaResumen } from '../models/academico.model';

/**
 * Tabla de asistencia (vista Asistencias): resumen por asignatura con el
 * detalle de clases y acceso al historial.
 */
@Component({
  selector: 'siga-asistencia-tabla',
  imports: [RouterLink],
  template: `
    <div class="overflow-hidden rounded-xl border border-gold/60">
      <table class="w-full border-collapse text-left text-sm">
        <thead>
          <tr class="bg-bar text-gold">
            <th class="px-4 py-3 font-semibold">Asignatura</th>
            <th class="px-4 py-3 text-center font-semibold">N° Clases Registradas</th>
            <th class="px-4 py-3 text-center font-semibold">N° Clases Asistidas</th>
            <th class="px-4 py-3 text-center font-semibold">Porcentaje de Asistencia Actual</th>
            <th class="px-4 py-3 text-right font-semibold">Detalle</th>
          </tr>
        </thead>
        <tbody>
          @for (item of items; track item.id) {
            <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
              <td class="px-4 py-3 align-middle">{{ item.asignatura }}</td>
              <td class="px-4 py-3 text-center align-middle">{{ item.clasesRegistradas }}</td>
              <td class="px-4 py-3 text-center align-middle">{{ item.clasesAsistidas }}</td>
              <td [class]="'px-4 py-3 text-center align-middle font-semibold ' + pctClass(item.porcentaje)">
                {{ item.porcentaje }}%
              </td>
              <td class="px-4 py-3 text-right align-middle">
                <a
                  [routerLink]="[item.id]"
                  class="inline-block rounded-lg border border-gold/60 px-3 py-1.5 text-xs font-semibold text-gold transition hover:bg-gold/10"
                >
                  Ver Historial
                </a>
              </td>
            </tr>
          } @empty {
            <tr class="bg-panel text-muted">
              <td class="px-4 py-6 text-center" colspan="5">Sin datos de asistencia.</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class AsistenciaTablaComponent {
  @Input() items: AsistenciaResumen[] = [];

  protected pctClass(porcentaje: number): string {
    if (porcentaje >= 85) {
      return 'text-ok';
    }
    if (porcentaje >= 70) {
      return 'text-warn';
    }
    return 'text-bad';
  }
}
