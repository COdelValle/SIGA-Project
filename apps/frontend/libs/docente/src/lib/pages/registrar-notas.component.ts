import { Component, computed, inject, signal } from '@angular/core';
import { SeccionCardComponent, SelectComponent, SelectOption } from '@siga/shared-ui';
import { Alumno, nombreCompleto } from '../mocks/docente.mock';
import { DocenteAcademicoService } from '../state/docente-academico.service';

@Component({
  selector: 'siga-docente-registrar-notas',
  imports: [SeccionCardComponent, SelectComponent],
  template: `
    <div class="mx-auto flex max-w-5xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Registrar notas</h1>

      <div class="w-full max-w-md">
        <siga-select
          [options]="opcionesCurso"
          [value]="cursoId()"
          ariaLabel="Seleccionar curso"
          (valueChange)="cambiarCurso($event)"
        />
      </div>

      <siga-seccion-card [title]="(curso()?.nombre ?? '') + ' · ' + (curso()?.asignatura ?? '')">
        <div class="flex flex-col gap-3">
          @for (alumno of alumnos(); track alumno.id) {
            <div class="rounded-xl bg-surface p-3">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <span class="text-sm font-medium text-ink">{{ nombre(alumno) }}</span>
                <span class="text-xs text-muted">Siguiente: Nota {{ siguiente(alumno.id) }}</span>
              </div>

              <div class="mt-2 flex flex-wrap items-center gap-2">
                @for (nota of notas(alumno.id); track nota.numero) {
                  <span
                    class="flex items-center gap-1 rounded-lg border border-gold/60 bg-panel px-2 py-1"
                  >
                    <span class="text-xs font-semibold text-gold">N{{ nota.numero }}</span>
                    <input
                      type="number"
                      min="1"
                      max="7"
                      step="0.1"
                      [value]="nota.valor"
                      (change)="editar(alumno.id, nota.numero, $event)"
                      class="w-16 bg-transparent text-sm text-ink focus:outline-none"
                    />
                    <button
                      type="button"
                      (click)="eliminar(alumno.id, nota.numero)"
                      class="text-bad transition hover:text-bad/70"
                      title="Eliminar nota"
                    >
                      ×
                    </button>
                  </span>
                }

                <span class="flex items-center gap-1">
                  <input
                    #nuevo
                    type="number"
                    min="1"
                    max="7"
                    step="0.1"
                    placeholder="Nota {{ siguiente(alumno.id) }}"
                    class="w-24 rounded-lg border border-line bg-panel px-2 py-1 text-sm text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
                  />
                  <button
                    type="button"
                    (click)="agregar(alumno.id, nuevo)"
                    class="rounded-lg border border-gold/60 px-3 py-1 text-xs font-semibold text-gold transition hover:bg-gold/10"
                  >
                    Agregar
                  </button>
                </span>
              </div>
            </div>
          }
        </div>

        <div class="mt-4 flex flex-wrap items-center justify-end gap-3">
          @if (pendiente()) {
            <p class="text-sm text-warn">Borrador sin guardar</p>
          } @else {
            <p class="text-sm text-ok">Guardado</p>
          }
          <button
            type="button"
            (click)="guardar()"
            class="rounded-lg border border-brand px-4 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
          >
            Guardar curso
          </button>
        </div>
      </siga-seccion-card>
    </div>
  `,
})
export class DocenteRegistrarNotasComponent {
  private readonly academico = inject(DocenteAcademicoService);

  protected readonly cursos = this.academico.cursos;
  protected readonly opcionesCurso: SelectOption[] = this.cursos.map((curso) => ({
    value: curso.id,
    label: `${curso.nombre} · ${curso.asignatura}`,
  }));
  protected readonly cursoId = signal(this.cursos[0]?.id ?? 0);
  protected readonly curso = computed(() =>
    this.cursos.find((item) => item.id === this.cursoId()),
  );
  protected readonly alumnos = computed(() => this.curso()?.alumnos ?? []);
  protected readonly pendiente = computed(() =>
    this.academico.pendiente(`notas|${this.cursoId()}`),
  );

  protected nombre(alumno: Alumno): string {
    return nombreCompleto(alumno);
  }

  protected notas(alumnoId: number) {
    return this.academico.notasDe(this.cursoId(), alumnoId);
  }

  protected siguiente(alumnoId: number): number {
    return this.academico.siguienteNumero(this.cursoId(), alumnoId);
  }

  protected agregar(alumnoId: number, input: HTMLInputElement): void {
    const valor = Math.round(Number(input.value) * 10) / 10;
    if (!valor || valor < 1 || valor > 7) {
      return;
    }
    this.academico.crearNota(this.cursoId(), alumnoId, valor);
    input.value = '';
  }

  protected editar(alumnoId: number, numero: number, event: Event): void {
    const valor = Math.round(Number((event.target as HTMLInputElement).value) * 10) / 10;
    if (valor >= 1 && valor <= 7) {
      this.academico.editarNota(this.cursoId(), alumnoId, numero, valor);
    }
  }

  protected eliminar(alumnoId: number, numero: number): void {
    this.academico.eliminarNota(this.cursoId(), alumnoId, numero);
  }

  protected guardar(): void {
    this.academico.guardarNotasCurso(this.cursoId());
  }

  protected cambiarCurso(value: string | number): void {
    this.cursoId.set(Number(value));
  }
}
