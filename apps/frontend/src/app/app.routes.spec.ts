import { describe, expect, it } from 'vitest';
import { routes } from './app.routes';

describe('app.routes', () => {
  it('declara los portales y las rutas públicas', () => {
    const caminos = routes.map((ruta) => ruta.path);

    expect(caminos).toContain('');
    expect(caminos).toContain('auth');
    expect(caminos).toContain('sin-acceso');
    expect(caminos).toContain('error-acceso');
    expect(caminos).toContain('estudiante');
    expect(caminos).toContain('apoderado');
    expect(caminos).toContain('docente');
    expect(caminos).toContain('admin');
    expect(caminos).toContain('**');

    for (const portal of ['estudiante', 'apoderado', 'docente', 'admin']) {
      const ruta = routes.find((item) => item.path === portal);
      expect(ruta?.canActivate?.length).toBe(2);
    }
  });

  it('resuelve todos los cargadores diferidos', async () => {
    const cargables = routes.filter((ruta) => ruta.loadChildren || ruta.loadComponent);

    for (const ruta of cargables) {
      const cargado = ruta.loadChildren
        ? await (ruta.loadChildren as () => Promise<unknown>)()
        : await (ruta.loadComponent as () => Promise<unknown>)();
      expect(cargado).toBeTruthy();
    }
  });
});
