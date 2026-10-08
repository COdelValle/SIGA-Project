import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MeService, Notificacion, NotificationService } from '@siga/core';
import { of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { NotificationBellComponent } from './notification-bell.component';

const noLeida: Notificacion = {
  id: 9,
  tipo: 'NOTA',
  accion: 'CREADA',
  titulo: 'Calificación agregada en Matemática',
  resumen: 'Se agregó una calificación en «Prueba 1» de Matemática.',
  fechaHora: '2026-10-08T01:00:00',
  leida: false,
};

const leida: Notificacion = {
  ...noLeida,
  id: 10,
  leida: true,
  titulo: 'Asistencia registrada',
};

function configurar(rol: 'ESTUDIANTE' | 'APODERADO' | 'DOCENTE', contenido: Notificacion[] = [noLeida]) {
  const notificationService = {
    getMine: vi.fn(() => of({
      content: contenido,
      totalElements: contenido.length,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      empty: contenido.length === 0,
    })),
    getUnreadCount: vi.fn(() => of({ noLeidas: contenido.filter((n) => !n.leida).length })),
    markAsRead: vi.fn(() => of(undefined)),
    markAllAsRead: vi.fn(() => of(undefined)),
    clearRead: vi.fn(() => of(undefined)),
  };
  const router = { navigateByUrl: vi.fn() };

  TestBed.configureTestingModule({
    imports: [NotificationBellComponent],
    providers: [
      { provide: MeService, useValue: { getMe: () => of({ id: 'oid-test', roles: [rol] }) } },
      { provide: NotificationService, useValue: notificationService },
      { provide: Router, useValue: router },
    ],
  });

  const fixture = TestBed.createComponent(NotificationBellComponent);
  fixture.detectChanges();
  return { fixture, notificationService, router };
}

async function abrirCampana(fixture: Awaited<ReturnType<typeof configurar>>['fixture']) {
  await fixture.whenStable();
  await new Promise((resolve) => setTimeout(resolve, 10));
  fixture.detectChanges();
  const campana = fixture.nativeElement.querySelector('button[aria-label="Notificaciones"]') as HTMLButtonElement;
  campana.click();
  await fixture.whenStable();
  fixture.detectChanges();
  return campana;
}

function botonConTexto(
  fixture: Awaited<ReturnType<typeof configurar>>['fixture'],
  texto: string,
): HTMLButtonElement {
  return Array.from(fixture.nativeElement.querySelectorAll('button')).find((boton) =>
    (boton as HTMLButtonElement).textContent?.includes(texto),
  ) as HTMLButtonElement;
}

describe('NotificationBellComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('muestra la campana, el contador y la fecha en formato chileno', async () => {
    const { fixture, notificationService } = configurar('ESTUDIANTE');
    const campana = await abrirCampana(fixture);

    expect(campana.textContent).toContain('1');
    expect(notificationService.getMine).toHaveBeenCalledWith(0, 10);
    expect(fixture.nativeElement.textContent).toContain('Calificación agregada en Matemática');
    expect(fixture.nativeElement.textContent).toContain('08/10/2026 01:00');
    fixture.destroy();
  });

  it('refresca el contador al abrir la campana', async () => {
    const { fixture, notificationService } = configurar('ESTUDIANTE');
    await abrirCampana(fixture);

    // Una consulta inicial del polling + una al abrir el panel.
    expect(notificationService.getUnreadCount).toHaveBeenCalledTimes(2);
    fixture.destroy();
  });

  it('al hacer clic marca leída de inmediato y navega al recurso', async () => {
    const { fixture, notificationService, router } = configurar('ESTUDIANTE');
    await abrirCampana(fixture);

    const item = fixture.nativeElement.querySelector('li button') as HTMLButtonElement;
    item.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(notificationService.markAsRead).toHaveBeenCalledWith(9);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/estudiante/notas');
    fixture.destroy();
  });

  it('marcar todas conserva los ítems y limpiar solo quita las leídas', async () => {
    const { fixture, notificationService } = configurar('ESTUDIANTE', [noLeida, leida]);
    await abrirCampana(fixture);

    botonConTexto(fixture, 'Marcar todas').click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(notificationService.markAllAsRead).toHaveBeenCalled();
    // Siguen visibles: no se comporta como "limpiar".
    expect(fixture.nativeElement.querySelectorAll('li').length).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('Calificación agregada en Matemática');

    botonConTexto(fixture, 'Limpiar leídas').click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(notificationService.clearRead).toHaveBeenCalled();
    // Tras "marcar todas" no quedan pendientes: limpiar deja el estado vacío.
    expect(fixture.nativeElement.querySelectorAll('li').length).toBe(0);
    expect(fixture.nativeElement.textContent).toContain('No tienes notificaciones.');
    fixture.destroy();
  });

  it('si una acción falla mantiene la lista y muestra error inline', async () => {
    const { fixture, notificationService } = configurar('ESTUDIANTE', [noLeida]);
    notificationService.markAllAsRead.mockReturnValue(throwError(() => new Error('boom')));
    await abrirCampana(fixture);

    botonConTexto(fixture, 'Marcar todas').click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('No se pudo completar la acción');
    expect(fixture.nativeElement.textContent).toContain('Calificación agregada en Matemática');
    fixture.destroy();
  });

  it('limpiar leídas sin pendientes deja el estado vacío sin mensaje de error', async () => {
    const { fixture, notificationService } = configurar('ESTUDIANTE', [leida]);
    await abrirCampana(fixture);

    botonConTexto(fixture, 'Limpiar leídas').click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(notificationService.clearRead).toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('No tienes notificaciones.');
    expect(fixture.nativeElement.textContent).not.toContain('No se pudieron cargar');
    fixture.destroy();
  });

  it('no muestra campana a roles que no reciben estas notificaciones en esta etapa', async () => {
    const { fixture, notificationService } = configurar('DOCENTE');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('button[aria-label="Notificaciones"]')).toBeNull();
    expect(notificationService.getUnreadCount).not.toHaveBeenCalled();
    fixture.destroy();
  });
});
