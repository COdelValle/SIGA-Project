export interface Pupilo {
  id: number;
  nombre: string;
  relacion: string;
  curso: string;
}

/** Datos de ejemplo mientras el BFF no exponga los estudiantes vinculados. */
export const PUPILOS_MOCK: Pupilo[] = [
  { id: 1, nombre: 'Camila Antonieta Soto Hernández', relacion: 'Hija', curso: '8° Básico A' },
  { id: 2, nombre: 'Matías Ignacio Soto Hernández', relacion: 'Hijo', curso: '5° Básico B' },
];
