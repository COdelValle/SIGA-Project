import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { firstValueFrom, Observable, of, throwError } from 'rxjs';
import { AccessTokenClaims, AccessTokenClaimsService } from './access-token-claims.service';
import { AuthErrorService } from './auth-error.service';
import { MeService } from './me.service';
import { roleGuard } from './role.guard';

describe('roleGuard', () => {
  const scopes = ['Acceso.Base', 'usuarios:read'];

  function configure(meRoles: ('ADMIN' | 'DOCENTE' | 'APODERADO' | 'ESTUDIANTE')[], claims: AccessTokenClaims) {
    const claimsService = {
      getAccessTokenClaims: () => of(claims),
      getMissingConfiguredScopes: (tokenClaims: AccessTokenClaims) =>
        scopes.filter((scope) => !tokenClaims.scopes.includes(scope)),
    };

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: MeService, useValue: { getMe: () => of({ id: 'oid', roles: meRoles }) } },
        { provide: AccessTokenClaimsService, useValue: claimsService },
        AuthErrorService,
      ],
    });
  }

  async function evaluate(roles: ('ADMIN' | 'DOCENTE' | 'APODERADO' | 'ESTUDIANTE')[]) {
    const guard = roleGuard(roles);
    const result = TestBed.runInInjectionContext(() =>
      guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
    return firstValueFrom(result as Observable<boolean | UrlTree>);
  }

  afterEach(() => TestBed.resetTestingModule());

  it('permite entrar cuando perfil y claim de rol coinciden y el token tiene los scopes', async () => {
    configure(['ADMIN'], { roles: ['ADMIN'], scopes });

    await expect(evaluate(['ADMIN'])).resolves.toBe(true);
  });

  it('redirige a sin acceso si el claim de rol no coincide con el perfil SIGA', async () => {
    configure(['ADMIN'], { roles: ['DOCENTE'], scopes });

    const result = await evaluate(['ADMIN']);

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/sin-acceso');
    expect(TestBed.inject(AuthErrorService).error()?.code).toBe('role_claim_mismatch');
  });

  it('redirige a error de acceso si faltan scopes solicitados por el frontend', async () => {
    configure(['ADMIN'], { roles: ['ADMIN'], scopes: ['Acceso.Base'] });

    const result = await evaluate(['ADMIN']);

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/error-acceso');
    expect(TestBed.inject(AuthErrorService).error()?.code).toBe('missing_scope');
  });

  it('envía a la página del rol autorizado cuando solicita otra ruta', async () => {
    configure(['DOCENTE'], { roles: ['DOCENTE'], scopes });

    const result = await evaluate(['ADMIN']);

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/docente');
  });

  it('redirige a error de acceso cuando no puede adquirir el token silenciosamente', async () => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: MeService, useValue: { getMe: () => of({ id: 'oid', roles: ['ADMIN'] }) } },
        {
          provide: AccessTokenClaimsService,
          useValue: {
            getAccessTokenClaims: () =>
              throwError(() =>
                Object.assign(new Error('La interacción es requerida.'), {
                  name: 'InteractionRequiredAuthError',
                }),
              ),
            getMissingConfiguredScopes: () => [],
          },
        },
        AuthErrorService,
      ],
    });

    const result = await evaluate(['ADMIN']);

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/error-acceso');
    expect(TestBed.inject(AuthErrorService).error()).toEqual({
      code: 'InteractionRequiredAuthError',
      message: 'La interacción es requerida.',
      detail: undefined,
    });
  });
});
