import { Injectable, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { PerfilEstudianteService } from '@siga/academico';
import { PUPILOS_MOCK } from '@siga/mocks';
import { switchMap } from 'rxjs';

/** Estado compartido del portal apoderado: pupilo seleccionado y su perfil real. */
@Injectable({ providedIn: 'root' })
export class ApoderadoStateService {
  private readonly current = signal<number>(PUPILOS_MOCK[0]?.id ?? 1);
  private readonly perfilService = inject(PerfilEstudianteService);

  readonly pupiloId = this.current.asReadonly();

  /** Perfil del pupilo seleccionado (null => las vistas usan el mock de respaldo). */
  readonly perfil = toSignal(
    toObservable(this.pupiloId).pipe(switchMap((id) => this.perfilService.getPerfil(id))),
    { initialValue: null },
  );

  seleccionar(pupiloId: number): void {
    this.current.set(pupiloId);
  }
}
