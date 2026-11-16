import { Location } from '@angular/common';
import { HttpClient, HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  MSAL_INTERCEPTOR_CONFIG,
  MsalBroadcastService,
  MsalInterceptor,
  MsalService,
} from '@azure/msal-angular';
import { InteractionStatus, InteractionType } from '@azure/msal-browser';
import { of } from 'rxjs';
import { AppConfig } from '../config/app-config.model';
import { msalInterceptorConfigFactory } from './msal.factory';

const config: AppConfig = {
  bffBaseUrl: 'https://siga.test/api',
  useMocks: false,
  msal: {
    clientId: 'spa-client',
    authority: 'https://login.microsoftonline.com/tenant',
    redirectUri: '/auth',
    postLogoutRedirectUri: '/sin-acceso',
    scopes: ['api://api-client/Acceso.Base', 'api://api-client/usuarios:read'],
  },
};
const localConfig: AppConfig = { ...config, bffBaseUrl: '/api' };

describe('msalInterceptorConfigFactory', () => {
  it('protege el BFF con todos los scopes configurados y strict matching', () => {
    const result = msalInterceptorConfigFactory(config);

    expect(result.interactionType).toBe(InteractionType.Redirect);
    expect(result.strictMatching).toBe(true);
    expect(result.protectedResourceMap.get('https://siga.test/api/*'))
      .toEqual(config.msal.scopes);
  });

  it('admite la ruta relativa /api usada por Nginx/ng serve', () => {
    const local = msalInterceptorConfigFactory({ ...config, bffBaseUrl: '/api' });

    expect(local.protectedResourceMap.get('/api/*')).toEqual(config.msal.scopes);
  });
});

describe('MsalInterceptor registrado en HTTP', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let acquireTokenSilent: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    const account = { homeAccountId: 'account-1' };
    const logger = {
      verbose: vi.fn(),
      info: vi.fn(),
      infoPii: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
    };
    acquireTokenSilent = vi.fn(() => of({ accessToken: 'access-token', account }));
    const msalService = {
      instance: {
        getActiveAccount: () => account,
        getAllAccounts: () => [account],
      },
      getLogger: () => logger,
      acquireTokenSilent,
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: Location, useValue: { normalize: (url: string) => url } },
        {
          provide: MSAL_INTERCEPTOR_CONFIG,
          useFactory: () => msalInterceptorConfigFactory(localConfig),
        },
        { provide: MsalService, useValue: msalService },
        { provide: MsalBroadcastService, useValue: { inProgress$: of(InteractionStatus.None) } },
        { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    TestBed.resetTestingModule();
  });

  it('adjunta el access token a una solicitud protegida del BFF', () => {
    http.get('/api/me').subscribe();

    const request = httpTesting.expectOne('/api/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer access-token');
    request.flush({ id: 'oid', roles: ['ADMIN'] });
  });

  it('no adjunta el access token a un recurso fuera del mapa protegido', () => {
    const interceptor = TestBed.inject(HTTP_INTERCEPTORS).find(
      (provider) => provider instanceof MsalInterceptor,
    ) as unknown as {
      getScopesForEndpoint: (url: string, method: string) => string[] | null;
      location: { normalize(url: string): string };
      getAbsoluteUrl(url: string): string;
    };
    expect(interceptor.getScopesForEndpoint('/assets/config.json', 'GET')).toBeNull();

    http.get('/assets/config.json').subscribe();

    const request = httpTesting.expectOne('/assets/config.json');
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(acquireTokenSilent).not.toHaveBeenCalled();
    request.flush({});
  });
});
