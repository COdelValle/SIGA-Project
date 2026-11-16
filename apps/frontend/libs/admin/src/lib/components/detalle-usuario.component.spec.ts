import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UsuarioDetalle } from '@siga/mocks';
import { Observable, Subject, of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AdminService } from '../state/admin.service';
import { DetalleUsuarioComponent } from './detalle-usuario.component';

const detalle: UsuarioDetalle = {
  id: 'u-1',
  fullName: 'Camila Soto Hernández',
  email: 'camila.soto@platformsiga.onmicrosoft.com',
  rol: 'ESTUDIANTE',
  estado: 'ACTIVO',
  rut: '12345678-9',
  fechaNacimiento: '2012-04-01',
  detalle: '8vo Básico A',
  etiquetas: ['Sin alergias', 'Apoderada: Claudia Hernández'],
};

function configurar(
  getUsuarioDetalle: (id: string) => Observable<UsuarioDetalle> = () => of(detalle),
): {
  fixture: ComponentFixture<DetalleUsuarioComponent>;
  adminService: { getUsuarioDetalle: ReturnType<typeof vi.fn> };
} {
  const adminService = { getUsuarioDetalle: vi.fn(getUsuarioDetalle) };

  TestBed.configureTestingModule({
    imports: [DetalleUsuarioComponent],
    providers: [{ provide: AdminService, useValue: adminService }],
  });

  const fixture = TestBed.createComponent(DetalleUsuarioComponent);
  fixture.componentRef.setInput('usuarioId', 'u-1');
  fixture.detectChanges();
  return { fixture, adminService };
}

function texto(fixture: ComponentFixture<DetalleUsuarioComponent>): string {
  return (fixture.nativeElement as HTMLElement).textContent ?? '';
}

describe('DetalleUsuarioComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('muestra el detalle completo del usuario', () => {
    const { fixture, adminService } = configurar();

    expect(adminService.getUsuarioDetalle).toHaveBeenCalledWith('u-1');
    expect(texto(fixture)).toContain('Camila Soto Hernández');
    expect(texto(fixture)).toContain('camila.soto@platformsiga.onmicrosoft.com');
    expect(texto(fixture)).toContain('ESTUDIANTE');
    expect(texto(fixture)).toContain('ACTIVO');
    expect(texto(fixture)).toContain('12.345.678-9');
    expect(texto(fixture)).toContain('2012-04-01');
    expect(texto(fixture)).toContain('8vo Básico A');
    expect(texto(fixture)).toContain('Sin alergias');
    expect(texto(fixture)).toContain('Apoderada: Claudia Hernández');
    expect(texto(fixture)).not.toContain('Cargando detalle');

    fixture.destroy();
  });

  it('muestra el estado de carga hasta que llega la respuesta', () => {
    const respuesta = new Subject<UsuarioDetalle>();
    const { fixture } = configurar(() => respuesta.asObservable());

    expect(texto(fixture)).toContain('Cargando detalle…');

    respuesta.next(detalle);
    respuesta.complete();
    fixture.detectChanges();

    expect(texto(fixture)).not.toContain('Cargando detalle…');
    expect(texto(fixture)).toContain('Camila Soto Hernández');

    fixture.destroy();
  });

  it('omite secciones opcionales y usa guiones cuando faltan datos', () => {
    const { fixture } = configurar(() =>
      of({
        ...detalle,
        rol: null,
        estado: null,
        rut: null,
        fechaNacimiento: null,
        etiquetas: [],
      }),
    );

    expect(texto(fixture)).toContain('—');
    expect(texto(fixture)).not.toContain('RUT');
    expect(texto(fixture)).not.toContain('Nacimiento');
    expect(texto(fixture)).not.toContain('Adicional');

    fixture.destroy();
  });

  it('muestra el mensaje de error enviado por el BFF', () => {
    const { fixture } = configurar(() =>
      throwError(
        () =>
          new HttpErrorResponse({
            error: { message: 'Usuario no encontrado' },
            status: 404,
            statusText: 'Not Found',
          }),
      ),
    );

    expect(texto(fixture)).toContain('Usuario no encontrado');
    expect(texto(fixture)).not.toContain('Cargando detalle…');

    fixture.destroy();
  });

  it('usa un mensaje generico cuando el BFF no entrega detalle del error', () => {
    const { fixture } = configurar(() =>
      throwError(() => new HttpErrorResponse({ status: 500, statusText: 'Server Error' })),
    );

    expect(texto(fixture)).toContain(
      'No se pudo cargar el detalle. Revisa la conexión con el BFF.',
    );

    fixture.destroy();
  });

  it('emite cerrar desde el overlay, el botón y evita cerrar al hacer clic dentro', () => {
    const { fixture } = configurar();
    let cerrados = 0;
    fixture.componentInstance.cerrar.subscribe(() => (cerrados += 1));

    const overlay = (fixture.nativeElement as HTMLElement).querySelector('.fixed') as HTMLElement;
    const tarjeta = overlay.querySelector('.max-w-lg') as HTMLElement;

    tarjeta.click();
    fixture.detectChanges();
    expect(cerrados).toBe(0);

    overlay.click();
    fixture.detectChanges();
    expect(cerrados).toBe(1);

    const botonCerrar = Array.from(overlay.querySelectorAll('button')).find((boton) =>
      boton.textContent?.includes('Cerrar'),
    ) as HTMLButtonElement;
    botonCerrar.click();
    fixture.detectChanges();
    expect(cerrados).toBe(2);

    fixture.destroy();
  });
});
