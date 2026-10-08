import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { MeService, Notificacion, NotificationService, RefrescoDatosService, TipoNotificacion } from '@siga/core';
import { catchError, filter, fromEvent, merge, of, switchMap, take, timer } from 'rxjs';

@Component({
  selector: 'siga-notification-bell',
  imports: [DatePipe],
  template: `
    @if (visible()) {
      <div class="relative">
        <button
          type="button"
          (click)="toggle()"
          class="relative flex h-9 w-9 items-center justify-center rounded-full border border-gold/50 text-gold transition hover:bg-gold/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-gold"
          [class.animate-pulse]="pulso()"
          aria-label="Notificaciones"
          aria-haspopup="dialog"
          [attr.aria-expanded]="abierta()"
        >
          @if (pulso()) {
            <span class="pointer-events-none absolute inset-0 animate-ping rounded-full bg-gold/40" aria-hidden="true"></span>
          }
          <svg
            class="h-5 w-5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9" />
            <path d="M10 21h4" />
          </svg>
          @if (noLeidas() > 0) {
            <span
              class="absolute -right-1 -top-1 flex min-h-4 min-w-4 items-center justify-center rounded-full bg-bad px-1 text-[10px] font-bold leading-none text-white"
              aria-label="Notificaciones sin leer"
            >
              {{ noLeidas() > 99 ? '99+' : noLeidas() }}
            </span>
          }
        </button>

        @if (abierta()) {
          <section
            class="absolute right-0 z-50 mt-3 max-h-[min(32rem,75vh)] w-[min(24rem,calc(100vw-2rem))] overflow-y-auto rounded-xl border border-line bg-panel text-ink shadow-2xl"
            role="dialog"
            aria-label="Bandeja de notificaciones"
          >
            <div class="border-b border-line px-4 py-3">
              <div class="flex items-center justify-between gap-2">
                <h2 class="font-semibold text-ink">Notificaciones</h2>
                <button
                  type="button"
                  (click)="cerrar()"
                  class="rounded px-2 py-1 text-sm text-muted transition hover:bg-surface focus-visible:outline focus-visible:outline-2 focus-visible:outline-gold"
                  aria-label="Cerrar notificaciones"
                >
                  Cerrar
                </button>
              </div>
              @if (notificaciones().length > 0) {
                <div class="mt-2 flex flex-wrap items-center gap-3 text-xs font-semibold">
                  <button
                    type="button"
                    (click)="marcarTodas()"
                    [disabled]="accionEnCurso() || !hayNoLeidas()"
                    class="text-brand transition hover:underline disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Marcar todas como leídas
                  </button>
                  <button
                    type="button"
                    (click)="limpiarLeidas()"
                    [disabled]="accionEnCurso() || !hayLeidas()"
                    class="text-muted transition hover:underline disabled:cursor-not-allowed disabled:opacity-40"
                  >
                    Limpiar leídas
                  </button>
                </div>
                @if (errorAccion()) {
                  <p class="mt-2 text-xs font-medium text-bad" role="alert">
                    No se pudo completar la acción. Reintenta.
                  </p>
                }
              }
            </div>

            @if (cargando()) {
              <p class="px-4 py-6 text-sm text-muted" role="status">Cargando notificaciones…</p>
            } @else if (errorCarga()) {
              <div class="px-4 py-5 text-sm" role="alert">
                <p class="text-ink">No se pudieron cargar las notificaciones.</p>
                <button type="button" (click)="cargar()" class="mt-2 font-semibold text-brand">
                  Reintentar
                </button>
              </div>
            } @else if (notificaciones().length === 0) {
              <p class="px-4 py-6 text-sm text-muted">No tienes notificaciones.</p>
            } @else {
              <ul>
                @for (notificacion of notificaciones(); track notificacion.id) {
                  <li class="border-b border-line last:border-b-0">
                    <button
                      type="button"
                      (click)="abrir(notificacion)"
                      class="w-full border-l-4 px-4 py-3 text-left transition hover:bg-surface focus-visible:outline focus-visible:outline-2 focus-visible:outline-inset focus-visible:outline-gold"
                      [class.border-brand]="!notificacion.leida"
                      [class.bg-surface]="!notificacion.leida"
                      [class.border-transparent]="notificacion.leida"
                    >
                      <span class="flex items-start justify-between gap-3">
                        <span class="font-semibold text-ink">{{ notificacion.titulo }}</span>
                        @if (!notificacion.leida) {
                          <span class="mt-1 h-2 w-2 shrink-0 rounded-full bg-brand" aria-label="Sin leer"></span>
                        }
                      </span>
                      <span class="mt-1 block text-sm text-muted">{{ notificacion.resumen }}</span>
                      <time class="mt-2 block text-xs text-muted" [attr.datetime]="notificacion.fechaHora">
                        {{ notificacion.fechaHora | date: 'dd/MM/yyyy HH:mm' }}
                      </time>
                    </button>
                  </li>
                }
              </ul>
            }
          </section>
        }
      </div>
    }
  `,
})
export class NotificationBellComponent {
  private static readonly POLL_MS = 10_000;
  private static readonly PULSO_MS = 4_000;

  private readonly meService = inject(MeService);
  private readonly notificationService = inject(NotificationService);
  private readonly refresco = inject(RefrescoDatosService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly visible = signal(false);
  protected readonly abierta = signal(false);
  protected readonly cargando = signal(false);
  protected readonly errorCarga = signal(false);
  protected readonly errorAccion = signal(false);
  protected readonly accionEnCurso = signal(false);
  protected readonly pulso = signal(false);
  protected readonly noLeidas = signal(0);
  protected readonly notificaciones = signal<Notificacion[]>([]);
  protected readonly hayNoLeidas = computed(() => this.notificaciones().some((n) => !n.leida));
  protected readonly hayLeidas = computed(() => this.notificaciones().some((n) => n.leida));
  private rolActual: 'ESTUDIANTE' | 'APODERADO' | null = null;

  constructor() {
    this.meService.getMe().pipe(take(1), takeUntilDestroyed(this.destroyRef)).subscribe((me) => {
      if (me.roles.includes('ESTUDIANTE')) {
        this.rolActual = 'ESTUDIANTE';
      } else if (me.roles.includes('APODERADO')) {
        this.rolActual = 'APODERADO';
      }
      this.visible.set(this.rolActual !== null);
      if (this.visible()) {
        // Contador casi en vivo: polling cada 10 s + refresco al volver a la
        // pestaña, para que el badge aumente sin tener que abrir la campana.
        merge(
          timer(0, NotificationBellComponent.POLL_MS),
          fromEvent(window, 'focus'),
          fromEvent(document, 'visibilitychange').pipe(
            filter(() => document.visibilityState === 'visible'),
          ),
        )
          .pipe(
            switchMap(() => this.notificationService.getUnreadCount().pipe(catchError(() => of(null)))),
            takeUntilDestroyed(this.destroyRef),
          )
          .subscribe((contador) => this.aplicarContador(contador?.noLeidas ?? 0));
      }
    });
  }

  protected toggle(): void {
    this.abierta.update((abierta) => !abierta);
    if (this.abierta()) {
      this.errorAccion.set(false);
      this.cargar();
      this.refrescarConteo();
    }
  }

  protected cerrar(): void {
    this.abierta.set(false);
  }

  protected cargar(): void {
    this.cargando.set(true);
    this.errorCarga.set(false);
    this.notificationService.getMine(0, 10)
      .pipe(
        take(1),
        catchError(() => {
          this.errorCarga.set(true);
          return of(null);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((pagina) => {
        if (pagina) {
          this.notificaciones.set(pagina.content);
        }
        this.cargando.set(false);
      });
  }

  /** Clic: marca leída de inmediato (optimista), navega y confirma contra el BFF. */
  protected abrir(notificacion: Notificacion): void {
    // La vista destino debe reflejar el cambio notificado sin recargar.
    this.refresco.solicitarRefresco();
    if (notificacion.leida) {
      this.cerrar();
      void this.router.navigateByUrl(this.rutaDe(notificacion.tipo));
      return;
    }
    this.marcarLocalmente(notificacion.id);
    this.notificationService.markAsRead(notificacion.id)
      .pipe(
        take(1),
        catchError(() => {
          // Si el BFF falla, se recarga para no mostrar un estado falso y el
          // mensaje inline queda visible en el panel.
          this.errorAccion.set(true);
          this.refrescarConteo();
          this.cargar();
          return of(undefined);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.cerrar();
        void this.router.navigateByUrl(this.rutaDe(notificacion.tipo));
      });
  }

  /** Conserva los ítems en la lista, quita el punto y baja el contador a 0. */
  protected marcarTodas(): void {
    this.accionEnCurso.set(true);
    this.errorAccion.set(false);
    this.notificationService.markAllAsRead()
      .pipe(
        take(1),
        catchError(() => {
          this.errorAccion.set(true);
          return of(undefined);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.notificaciones.update((lista) => lista.map((n) => ({ ...n, leida: true })));
        this.noLeidas.set(0);
        this.accionEnCurso.set(false);
      });
  }

  /** Oculta solo las leídas del usuario; las no leídas permanecen. */
  protected limpiarLeidas(): void {
    this.accionEnCurso.set(true);
    this.errorAccion.set(false);
    this.notificationService.clearRead()
      .pipe(
        take(1),
        catchError(() => {
          this.errorAccion.set(true);
          return of(undefined);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        this.notificaciones.update((lista) => lista.filter((n) => !n.leida));
        this.accionEnCurso.set(false);
      });
  }

  private aplicarContador(nuevo: number): void {
    const anterior = this.noLeidas();
    this.noLeidas.set(nuevo);
    if (nuevo > anterior) {
      this.pulsar();
      // Llegó algo nuevo: las vistas activas (notas, asistencias) se recargan.
      this.refresco.solicitarRefresco();
      if (this.abierta() && !this.cargando()) {
        this.cargar();
      }
    }
  }

  private pulsar(): void {
    this.pulso.set(true);
    setTimeout(() => this.pulso.set(false), NotificationBellComponent.PULSO_MS);
  }

  private marcarLocalmente(id: number): void {
    this.notificaciones.update((lista) =>
      lista.map((n) => (n.id === id ? { ...n, leida: true } : n)),
    );
    this.noLeidas.update((contador) => Math.max(0, contador - 1));
  }

  private refrescarConteo(): void {
    this.notificationService.getUnreadCount()
      .pipe(take(1), catchError(() => of(null)), takeUntilDestroyed(this.destroyRef))
      .subscribe((contador) => this.aplicarContador(contador?.noLeidas ?? 0));
  }

  private rutaDe(tipo: TipoNotificacion): string {
    const portal = this.rolActual === 'APODERADO' ? '/apoderado' : '/estudiante';
    if (tipo === 'ASISTENCIA') {
      return `${portal}/asistencias`;
    }
    return tipo === 'EVALUACION' ? `${portal}/progreso` : `${portal}/notas`;
  }
}
