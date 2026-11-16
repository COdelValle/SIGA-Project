import { afterEach, describe, expect, it, vi } from 'vitest';
import { limpiarParametrosMsal } from './msal-url.util';

function fijarUrl(url: string): void {
  window.history.replaceState({}, '', url);
}

describe('limpiarParametrosMsal', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    fijarUrl('/');
  });

  it('no toca la URL cuando no hay parametros de MSAL', () => {
    fijarUrl('/portal?page=2#seccion');
    const reemplazar = vi.spyOn(window.history, 'replaceState');

    limpiarParametrosMsal();

    expect(reemplazar).not.toHaveBeenCalled();
  });

  it('quita los parametros de MSAL del query y deja los demás intactos', () => {
    fijarUrl('/auth?state=abc&code=123&error=login&error_description=fallo&page=2');
    const reemplazar = vi.spyOn(window.history, 'replaceState');

    limpiarParametrosMsal();

    expect(reemplazar).toHaveBeenCalledWith({}, document.title, '/auth?page=2');
  });

  it('quita los parametros de MSAL del hash y conserva el resto', () => {
    fijarUrl('/auth#state=abc&session_state=xyz&client_info=info&seccion=notas');
    const reemplazar = vi.spyOn(window.history, 'replaceState');

    limpiarParametrosMsal();

    expect(reemplazar).toHaveBeenCalledWith({}, document.title, '/auth#seccion=notas');
  });

  it('elimina el hash completo cuando solo tenía parametros de MSAL', () => {
    fijarUrl('/auth#state=abc&code=123');
    const reemplazar = vi.spyOn(window.history, 'replaceState');

    limpiarParametrosMsal();

    expect(reemplazar).toHaveBeenCalledWith({}, document.title, '/auth');
  });

  it('limpia query y hash en la misma llamada', () => {
    fijarUrl('/auth?state=abc&ear_jwe=token#code=123&seccion=inicio');
    const reemplazar = vi.spyOn(window.history, 'replaceState');

    limpiarParametrosMsal();

    expect(reemplazar).toHaveBeenCalledWith({}, document.title, '/auth#seccion=inicio');
  });

  it('no reemplaza la URL si el hash no tiene formato de parametros', () => {
    fijarUrl('/auth?state=abc#seccion');
    const reemplazar = vi.spyOn(window.history, 'replaceState');

    limpiarParametrosMsal();

    expect(reemplazar).toHaveBeenCalledWith({}, document.title, '/auth#seccion');
  });

  it('no reemplaza la URL si el hash con parametros no incluye claves de MSAL', () => {
    fijarUrl('/auth#seccion=notas&page=2');
    const reemplazar = vi.spyOn(window.history, 'replaceState');

    limpiarParametrosMsal();

    expect(reemplazar).not.toHaveBeenCalled();
  });
});
