import { Injectable, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { AsistenciaService, PerfilEstudianteService } from '@siga/academico';
import { PUPILOS_MOCK } from '@siga/mocks';
import { switchMap } from 'rxjs';

/** Estado compartido del portal apoderado: pupilo seleccionado, perfil y asistencias. */
@Injectable({ providedIn: 'root' })
export class ApoderadoStateService {
  private readonly current = signal<number>(PUPILOS_MOCK[0]?.id ?? 1);
  private readonly perfilService = inject(PerfilEstudianteService);
  private readonly asistenciaService = inject(AsistenciaService);

  readonly pupiloId = this.current.asReadonly();

  /** Perfil del pupilo seleccionado (null => las vistas usan el mock de respaldo). */
  readonly perfil = toSignal(
    toObservable(this.pupiloId).pipe(switchMap((id) => this.perfilService.getPerfil(id))),
    { initialValue: null },
  );

  /** Asistencias del pupilo seleccionado (null => mock de respaldo). */
  readonly asistencias = toSignal(
    toObservable(this.pupiloId).pipe(switchMap((id) => this.asistenciaService.getAsistenciasEstudiante(id))),
    { initialValue: null },
  );

  seleccionar(pupiloId: number): void {
    this.current.set(pupiloId);
  }
}
