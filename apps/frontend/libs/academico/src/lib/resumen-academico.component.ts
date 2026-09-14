import { Component, Input } from '@angular/core';

@Component({
  selector: 'siga-resumen-academico',
  template: `
    <section class="mt-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <h3 class="text-base font-semibold text-slate-900">Resumen academico</h3>
      @if (idEstudiante) {
        <p class="mt-2 text-sm text-slate-700">Estudiante seleccionado: {{ idEstudiante }}</p>
      }
      <p class="mt-2 text-sm text-slate-500">
        Notas, asignaturas y asistencias se cargaran desde el BFF.
      </p>
    </section>
  `,
})
export class ResumenAcademicoComponent {
  @Input() idEstudiante?: number;
}
