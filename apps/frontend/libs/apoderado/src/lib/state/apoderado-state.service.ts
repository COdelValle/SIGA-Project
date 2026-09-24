import { Injectable, signal } from '@angular/core';
import { PUPILOS_MOCK } from '../mocks/pupilos.mock';

/** Estado compartido del portal apoderado: pupilo seleccionado. */
@Injectable({ providedIn: 'root' })
export class ApoderadoStateService {
  private readonly current = signal<number>(PUPILOS_MOCK[0]?.id ?? 1);

  readonly pupiloId = this.current.asReadonly();

  seleccionar(pupiloId: number): void {
    this.current.set(pupiloId);
  }
}
