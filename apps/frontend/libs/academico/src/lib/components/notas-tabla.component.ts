import { Component, Input } from '@angular/core';
import { AsignaturaNotas, NotaDetalle } from '../models/academico.model';

/**
 * Tabla de notas con columnas dinamicas (maximo de notas del semestre).
 * Cada nota se muestra como un cuadro coloreado por rango y el promedio de la
 * asignatura en negrita. Las celdas sin nota conservan el color de la franja.
 */
@Component({
  selector: 'siga-notas-tabla',
  template: `
    <div class="overflow-x-auto rounded-xl border border-gold/60">
      <table class="w-full border-collapse text-left text-sm">
        <thead>
          <tr class="bg-bar text-gold">
            <th class="px-4 py-3 font-semibold">Asignatura</th>
            @for (columna of columnas; track columna) {
              <th class="px-3 py-3 text-center font-semibold">Nota {{ columna }}</th>
            }
            <th class="px-4 py-3 text-center font-semibold">Promedio</th>
          </tr>
        </thead>
        <tbody>
          @for (asignatura of asignaturas; track asignatura.id) {
            <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
              <td class="px-4 py-3 align-middle">{{ asignatura.asignatura }}</td>
              @for (columna of columnas; track columna) {
                <td class="px-3 py-3 text-center align-middle">
                  @if (notaDe(asignatura, columna); as nota) {
                    <span
                      [class]="
                        'inline-flex min-w-10 justify-center rounded-md px-2 py-1 font-semibold ' +
                        notaClass(nota.valor)
                      "
                    >
                      {{ nota.valor.toFixed(1) }}
                    </span>
                  } @else {
                    <span class="inline-flex min-w-10 justify-center px-2 py-1 text-ink/30">—</span>
                  }
                </td>
              }
              <td
                [class]="
                  'px-4 py-3 text-center align-middle font-bold ' + notaClass(asignatura.promedio)
                "
              >
                {{ asignatura.promedio.toFixed(1) }}
              </td>
            </tr>
          } @empty {
            <tr class="bg-panel text-muted">
              <td class="px-4 py-6 text-center" [attr.colspan]="columnas.length + 2">
                Sin notas registradas.
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
})
export class NotasTablaComponent {
  @Input() asignaturas: AsignaturaNotas[] = [];

  get columnas(): number[] {
    const maximo = this.asignaturas.reduce(
      (mayor, asignatura) => Math.max(mayor, asignatura.notas.length),
      0,
    );
    return Array.from({ length: maximo }, (_, index) => index + 1);
  }

  protected notaDe(asignatura: AsignaturaNotas, numero: number): NotaDetalle | undefined {
    return asignatura.notas.find((nota) => nota.numero === numero);
  }

  protected notaClass(valor: number): string {
    if (valor >= 6) {
      return 'bg-ok/20 text-ok';
    }
    if (valor >= 4) {
      return 'bg-warn/20 text-warn';
    }
    return 'bg-bad/20 text-bad';
  }
}
