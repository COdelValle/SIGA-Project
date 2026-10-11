import { Component, ElementRef, HostListener, Input, signal, viewChild } from '@angular/core';
import { AsignaturaNotas, NotaDetalle, formatearNota } from '../models/academico.model';

interface DetalleNotaAbierto {
  nota: NotaDetalle;
  x: number;
  y: number;
  listo: boolean;
}

/**
 * Tabla de notas con columnas dinamicas (maximo de notas del semestre).
 * Cada nota se muestra como un cuadro coloreado por rango y el promedio de la
 * asignatura en negrita. Al pasar el mouse, enfocar con teclado o tocar una
 * celda con nota se muestra un detalle compacto con el nombre de la
 * evaluacion, la nota y su ponderacion.
 */
@Component({
  selector: 'siga-notas-tabla',
  template: `
    <div class="overflow-x-auto rounded-xl border border-gold/60" (scroll)="ocultarDetalle()">
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
                        'inline-flex min-w-10 cursor-help justify-center rounded-md px-2 py-1 font-semibold focus:outline-none focus-visible:ring-2 focus-visible:ring-gold ' +
                        notaClass(nota.valor)
                      "
                      tabindex="0"
                      [attr.aria-label]="descripcionNota(nota)"
                      (pointerenter)="alEntrar(nota, $event)"
                      (pointerleave)="alSalir()"
                      (focus)="alEnfocar(nota, $event)"
                      (blur)="alSalir()"
                      (click)="alClic(nota, $event)"
                    >
                      {{ formatear(nota.valor) }}
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
                {{ formatear(asignatura.promedio) }}
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

    @if (detalle(); as d) {
      <div
        #tooltip
        role="tooltip"
        class="pointer-events-none fixed z-50 w-max max-w-64 rounded-lg border border-line bg-panel px-3 py-2 text-xs text-ink shadow-xl transition-opacity"
        [class.opacity-0]="!d.listo"
        [style.left.px]="d.x"
        [style.top.px]="d.y"
      >
        <p class="font-semibold break-words">{{ d.nota.nombre }}</p>
        <p class="mt-1 text-muted">
          Nota: <span class="font-semibold text-ink">{{ formatear(d.nota.valor) }}</span>
        </p>
        <p class="text-muted">
          Ponderación:
          <span class="font-semibold text-ink">{{ descripcionPonderacion(d.nota.ponderacion) }}</span>
        </p>
      </div>
    }
  `,
})
export class NotasTablaComponent {
  @Input() asignaturas: AsignaturaNotas[] = [];

  protected readonly detalle = signal<DetalleNotaAbierto | null>(null);
  private readonly tooltip = viewChild<ElementRef<HTMLElement>>('tooltip');
  private celdaActiva: HTMLElement | null = null;

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

  protected formatear(valor: number): string {
    return formatearNota(valor);
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

  protected descripcionPonderacion(ponderacion: number): string {
    return ponderacion > 0 ? `${ponderacion}%` : 'No pondera';
  }

  protected descripcionNota(nota: NotaDetalle): string {
    return `${nota.nombre}: nota ${this.formatear(nota.valor)}, ${this.descripcionPonderacion(
      nota.ponderacion,
    )}`;
  }

  protected alEntrar(nota: NotaDetalle, event: Event): void {
    if ((event as PointerEvent).pointerType === 'touch') {
      return;
    }
    this.mostrar(nota, event.target as HTMLElement);
  }

  protected alEnfocar(nota: NotaDetalle, event: FocusEvent): void {
    if (!this.permiteHover()) {
      // En pantallas tactiles el detalle se alterna con el toque.
      return;
    }
    this.mostrar(nota, event.target as HTMLElement);
  }

  protected alSalir(): void {
    this.ocultarDetalle();
  }

  protected alClic(nota: NotaDetalle, event: MouseEvent): void {
    const tipo = (event as PointerEvent).pointerType;
    if (tipo === 'mouse' || tipo === 'pen') {
      // Con punteros hover el detalle ya aparece al entrar.
      return;
    }
    event.stopPropagation();
    if (this.detalle()?.nota === nota) {
      this.ocultarDetalle();
      return;
    }
    this.mostrar(nota, event.target as HTMLElement);
  }

  protected ocultarDetalle(): void {
    this.detalle.set(null);
    this.celdaActiva = null;
  }

  @HostListener('document:click')
  protected alClicDocumento(): void {
    if (!this.permiteHover()) {
      this.ocultarDetalle();
    }
  }

  @HostListener('document:keydown.escape')
  protected alEscape(): void {
    this.ocultarDetalle();
  }

  private mostrar(nota: NotaDetalle, celda: HTMLElement): void {
    if (this.detalle()?.nota === nota) {
      return;
    }
    this.celdaActiva = celda;
    const rect = celda.getBoundingClientRect();
    this.detalle.set({ nota, x: rect.left, y: rect.top, listo: false });
    requestAnimationFrame(() => this.posicionar());
  }

  /** Centra el detalle sobre la celda, lo sube si cabe y lo mantiene en pantalla. */
  private posicionar(): void {
    const tooltip = this.tooltip()?.nativeElement;
    const celda = this.celdaActiva;
    const actual = this.detalle();
    if (!tooltip || !celda || !actual) {
      return;
    }
    const celdaRect = celda.getBoundingClientRect();
    const caja = tooltip.getBoundingClientRect();
    const margen = 8;
    const centrado = celdaRect.left + celdaRect.width / 2 - caja.width / 2;
    const x = Math.min(Math.max(centrado, margen), window.innerWidth - caja.width - margen);
    const arriba = celdaRect.top - caja.height - margen;
    const y = arriba >= margen ? arriba : celdaRect.bottom + margen;
    this.detalle.set({ ...actual, x, y, listo: true });
  }

  private permiteHover(): boolean {
    return window.matchMedia?.('(hover: hover)')?.matches ?? true;
  }
}
