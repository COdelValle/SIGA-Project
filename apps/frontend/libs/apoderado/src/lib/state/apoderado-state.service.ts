import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { AsistenciaService, PerfilEstudianteService } from '@siga/academico';
import { APP_CONFIG, EstadoRemoto, recursoRemoto } from '@siga/core';
import { PUPILOS_MOCK, Pupilo } from '@siga/mocks';
import { combineLatest, of, switchMap } from 'rxjs';
import { ApoderadoService } from './apoderado.service';

/** Estado compartido del portal apoderado: pupilo seleccionado, perfil y asistencias. */
@Injectable({ providedIn: 'root' })
export class ApoderadoStateService {
  private readonly config = inject(APP_CONFIG);
  private readonly perfilService = inject(PerfilEstudianteService);
  private readonly asistenciaService = inject(AsistenciaService);
  private readonly apoderadoService = inject(ApoderadoService);

  private readonly current = signal<number>(
    this.config.useMocks ? (PUPILOS_MOCK[0]?.id ?? 1) : 0,
  );
  private readonly recarga = signal(0);
  private readonly recarga$ = toObservable(this.recarga);

  readonly pupiloId = this.current.asReadonly();
  private readonly pupiloId$ = toObservable(this.pupiloId);

  private readonly pupilosRecurso = recursoRemoto(
    this.recarga$.pipe(switchMap(() => this.apoderadoService.getPupilos())),
  );

  /** Estado de carga de los pupilos reales (error => vista con reintento). */
  readonly estadoPupilos = computed<EstadoRemoto>(() => this.pupilosRecurso().estado);

  /** Pupilos del apoderado (BFF en real, demo en `useMocks`). */
  readonly pupilos = computed<Pupilo[]>(() =>
    this.config.useMocks ? PUPILOS_MOCK : this.pupilosRecurso().dato ?? [],
  );

  private readonly perfilRecurso = recursoRemoto(
    combineLatest([this.recarga$, this.pupiloId$]).pipe(
      switchMap(([, id]) => (id ? this.perfilService.getPerfil(id) : of(null))),
    ),
  );

  private readonly asistenciasRecurso = recursoRemoto(
    combineLatest([this.recarga$, this.pupiloId$]).pipe(
      switchMap(([, id]) => (id ? this.asistenciaService.getAsistenciasEstudiante(id) : of(null))),
    ),
  );

  /** Estado de carga del perfil del pupilo seleccionado. */
  readonly estadoPerfil = computed<EstadoRemoto>(() => this.perfilRecurso().estado);

  /** Estado de carga de las asistencias del pupilo seleccionado. */
  readonly estadoAsistencias = computed<EstadoRemoto>(() => this.asistenciasRecurso().estado);

  /** Perfil del pupilo seleccionado (null mientras carga o si no hay dato). */
  readonly perfil = computed(() => this.perfilRecurso().dato);

  /** Asistencias del pupilo seleccionado (null mientras carga o si no hay dato). */
  readonly asistencias = computed(() => this.asistenciasRecurso().dato);

  constructor() {
    effect(() => {
      if (this.config.useMocks) {
        return;
      }
      const pupilos = this.pupilos();
      if (pupilos.length > 0 && !pupilos.some((pupilo) => pupilo.id === this.current())) {
        this.current.set(pupilos[0].id);
      }
    });
  }

  seleccionar(pupiloId: number): void {
    this.current.set(pupiloId);
  }

  /** Invalida los caches y reintenta pupilos, perfil y asistencias. */
  reintentar(): void {
    this.apoderadoService.invalidar();
    this.perfilService.invalidar();
    this.asistenciaService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }
}
