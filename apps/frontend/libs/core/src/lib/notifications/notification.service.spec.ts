import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '../config/app-config.model';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: APP_CONFIG,
          useValue: {
            bffBaseUrl: '/api',
            useMocks: false,
            msal: {
              clientId: '',
              authority: '',
              redirectUri: '',
              postLogoutRedirectUri: '',
              scopes: [],
            },
          },
        },
      ],
    });
    service = TestBed.inject(NotificationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('obtiene una página propia de notificaciones por el BFF', () => {
    let total = 0;
    service.getMine(1, 5).subscribe((page) => (total = page.totalElements));

    const request = http.expectOne('/api/bff/v1/notificaciones/me?page=1&size=5');
    expect(request.request.method).toBe('GET');
    request.flush({
      content: [],
      totalElements: 3,
      totalPages: 1,
      number: 0,
      size: 5,
      first: true,
      last: true,
      empty: true,
    });

    expect(total).toBe(3);
  });

  it('consulta el contador de no leídas y marca por id', () => {
    let noLeidas = 0;
    service.getUnreadCount().subscribe((result) => (noLeidas = result.noLeidas));
    http.expectOne('/api/bff/v1/notificaciones/me/no-leidas/count').flush({ noLeidas: 4 });
    expect(noLeidas).toBe(4);

    service.markAsRead(25).subscribe();
    const request = http.expectOne('/api/bff/v1/notificaciones/me/25/leida');
    expect(request.request.method).toBe('PATCH');
    request.flush(null);
  });
});
