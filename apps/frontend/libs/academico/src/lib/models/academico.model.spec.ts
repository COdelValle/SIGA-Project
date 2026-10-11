import { describe, expect, it } from 'vitest';
import { NotaDetalle, promedioNotas } from './academico.model';

function nota(valor: number, ponderacion: number): NotaDetalle {
  return { numero: 1, nombre: 'PRUEBA', valor, ponderacion };
}

describe('promedioNotas', () => {
  it('pondera solo las notas con ponderación mayor que 0', () => {
    const notas = [nota(6.0, 60), nota(7.0, 40), nota(2.0, 0)];

    expect(promedioNotas(notas, 'PONDERADO')).toBe(6.4);
  });

  it('devuelve 0 cuando ninguna nota pondera (FORMATIVA/DIAGNOSTICO)', () => {
    const notas = [nota(7.0, 0), nota(1.0, 0)];

    expect(promedioNotas(notas, 'PONDERADO')).toBe(0);
  });

  it('mantiene el promedio simple cuando el modo es SIMPLE', () => {
    const notas = [nota(6.0, 0), nota(4.0, 0)];

    expect(promedioNotas(notas, 'SIMPLE')).toBe(5);
  });

  it('devuelve 0 sin notas', () => {
    expect(promedioNotas([], 'PONDERADO')).toBe(0);
  });
});
