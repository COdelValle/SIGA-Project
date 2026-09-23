import { Component, computed, effect, input, signal } from '@angular/core';
import { AsistenciaRegistro, formatearFecha } from '../models/academico.model';
import { PaginadorComponent } from '@siga/shared-ui';

/** Historial de asistencia de una asignatura: Fecha / Tipo / Justificado. */
@Component({
  selector: 'siga-asistencia-historial',
  imports: [PaginadorComponent],
  template: `
    <div class="overflow-hidden rounded-xl border border-gold/60">
      <table class="w-full border-collapse text-left text-sm">
        <thead>
          <tr class="bg-bar text-gold">
            <th class="px-4 py-3 font-semibold">Fecha</th>
            <th class="px-4 py-3 font-semibold">Tipo</th>
            <th class="px-4 py-3 font-semibold">Justificado</th>
          </tr>
        </thead>
        <tbody>
          @for (registro of registrosPagina(); track $index) {
            <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
              <td class="px-4 py-3 align-middle">{{ formatear(registro.fecha) }}</td>
              <td [class]="'px-4 py-3 align-middle font-semibold ' + tipoClass(registro.tipo)">
                {{ registro.tipo }}
              </td>
              <td class="px-4 py-3 align-middle">{{ registro.justificado }}</td>
            </tr>
          } @empty {
            <tr class="bg-panel text-muted">
              <td class="px-4 py-6 text-center" colspan="3">Sin registros de asistencia.</td>
            </tr>
          }
        </tbody>
      </table>

      @if (registros().length > 0) {
        <div class="border-t border-line">
          <siga-paginador
            [total]="registros().length"
            [page]="pagina()"
            [pageSize]="pageSize"
            (pageChange)="cambiarPagina($event)"
          />
        </div>
      }
    </div>
  `,
})
export class AsistenciaHistorialComponent {
  readonly registros = input<AsistenciaRegistro[]>([]);

  protected readonly pageSize = 10;
  protected readonly pagina = signal(1);
  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.registros().length / this.pageSize)),
  );
  protected readonly registrosPagina = computed(() => {
    const pagina = Math.min(this.pagina(), this.totalPaginas());
    const inicio = (pagina - 1) * this.pageSize;
    return this.registros().slice(inicio, inicio + this.pageSize);
  });

  constructor() {
    // Al cambiar de asignatura (nuevos registros) se vuelve a la pagina 1.
    effect(() => {
      this.registros();
      this.pagina.set(1);
    });
  }

  protected cambiarPagina(pagina: number): void {
    this.pagina.set(pagina);
  }

  protected formatear(fecha: string): string {
    return formatearFecha(fecha);
  }

  protected tipoClass(tipo: AsistenciaRegistro['tipo']): string {
    return tipo === 'Presente' ? 'text-ok' : 'text-bad';
  }
}
