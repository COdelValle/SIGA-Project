import { TestBed } from '@angular/core/testing';
import { MsalService } from '@azure/msal-angular';
import { AccountInfo, IPublicClientApplication } from '@azure/msal-browser';
import { firstValueFrom } from 'rxjs';
import { APP_CONFIG, AppConfig } from '../config/app-config.model';
import { AccessTokenClaimsService, decodeAccessTokenClaims } from './access-token-claims.service';

const config: AppConfig = {
  bffBaseUrl: '/api',
  useMocks: false,
  msal: {
    clientId: 'spa-client',
    authority: 'https://login.microsoftonline.com/tenant',
    redirectUri: '/auth',
    postLogoutRedirectUri: '/sin-acceso',
    scopes: ['api://api-client/Acceso.Base', 'api://api-client/usuarios:read'],
  },
};

function jwt(payload: Record<string, unknown>): string {
  const encoded = btoa(JSON.stringify(payload))
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
  return `header.${encoded}.signature`;
}

describe('AccessTokenClaimsService', () => {
  const account = { homeAccountId: 'account-1' } as AccountInfo;
  let acquireTokenSilent: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    acquireTokenSilent = vi.fn().mockResolvedValue({
      accessToken: jwt({ roles: ['ADMIN', 'UNKNOWN'], scp: 'Acceso.Base usuarios:read' }),
    });
    const instance = {
      getActiveAccount: () => account,
      getAllAccounts: () => [account],
      acquireTokenSilent,
    } as unknown as IPublicClientApplication;

    TestBed.configureTestingModule({
      providers: [
        AccessTokenClaimsService,
        { provide: APP_CONFIG, useValue: config },
        { provide: MsalService, useValue: { instance } },
      ],
    });
  });

  afterEach(() => TestBed.resetTestingModule());

  it('obtiene claims del access token MSAL y solicita los scopes de la app', async () => {
    const service = TestBed.inject(AccessTokenClaimsService);

    await expect(firstValueFrom(service.getAccessTokenClaims())).resolves.toEqual({
      roles: ['ADMIN'],
      scopes: ['Acceso.Base', 'usuarios:read'],
    });
    expect(acquireTokenSilent).toHaveBeenCalledWith({
      scopes: config.msal.scopes,
      account,
    });
  });

  it('identifica los scopes configurados que no aparecen en scp', () => {
    const service = TestBed.inject(AccessTokenClaimsService);

    expect(service.getMissingConfiguredScopes({ roles: ['ADMIN'], scopes: ['Acceso.Base'] }))
      .toEqual(['usuarios:read']);
  });

  it('rechaza un token que no sea JWT en lugar de confiar en claims vacíos', () => {
    expect(() => decodeAccessTokenClaims('not-a-jwt')).toThrow('no tiene formato JWT');
  });

  it('rechaza un payload que no sea JSON', () => {
    const invalidPayload = btoa('not-json').replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');

    expect(() => decodeAccessTokenClaims(`header.${invalidPayload}.signature`))
      .toThrow('No se pudieron leer los claims');
  });
});
