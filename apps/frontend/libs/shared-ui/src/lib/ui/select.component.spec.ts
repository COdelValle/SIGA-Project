import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { SelectComponent } from './select.component';
import { SelectOption } from './select.model';

const OPCIONES: SelectOption[] = [
  { value: 'a', label: 'Opción A' },
  { value: 'b', label: 'Opción B' },
  { value: 3, label: 'Opción C' },
];

function crear(valor: string | number | null = 'b'): ComponentFixture<SelectComponent> {
  TestBed.configureTestingModule({ imports: [SelectComponent] });
  const fixture = TestBed.createComponent(SelectComponent);
  fixture.componentRef.setInput('options', OPCIONES);
  fixture.componentRef.setInput('value', valor);
  fixture.componentRef.setInput('ariaLabel', 'Seleccionar opción');
  fixture.detectChanges();
  return fixture;
}

function disparador(fixture: ComponentFixture<SelectComponent>): HTMLButtonElement {
  return (fixture.nativeElement as HTMLElement).querySelector('button') as HTMLButtonElement;
}

function opciones(fixture: ComponentFixture<SelectComponent>): HTMLButtonElement[] {
  return Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('li button'),
  ) as HTMLButtonElement[];
}

describe('SelectComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('muestra la etiqueta de la opcion seleccionada con accesibilidad', () => {
    const fixture = crear('b');

    const boton = disparador(fixture);
    expect(boton.textContent).toContain('Opción B');
    expect(boton.getAttribute('title')).toBe('Opción B');
    expect(boton.getAttribute('aria-label')).toBe('Seleccionar opción');
    expect(boton.getAttribute('aria-expanded')).toBe('false');
    fixture.destroy();
  });

  it('sin valor seleccionado la etiqueta queda vacia', () => {
    const fixture = crear('inexistente');

    expect(disparador(fixture).textContent?.trim()).toBe('');
    fixture.destroy();
  });

  it('abre el listbox y marca la opcion actual', () => {
    const fixture = crear('b');

    disparador(fixture).click();
    fixture.detectChanges();

    const lista = (fixture.nativeElement as HTMLElement).querySelector('[role="listbox"]');
    expect(lista).not.toBeNull();
    expect(disparador(fixture).getAttribute('aria-expanded')).toBe('true');

    const opcionesLista = (fixture.nativeElement as HTMLElement).querySelectorAll('[role="option"]');
    expect(opcionesLista.length).toBe(3);
    expect(opcionesLista[1].getAttribute('aria-selected')).toBe('true');
    expect(opcionesLista[0].getAttribute('aria-selected')).toBe('false');
    fixture.destroy();
  });

  it('emite el valor elegido y cierra el listbox', () => {
    const fixture = crear('b');
    const cambios = vi.fn();
    fixture.componentInstance.valueChange.subscribe(cambios);

    disparador(fixture).click();
    fixture.detectChanges();
    opciones(fixture)[2].click();
    fixture.detectChanges();

    expect(cambios).toHaveBeenCalledWith(3);
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="listbox"]')).toBeNull();
    fixture.destroy();
  });

  it('cerrar con Escape oculta el listbox', () => {
    const fixture = crear('a');

    disparador(fixture).click();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="listbox"]')).not.toBeNull();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('[role="listbox"]')).toBeNull();
    fixture.destroy();
  });

  it('un clic fuera del componente cierra el listbox', () => {
    const fixture = crear('a');

    disparador(fixture).click();
    fixture.detectChanges();
    document.body.click();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('[role="listbox"]')).toBeNull();
    fixture.destroy();
  });

  it('deshabilitado no abre el listbox', () => {
    const fixture = crear('a');
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const boton = disparador(fixture);
    expect(boton.disabled).toBe(true);
    boton.click();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('[role="listbox"]')).toBeNull();
    fixture.destroy();
  });
});
