import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { APP_CONFIG } from '@siga/core';
import { Pupilo } from '@siga/mocks';
import { Observable, map, of, shareReplay } from 'rxjs';

interface PupiloDTO {
  id: number;
  nombre: string;
  relacion: string | null;
  curso: string | null;
}

function relacionDisplay(relacion: string | null): string {
  switch (relacion) {
    case 'MADRE_PADRE':
      return 'Hija';
    case 'TUTOR_LEGAL':
      return 'Pupilo(a)';
    case 'TIA_TIO':
      return 'Sobrino(a)';
    case 'ABUELA_ABUELO':
      return 'Nieto(a)';
    case 'HERMANA_HERMANO':
      return 'Hermano(a)';
    case 'PRIMO_PRIMA':
      return 'Primo(a)';
    default:
      return relacion ?? '';
  }
}

/**
 * Pupilos reales del apoderado autenticado. En modo demo (`useMocks`) devuelve
 * `null`; los errores reales se propagan a la vista (sin fallback a mocks).
 */
@Injectable({ providedIn: 'root' })
export class ApoderadoService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);
  private cache$?: Observable<Pupilo[] | null>;

  getPupilos(): Observable<Pupilo[] | null> {
    if (!this.cache$) {
      this.cache$ = this.config.useMocks
        ? of<Pupilo[] | null>(null)
        : this.http
            .get<PupiloDTO[]>(`${this.config.bffBaseUrl}/bff/v1/apoderados/pupilos`)
            .pipe(
              map((pupilos) =>
                pupilos.map((pupilo) => ({
                  id: pupilo.id,
                  nombre: pupilo.nombre,
                  relacion: relacionDisplay(pupilo.relacion),
                  curso: pupilo.curso ?? '',
                })),
              ),
            );
    }
    return this.cache$.pipe(shareReplay(1));
  }

  /** Invalida el cache de pupilos para forzar una recarga. */
  invalidar(): void {
    this.cache$ = undefined;
  }
}
