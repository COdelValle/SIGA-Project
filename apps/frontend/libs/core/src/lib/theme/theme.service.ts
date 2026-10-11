import { Injectable, effect, signal } from '@angular/core';

export type Theme = 'dark' | 'light';

const STORAGE_KEY = 'siga-theme';

/**
 * Tema visual de la aplicacion. Oscuro (magenta/dorado) por defecto; claro
 * (azul) alternativo. Persiste la preferencia en localStorage y la refleja en
 * el atributo `data-theme` del elemento `<html>`.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly current = signal<Theme>(readInitialTheme());

  readonly theme = this.current.asReadonly();

  constructor() {
    effect(() => {
      const theme = this.current();
      document.documentElement.setAttribute('data-theme', theme);
      localStorage.setItem(STORAGE_KEY, theme);
    });
  }

  toggle(): void {
    this.current.update((theme) => (theme === 'dark' ? 'light' : 'dark'));
  }

  set(theme: Theme): void {
    this.current.set(theme);
  }
}

function readInitialTheme(): Theme {
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored === 'light' ? 'light' : 'dark';
}
