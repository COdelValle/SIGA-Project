import { Component } from '@angular/core';

@Component({
  selector: 'siga-apoderado-solicitudes',
  template: `
    <div class="mx-auto flex max-w-4xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Solicitudes</h1>

      <ul class="flex flex-col gap-3">
        @for (solicitud of solicitudes; track solicitud.id) {
          <li class="rounded-2xl bg-panel p-4 shadow-lg">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <h3 class="font-semibold text-ink">{{ solicitud.titulo }}</h3>
              <span
                class="rounded-full border border-brand/50 px-3 py-1 text-xs font-semibold text-brand"
              >
                {{ solicitud.estado }}
              </span>
            </div>
            <p class="mt-1 text-sm text-muted">{{ solicitud.descripcion }}</p>
          </li>
        }
      </ul>
    </div>
  `,
})
export class ApoderadoSolicitudesComponent {
  protected readonly solicitudes = [
    {
      id: 1,
      titulo: 'Cambio de teléfono de emergencia',
      descripcion: 'Actualizar el contacto de emergencia registrado.',
      estado: 'Pendiente',
    },
    {
      id: 2,
      titulo: 'Justificación de inasistencia',
      descripcion: 'Certificado médico del 12 de septiembre.',
      estado: 'Aprobada',
    },
  ];
}
