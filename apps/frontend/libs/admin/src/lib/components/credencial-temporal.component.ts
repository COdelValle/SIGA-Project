import { Component, input, output, signal } from '@angular/core';
import { CredencialTemporal } from '@siga/mocks';
import { copiarAlPortapapeles } from '../utils/clipboard';

/**
 * Modal de credencial temporal: se muestra una sola vez (la clave no vuelve a
 * estar disponible en el backend).
 */
@Component({
  selector: 'siga-credencial-temporal',
  template: `
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      (click)="cerrar.emit()"
    >
      <div
        class="w-full max-w-md rounded-2xl bg-panel p-6 shadow-2xl"
        (click)="$event.stopPropagation()"
      >
        <h3 class="text-lg font-semibold text-heading">Credencial temporal</h3>
        <p class="mt-1 text-sm text-muted">
          Se muestra una sola vez. Cópiala y entrégala al usuario: el backend no vuelve a
          exponerla.
        </p>

        <dl class="mt-4 flex flex-col gap-3 text-sm">
          <div>
            <dt class="text-muted">Correo</dt>
            <dd class="font-medium text-ink">{{ credencial().email }}</dd>
          </div>
          <div>
            <dt class="text-muted">Contraseña temporal</dt>
            <dd class="mt-1 flex items-center gap-2">
              <code
                class="flex-1 overflow-x-auto rounded-lg border border-line bg-surface px-3 py-2 font-mono text-base text-ink"
                >{{ credencial().temporaryPassword }}</code
              >
              <button
                type="button"
                (click)="copiar()"
                class="rounded-lg border border-brand px-3 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
              >
                {{ copiado() ? 'Copiada' : 'Copiar' }}
              </button>
            </dd>
          </div>
        </dl>

        @if (credencial().expiresAt) {
          <p class="mt-3 text-xs text-muted">Vence: {{ credencial().expiresAt }}</p>
        }
        <p class="mt-2 text-xs text-muted">
          El usuario deberá cambiar la contraseña en su primer inicio de sesión.
        </p>

        <div class="mt-5 flex justify-end">
          <button
            type="button"
            (click)="cerrar.emit()"
            class="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-page transition hover:brightness-110"
          >
            Entendido
          </button>
        </div>
      </div>
    </div>
  `,
})
export class CredencialTemporalComponent {
  readonly credencial = input.required<CredencialTemporal>();
  readonly cerrar = output<void>();

  protected readonly copiado = signal(false);

  protected copiar(): void {
    void copiarAlPortapapeles(this.credencial().temporaryPassword).then((ok) =>
      this.copiado.set(ok),
    );
  }
}
