import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '@siga/core';

@Component({
  selector: 'siga-sin-acceso',
  imports: [RouterLink],
  template: `
    <div
      class="flex min-h-dvh flex-col items-center justify-center gap-6 bg-gradient-to-b from-red-50 via-white to-white px-6 text-center"
    >
      <span
        class="inline-flex h-16 w-16 items-center justify-center rounded-full bg-red-100 text-3xl font-bold text-red-600"
      >
        !
      </span>
      <h1 class="text-3xl font-bold tracking-tight text-slate-900">Cuenta no habilitada</h1>
      <p class="max-w-md text-base text-slate-600">
        Tu cuenta de Microsoft se autenticó correctamente, pero no está registrada ni habilitada en
        SIGA. Solicita al equipo de TI o al administrador que habilite tu acceso.
      </p>
      <a
        routerLink="/"
        class="rounded-full bg-slate-900 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-700"
      >
        Volver al inicio
      </a>
    </div>
  `,
})
export class SinAccesoComponent implements OnInit {
  private readonly auth = inject(AuthService);

  ngOnInit(): void {
    // Si quedó una sesión de Microsoft activa, se cierra para que el usuario
    // no permanezca autenticado sin acceso a SIGA.
    if (this.auth.isAuthenticated()) {
      this.auth.logout('/sin-acceso');
    }
  }
}
