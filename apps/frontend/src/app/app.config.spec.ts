import { ApplicationInitStatus } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MSAL_INTERCEPTOR_CONFIG,
  MsalGuard,
  MsalService,
} from '@azure/msal-angular';
import { APP_CONFIG, AuthErrorService, AppConfig } from '@siga/core';
import { of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { appConfig } from './app.config';

const config: AppConfig = {
  bffBaseUrl: '/api',
  useMocks: false,
  msal: {
    clientId: 'spa-client',
    authority: 'https://login.microsoftonline.com/tenant',
    redirectUri: '/auth',
    postLogoutRedirectUri: '/sin-acceso',
    scopes: ['api://api-client/Acceso.Base'],
  },
};

const configMock: AppConfig = {
  ...config,
  authMode: 'mock',
  mockAccessToken: 'token-e2e',
};

const cuenta = {
  homeAccountId: 'e2e-account',
  environment: 'e2e',
  tenantId: 'e2e',
  username: 'e2e@siga.cl',
  localAccountId: 'e2e',
  name: 'E2E SIGA',
};

function crearMsal(opciones: {
  activa?: unknown;
  cuentas?: unknown[];
  retorno?: ReturnType<typeof of> | ReturnType<typeof throwError>;
} = {}) {
  const setActiveAccount = vi.fn();
  return {
    instance: {
      getActiveAccount: () => opciones.activa ?? null,
      getAllAccounts: () => opciones.cuentas ?? [],
      setActiveAccount,
    },
    handleRedirectObservable: () => opciones.retorno ?? of(null),
    setActiveAccount,
  };
}

async function inicializar(msal: ReturnType<typeof crearMsal>): Promise<AuthErrorService> {
  TestBed.configureTestingModule({
    providers: [...appConfig(config).providers, { provide: MsalService, useValue: msal }],
  });
  const estado = TestBed.inject(ApplicationInitStatus);
  await estado.donePromise;
  return TestBed.inject(AuthErrorService);
}

describe('appConfig', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, '', '/');
    TestBed.resetTestingModule();
  });

  it('en modo mock expone la configuración y un token de prueba', async () => {
    const resultado = appConfig(configMock);

    TestBed.configureTestingModule({ providers: resultado.providers });
    const configuracion = TestBed.inject(APP_CONFIG);
    const msal = TestBed.inject(MsalService) as unknown as {
      instance: {
        getAllAccounts: () => { username: string }[];
        getActiveAccount: () => { username: string } | null;
        setActiveAccount: (cuenta: null) => void;
        acquireTokenSilent: () => Promise<{ accessToken: string }>;
        clearCache: () => Promise<void>;
        logoutRedirect: () => Promise<void>;
      };
      acquireTokenSilent: () => { subscribe: (fn: (valor: { accessToken: string }) => void) => void };
      handleRedirectObservable: () => { subscribe: (fn: (valor: null) => void) => void };
      getLogger: () => {
        verbose: () => void;
        info: () => void;
        infoPii: () => void;
        error: () => void;
        warning: () => void;
      };
    };
    const interceptor = TestBed.inject(MSAL_INTERCEPTOR_CONFIG);
    const guard = TestBed.inject(MsalGuard) as unknown as { canActivate: () => boolean };

    expect(configuracion).toBe(configMock);
    expect(guard.canActivate()).toBe(true);
    expect(interceptor.protectedResourceMap.get('/api/*')).toEqual(config.msal.scopes);
    expect(msal.instance.getAllAccounts().length).toBe(1);
    expect(msal.instance.getActiveAccount()?.username).toBe('e2e@siga.cl');

    msal.instance.setActiveAccount(null);
    expect((await msal.instance.acquireTokenSilent()).accessToken).toBe('token-e2e');
    await msal.instance.clearCache();
    await msal.instance.logoutRedirect();

    let tokenSilencioso = '';
    msal.acquireTokenSilent().subscribe((valor) => (tokenSilencioso = valor.accessToken));
    expect(tokenSilencioso).toBe('token-e2e');

    let retorno: null | undefined;
    msal.handleRedirectObservable().subscribe((valor) => (retorno = valor));
    expect(retorno).toBeNull();

    const logger = msal.getLogger();
    expect(() => {
      logger.verbose();
      logger.info();
      logger.infoPii();
      logger.error();
      logger.warning();
    }).not.toThrow();
  });

  it('en modo MSAL registra las factories de instancia, guard e interceptor', () => {
    TestBed.configureTestingModule({
      providers: [...appConfig(config).providers, { provide: MsalService, useValue: crearMsal() }],
    });

    const instancia = TestBed.inject(MSAL_INSTANCE);
    const guard = TestBed.inject(MSAL_GUARD_CONFIG);
    const interceptor = TestBed.inject(MSAL_INTERCEPTOR_CONFIG);

    expect(instancia).toBeTruthy();
    const peticion = guard.authRequest as { scopes?: string[] };
    expect(peticion.scopes).toEqual(config.msal.scopes);
    expect(interceptor.protectedResourceMap.get('/api/*')).toEqual(config.msal.scopes);
  });
});

describe('appInitializer de MSAL', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.history.replaceState({}, '', '/');
    TestBed.resetTestingModule();
  });

  it('fija la cuenta activa y limpia la URL de retorno', async () => {
    window.history.replaceState({}, '', '/auth?state=abc&code=123');
    const reemplazar = vi.spyOn(window.history, 'replaceState');
    const msal = crearMsal({ cuentas: [cuenta] });

    const authError = await inicializar(msal);

    expect(msal.setActiveAccount).toHaveBeenCalledWith(cuenta);
    expect(reemplazar).toHaveBeenCalledWith({}, document.title, '/auth');
    expect(authError.error()).toBeNull();
  });

  it('mantiene la cuenta activa existente y no falla sin cuentas', async () => {
    const msal = crearMsal({ activa: cuenta, cuentas: [cuenta] });
    await inicializar(msal);
    expect(msal.setActiveAccount).not.toHaveBeenCalled();

    TestBed.resetTestingModule();
    const sinCuentas = crearMsal({ cuentas: [] });
    await inicializar(sinCuentas);
    expect(sinCuentas.setActiveAccount).not.toHaveBeenCalled();
  });

  it('guarda el error normalizado cuando falla el retorno de Microsoft', async () => {
    const consola = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const error = { errorCode: 'invalid_grant', errorMessage: 'El código expiró' };
    const msal = crearMsal({ cuentas: [cuenta], retorno: throwError(() => error) });
    const authError = await inicializar(msal);

    expect(authError.error()).toEqual({
      code: 'invalid_grant',
      message: 'El código expiró',
      detail: undefined,
    });
    expect(consola).toHaveBeenCalled();
    expect(msal.setActiveAccount).toHaveBeenCalledWith(cuenta);
  });
});
