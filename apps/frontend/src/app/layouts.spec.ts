import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { InteractionStatus } from '@azure/msal-browser';
import { AdminLayoutComponent } from '@siga/admin';
import { ApoderadoLayoutComponent } from '@siga/apoderado';
import { ApoderadoStateService } from '../../libs/apoderado/src/lib/state/apoderado-state.service';
import { Me, MeService, NotificationService } from '@siga/core';
import { DocenteLayoutComponent } from '@siga/docente';
import { EstudianteLayoutComponent } from '@siga/estudiante';
import { Pupilo } from '@siga/mocks';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';

function configurar<T>(componente: new () => T, me: Me, pupilos: Pupilo[] = []) {
  const state = {
    pupilos: signal<Pupilo[]>(pupilos),
    pupiloId: signal(pupilos[0]?.id ?? 0),
    seleccionar: vi.fn(),
  };

  TestBed.configureTestingModule({
    imports: [componente],
    providers: [
      provideRouter([]),
      {
        provide: MsalService,
        useValue: { instance: { getAllAccounts: () => [], setActiveAccount: vi.fn() } },
      },
      {
        provide: MsalBroadcastService,
        useValue: { inProgress$: of(InteractionStatus.None) },
      },
      { provide: MeService, useValue: { getMe: () => of(me) } },
      {
        provide: NotificationService,
        useValue: {
          getUnreadCount: () => of({ noLeidas: 0 }),
          getMine: () => of({ content: [], total: 0, page: 0, size: 10 }),
        },
      },
      { provide: ApoderadoStateService, useValue: state },
    ],
  });

  const fixture = TestBed.createComponent(componente);
  fixture.detectChanges();
  return { fixture, state };
}

async function esperar(fixture: ComponentFixture<unknown>): Promise<void> {
  for (let intento = 0; intento < 3; intento += 1) {
    TestBed.tick();
    await fixture.whenStable();
    await Promise.resolve();
  }
  fixture.detectChanges();
}

function texto(fixture: ComponentFixture<unknown>): string {
  return (fixture.nativeElement as HTMLElement).textContent ?? '';
}

const meAdmin: Me = {
  id: 'a',
  email: 'admin@siga.cl',
  displayName: 'Admin SIGA',
  roles: ['ADMIN'],
};
const meDocente: Me = {
  id: 'd',
  email: 'docente@siga.cl',
  displayName: 'Docente SIGA',
  roles: ['DOCENTE'],
};
const meEstudiante: Me = {
  id: 'e',
  email: 'estudiante@siga.cl',
  displayName: 'Estudiante SIGA',
  roles: ['ESTUDIANTE'],
};
const meApoderado: Me = {
  id: 'p',
  email: 'apoderado@siga.cl',
  displayName: 'Apoderado SIGA',
  roles: ['APODERADO'],
};

describe('layouts de los portales', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('admin muestra su menú y el encabezado del portal', async () => {
    const { fixture } = configurar(AdminLayoutComponent, meAdmin);
    await esperar(fixture);

    expect(texto(fixture)).toContain('Admin SIGA');
    expect(texto(fixture)).toContain('Usuarios');
    expect(texto(fixture)).toContain('Roles');
    expect(texto(fixture)).toContain('Malla curricular');
    expect((fixture.nativeElement as HTMLElement).querySelector('router-outlet')).toBeTruthy();
    fixture.destroy();
  });

  it('docente muestra su menú y el encabezado del portal', async () => {
    const { fixture } = configurar(DocenteLayoutComponent, meDocente);
    await esperar(fixture);

    expect(texto(fixture)).toContain('Docente SIGA');
    expect(texto(fixture)).toContain('Mis cursos');
    expect(texto(fixture)).toContain('Registrar notas');
    expect(texto(fixture)).toContain('Registrar asistencias');
    fixture.destroy();
  });

  it('estudiante muestra su menú y el encabezado del portal', async () => {
    const { fixture } = configurar(EstudianteLayoutComponent, meEstudiante);
    await esperar(fixture);

    expect(texto(fixture)).toContain('Estudiante SIGA');
    expect(texto(fixture)).toContain('Horarios de clases');
    expect(texto(fixture)).toContain('Progreso Acad');
    fixture.destroy();
  });

  it('apoderado muestra su menú, la barra de pupilos y el encabezado', async () => {
    const pupilos: Pupilo[] = [
      { id: 1, nombre: 'Camila Soto', relacion: 'Hija', curso: '8vo Básico A' },
    ];
    const { fixture, state } = configurar(ApoderadoLayoutComponent, meApoderado, pupilos);
    await esperar(fixture);

    expect(texto(fixture)).toContain('Apoderado SIGA');
    expect(texto(fixture)).toContain('Mis pupilos');
    expect(texto(fixture)).toContain('Solicitudes');
    expect(texto(fixture)).toContain('Camila Soto');
    expect(state.pupilos()).toHaveLength(1);
    fixture.destroy();
  });
});
