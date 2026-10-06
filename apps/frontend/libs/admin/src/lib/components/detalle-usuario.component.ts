import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { UsuarioDetalle, formatearRut } from '@siga/mocks';
import { AdminService } from '../state/admin.service';

/** Modal con el detalle del usuario y un resumen del perfil de su rol. */
@Component({
  selector: 'siga-detalle-usuario',
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      (click)="cerrar.emit()"
    >
      <div
        class="w-full max-w-lg rounded-2xl bg-panel p-6 shadow-2xl"
        (click)="$event.stopPropagation()"
      >
        <div class="flex items-start justify-between gap-4">
          <h3 class="text-lg font-semibold text-heading">Detalle del usuario</h3>
          <button
            type="button"
            (click)="cerrar.emit()"
            class="rounded-lg border border-line px-2 py-0.5 text-muted transition hover:text-ink"
          >
            ×
          </button>
        </div>

        @if (cargando()) {
          <p class="mt-4 text-sm text-muted">Cargando detalle…</p>
        } @else if (error()) {
          <p class="mt-4 rounded-xl border border-bad/60 bg-surface px-4 py-3 text-sm text-bad">
            {{ error() }}
          </p>
        } @else if (detalle(); as datos) {
          <dl class="mt-4 grid gap-3 text-sm sm:grid-cols-2">
            <div class="sm:col-span-2">
              <dt class="text-muted">Nombre</dt>
              <dd class="font-medium text-ink">{{ datos.fullName }}</dd>
            </div>
            <div class="sm:col-span-2">
              <dt class="text-muted">Correo</dt>
              <dd class="break-all text-ink">{{ datos.email }}</dd>
            </div>
            <div>
              <dt class="text-muted">Rol</dt>
              <dd class="text-ink">{{ datos.rol ?? '—' }}</dd>
            </div>
            <div>
              <dt class="text-muted">Estado</dt>
              <dd class="text-ink">{{ datos.estado ?? '—' }}</dd>
            </div>
            @if (datos.rut) {
              <div>
                <dt class="text-muted">RUT</dt>
                <dd class="text-ink">{{ formatear(datos.rut) }}</dd>
              </div>
            }
            @if (datos.fechaNacimiento) {
              <div>
                <dt class="text-muted">Nacimiento</dt>
                <dd class="text-ink">{{ datos.fechaNacimiento }}</dd>
              </div>
            }
            <div class="sm:col-span-2">
              <dt class="text-muted">Perfil</dt>
              <dd class="text-ink">{{ datos.detalle }}</dd>
            </div>
            @if (datos.etiquetas.length > 0) {
              <div class="sm:col-span-2">
                <dt class="text-muted">Adicional</dt>
                <dd class="flex flex-col gap-1 text-ink">
                  @for (etiqueta of datos.etiquetas; track etiqueta) {
                    <span>• {{ etiqueta }}</span>
                  }
                </dd>
              </div>
            }
          </dl>
        }

        <div class="mt-5 flex justify-end">
          <button
            type="button"
            (click)="cerrar.emit()"
            class="rounded-lg border border-line px-4 py-2 text-sm text-muted transition hover:text-ink"
          >
            Cerrar
          </button>
        </div>
      </div>
    </div>
  `,
})
export class DetalleUsuarioComponent implements OnInit {
  private readonly adminService = inject(AdminService);

  readonly usuarioId = input.required<string>();
  readonly cerrar = output<void>();

  protected readonly cargando = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly detalle = signal<UsuarioDetalle | null>(null);
  protected readonly formatear = formatearRut;

  ngOnInit(): void {
    this.adminService.getUsuarioDetalle(this.usuarioId()).subscribe({
      next: (detalle) => {
        this.cargando.set(false);
        this.detalle.set(detalle);
      },
      error: (error: HttpErrorResponse) => {
        this.cargando.set(false);
        const cuerpo = error.error as { message?: string } | null;
        this.error.set(
          cuerpo?.message ?? 'No se pudo cargar el detalle. Revisa la conexión con el BFF.',
        );
      },
    });
  }
}
