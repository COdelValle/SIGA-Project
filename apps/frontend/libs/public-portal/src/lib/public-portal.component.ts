import { Component, inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthService } from '@siga/core';

@Component({
  selector: 'siga-public-portal',
  template: `
    <div class="landing">
      <header class="hero">
        <h1>Portal Academico SIGA</h1>
        <p>Accede a la informacion academica de tu institucion.</p>
        <button type="button" class="cta" (click)="ingresar()">Ingresar al portal</button>
      </header>

      <section class="cards">
        <article class="card">
          <h3>Estudiantes</h3>
          <p>Consulta tus notas, asignaturas y asistencias.</p>
        </article>
        <article class="card">
          <h3>Apoderados</h3>
          <p>Revisa la informacion de tus pupilos y gestiona solicitudes.</p>
        </article>
        <article class="card">
          <h3>Docentes</h3>
          <p>Crea eventos, agenda evaluaciones y registra notas y asistencia.</p>
        </article>
      </section>

      <footer class="footer">
        <span>SIGA - Gestion academica</span>
      </footer>
    </div>
  `,
  styles: [
    `
      .landing {
        font-family: 'Inter', system-ui, -apple-system, 'Segoe UI', sans-serif;
        min-height: 100dvh;
        display: flex;
        flex-direction: column;
        gap: 2rem;
        padding: 3rem 1.5rem;
        box-sizing: border-box;
        background: linear-gradient(180deg, #eef3fb 0%, #ffffff 60%);
      }
      .hero {
        text-align: center;
      }
      .hero h1 {
        font-size: 2.5rem;
        color: #1f3a5f;
        margin: 0 0 0.5rem;
      }
      .hero p {
        color: #52607a;
        margin: 0 0 1.5rem;
      }
      .cta {
        border: 0;
        border-radius: 999px;
        padding: 0.85rem 2rem;
        background: #1f6feb;
        color: #ffffff;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
      }
      .cta:hover {
        background: #1a5fd0;
      }
      .cards {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
        gap: 1.25rem;
        max-width: 900px;
        margin: 0 auto;
        width: 100%;
      }
      .card {
        border: 1px solid #d7dde5;
        border-radius: 12px;
        padding: 1.25rem;
        background: #ffffff;
        box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
      }
      .card h3 {
        margin: 0 0 0.5rem;
        color: #1f3a5f;
      }
      .card p {
        margin: 0;
        color: #52607a;
      }
      .footer {
        margin-top: auto;
        text-align: center;
        color: #8894a8;
        font-size: 0.85rem;
      }
    `,
  ],
})
export class PublicPortalComponent {
  private readonly auth = inject(AuthService);

  ingresar(): void {
    this.auth.login();
  }
}

export const PUBLIC_ROUTES: Routes = [{ path: '', component: PublicPortalComponent }];
