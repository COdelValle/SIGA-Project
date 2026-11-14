import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { MenuIconComponent } from './menu-icon.component';
import { MenuIconName } from './menu-item.model';

const ICONOS: MenuIconName[] = [
  'home',
  'clock',
  'notes',
  'attendance',
  'courses',
  'requests',
  'users',
  'roles',
  'subjects',
  'curriculum',
  'progress',
  'register-notes',
  'register-attendance',
];

describe('MenuIconComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('dibuja un svg accesible con trazos para cada nombre del menu', () => {
    for (const nombre of ICONOS) {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({ imports: [MenuIconComponent] });
      const fixture = TestBed.createComponent(MenuIconComponent);
      fixture.componentRef.setInput('name', nombre);
      fixture.detectChanges();

      const svg = (fixture.nativeElement as HTMLElement).querySelector('svg');
      expect(svg, `icono ${nombre}`).not.toBeNull();
      expect(svg?.getAttribute('aria-hidden'), `icono ${nombre}`).toBe('true');
      expect(svg?.querySelectorAll('path, circle').length, `icono ${nombre}`).toBeGreaterThan(0);
      fixture.destroy();
    }
  });

  it('un nombre desconocido no dibuja ningun icono', () => {
    TestBed.configureTestingModule({ imports: [MenuIconComponent] });
    const fixture = TestBed.createComponent(MenuIconComponent);
    fixture.componentRef.setInput('name', 'desconocido' as MenuIconName);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('svg')).toBeNull();
    fixture.destroy();
  });
});
