import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthErrorService, AuthService } from '@siga/core';

@Component({
  selector: 'siga-auth-error',
  imports: [RouterLink],
  template: `
    <div
      class="flex min-h-dvh flex-col items-center justify-center gap-6 bg-gradient-to-b from-amber-50 via-white to-white px-6 text-center"
    >
      <span
        class="inline-flex h-16 w-16 items-center justify-center rounded-full bg-amber-100 text-3xl font-bold text-amber-600"
      >
        !
      </span>
      <h1 class="text-3xl font-bold tracking-tight text-slate-900">
        No pudimos iniciar tu sesion
      </h1>
      <p class="max-w-lg text-base text-slate-600">
        Ocurrio un problema al validar tus credenciales con Microsoft. Intenta nuevamente o
        contacta al administrador.
      </p>
      @if (error(); as err) {
        <div class="max-w-xl rounded-xl border border-amber-200 bg-amber-50 p-4 text-left">
          @if (err.code) {
            <p class="text-xs font-semibold uppercase tracking-wide text-amber-700">
              {{ err.code }}
            </p>
          }
          <p class="mt-1 text-sm text-amber-900">{{ err.message }}</p>
          @if (err.detail) {
            <p class="mt-1 text-xs text-amber-700">{{ err.detail }}</p>
          }
        </div>
      }
      <div class="flex flex-wrap items-center justify-center gap-3">
        <button
          type="button"
          (click)="reintentar()"
          class="rounded-full bg-blue-600 px-6 py-3 text-sm font-semibold text-white transition hover:bg-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-400"
        >
          Reintentar
        </button>
        <a
          routerLink="/"
          class="rounded-full border border-slate-300 px-6 py-3 text-sm font-semibold text-slate-700 transition hover:bg-slate-100"
        >
          Volver al inicio
        </a>
      </div>
    </div>
  `,
})
export class AuthErrorComponent {
  private readonly authError = inject(AuthErrorService);
  private readonly auth = inject(AuthService);

  protected readonly error = this.authError.error;

  protected reintentar(): void {
    this.authError.clear();
    this.auth.login();
  }
}
