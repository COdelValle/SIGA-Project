import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { APP_CONFIG } from '../config/app-config.model';
import { Me } from '../models/me.model';
import { Rol } from '../models/role.enum';

@Injectable({ providedIn: 'root' })
export class MeService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);

  private cache$?: Observable<Me>;

  getMe(force = false): Observable<Me> {
    if (!this.cache$ || force) {
      this.cache$ = this.http
        .get<Me>(`${this.config.bffBaseUrl}/me`)
        .pipe(shareReplay(1));
    }
    return this.cache$;
  }

  clear(): void {
    this.cache$ = undefined;
  }

  hasRole(me: Me | null, roles: Rol[]): boolean {
    return !!me && me.roles.some((rol) => roles.includes(rol));
  }
}
