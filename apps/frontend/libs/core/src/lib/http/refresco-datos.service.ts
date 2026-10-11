import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

/**
 * Aviso global de datos desactualizados: los servicios con cache se suscriben
 * y vuelven a consultar al BFF. Lo dispara la campana cuando llega o se abre
 * una notificacion, para que las vistas activas reflejen el cambio sin recargar.
 */
@Injectable({ providedIn: 'root' })
export class RefrescoDatosService {
  private readonly solicitudes = new Subject<void>();

  readonly refresco$ = this.solicitudes.asObservable();

  solicitarRefresco(): void {
    this.solicitudes.next();
  }
}
