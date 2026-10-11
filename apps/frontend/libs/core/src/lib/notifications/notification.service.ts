import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config/app-config.model';
import { ContadorNotificaciones, PaginaNotificaciones } from './notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(APP_CONFIG);
  private readonly endpoint = `${this.config.bffBaseUrl}/bff/v1/notificaciones/me`;

  getMine(page = 0, size = 10): Observable<PaginaNotificaciones> {
    return this.http.get<PaginaNotificaciones>(this.endpoint, {
      params: { page, size },
    });
  }

  getUnreadCount(): Observable<ContadorNotificaciones> {
    return this.http.get<ContadorNotificaciones>(`${this.endpoint}/no-leidas/count`);
  }

  markAsRead(id: number): Observable<void> {
    return this.http.patch<void>(`${this.endpoint}/${id}/leida`, null);
  }

  /** Marca todas las visibles como leídas (baja el contador, no borra nada). */
  markAllAsRead(): Observable<void> {
    return this.http.patch<void>(`${this.endpoint}/leidas`, null);
  }

  /** Oculta de mi bandeja las notificaciones ya leídas. */
  clearRead(): Observable<void> {
    return this.http.delete<void>(`${this.endpoint}/leidas`);
  }
}
