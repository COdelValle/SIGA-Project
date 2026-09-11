import { Component, Input } from '@angular/core';

@Component({
  selector: 'siga-resumen-academico',
  template: `
    <section class="siga-card">
      <h3>Resumen academico</h3>
      @if (idEstudiante) {
        <p>Estudiante seleccionado: {{ idEstudiante }}</p>
      }
      <p>Notas, asignaturas y asistencias se cargaran desde el BFF.</p>
    </section>
  `,
  styles: [
    `
      .siga-card {
        border: 1px solid #d7dde5;
        border-radius: 10px;
        padding: 1rem 1.25rem;
        margin-top: 1rem;
        background: #fbfcfe;
      }
    `,
  ],
})
export class ResumenAcademicoComponent {
  @Input() idEstudiante?: number;
}
