import { Component, inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthService } from '@siga/core';

@Component({
  selector: 'siga-public-portal',
  template: `
    <div
      class="flex min-h-dvh flex-col gap-12 bg-gradient-to-b from-blue-50 via-white to-white px-6 py-12"
    >
      <header class="mx-auto max-w-3xl text-center">
        <span
          class="inline-block rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold tracking-wide text-blue-700"
        >
          Gestion academica
        </span>
        <h1 class="mt-4 text-4xl font-bold tracking-tight text-slate-900 sm:text-5xl">
          Portal Academico SIGA
        </h1>
        <p class="mx-auto mt-4 max-w-xl text-lg text-slate-600">
          Accede a la informacion academica de tu institucion de forma segura.
        </p>
        <button
          type="button"
          class="mt-8 rounded-full bg-blue-600 px-8 py-3 text-base font-semibold text-white shadow-lg shadow-blue-600/20 transition hover:bg-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-400"
          (click)="ingresar()"
        >
          Ingresar al portal
        </button>
      </header>

      <section class="mx-auto grid w-full max-w-5xl gap-6 sm:grid-cols-2 lg:grid-cols-3">
        @for (item of accesos; track item.title) {
          <article
            class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-md"
          >
            <h3 class="text-lg font-semibold text-slate-900">{{ item.title }}</h3>
            <p class="mt-2 text-sm text-slate-600">{{ item.description }}</p>
          </article>
        }
      </section>

      <footer class="mt-auto text-center text-sm text-slate-400">SIGA - Gestion academica</footer>
    </div>
  `,
})
export class PublicPortalComponent {
  private readonly auth = inject(AuthService);

  protected readonly accesos = [
    { title: 'Estudiantes', description: 'Consulta tus notas, asignaturas y asistencias.' },
    {
      title: 'Apoderados',
      description: 'Revisa la informacion de tus pupilos y gestiona solicitudes.',
    },
    {
      title: 'Docentes',
      description: 'Crea eventos, agenda evaluaciones y registra notas y asistencia.',
    },
  ];

  ingresar(): void {
    this.auth.login();
  }
}

export const PUBLIC_ROUTES: Routes = [{ path: '', component: PublicPortalComponent }];
