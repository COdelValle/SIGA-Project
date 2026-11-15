import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '@siga/core';
import { afterEach, describe, expect, it } from 'vitest';
import { ApoderadoService } from './apoderado.service';

function configurar(useMocks: boolean): void {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      {
        provide: APP_CONFIG,
        useValue: {
          bffBaseUrl: '/api',
          useMocks,
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
}

describe('ApoderadoService', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('en modo demo devuelve null sin llamar al BFF', () => {
    configurar(true);
    const service = TestBed.inject(ApoderadoService);
    const http = TestBed.inject(HttpTestingController);

    let pupilos: unknown = 'pendiente';
    service.getPupilos().subscribe((valor) => (pupilos = valor));

    expect(pupilos).toBeNull();
    http.expectNone('/api/bff/v1/apoderados/pupilos');
  });

  it('mapea la relación y el curso de cada pupilo', () => {
    configurar(false);
    const service = TestBed.inject(ApoderadoService);
    const http = TestBed.inject(HttpTestingController);

    let pupilos: unknown;
    service.getPupilos().subscribe((valor) => (pupilos = valor));

    const req = http.expectOne('/api/bff/v1/apoderados/pupilos');
    expect(req.request.method).toBe('GET');
    req.flush([
      { id: 1, nombre: 'Camila Soto', relacion: 'MADRE_PADRE', curso: '8vo Básico A' },
      { id: 2, nombre: 'Lilith Soto', relacion: 'TUTOR_LEGAL', curso: null },
      { id: 3, nombre: 'Ana Soto', relacion: 'TIA_TIO', curso: '7mo B' },
      { id: 4, nombre: 'Bruno Soto', relacion: 'ABUELA_ABUELO', curso: '1ro Medio' },
      { id: 5, nombre: 'Carla Soto', relacion: 'HERMANA_HERMANO', curso: '2do Medio' },
      { id: 6, nombre: 'Diego Soto', relacion: 'PRIMO_PRIMA', curso: '3ro Medio' },
      { id: 7, nombre: 'Elena Soto', relacion: null, curso: '4to Medio' },
      { id: 8, nombre: 'Felipe Soto', relacion: 'OTRO', curso: '5to Básico' },
    ]);

    expect(pupilos).toEqual([
      { id: 1, nombre: 'Camila Soto', relacion: 'Hija', curso: '8vo Básico A' },
      { id: 2, nombre: 'Lilith Soto', relacion: 'Pupilo(a)', curso: '' },
      { id: 3, nombre: 'Ana Soto', relacion: 'Sobrino(a)', curso: '7mo B' },
      { id: 4, nombre: 'Bruno Soto', relacion: 'Nieto(a)', curso: '1ro Medio' },
      { id: 5, nombre: 'Carla Soto', relacion: 'Hermano(a)', curso: '2do Medio' },
      { id: 6, nombre: 'Diego Soto', relacion: 'Primo(a)', curso: '3ro Medio' },
      { id: 7, nombre: 'Elena Soto', relacion: '', curso: '4to Medio' },
      { id: 8, nombre: 'Felipe Soto', relacion: 'OTRO', curso: '5to Básico' },
    ]);
    http.verify();
  });

  it('reutiliza el cache y lo invalida para volver a consultar', () => {
    configurar(false);
    const service = TestBed.inject(ApoderadoService);
    const http = TestBed.inject(HttpTestingController);

    const pupilos$ = service.getPupilos();
    pupilos$.subscribe();
    pupilos$.subscribe();
    http.expectOne('/api/bff/v1/apoderados/pupilos').flush([]);

    service.invalidar();
    service.getPupilos().subscribe();
    http.expectOne('/api/bff/v1/apoderados/pupilos').flush([]);
    http.verify();
  });

  it('propaga el error del BFF sin usar datos mock', () => {
    configurar(false);
    const service = TestBed.inject(ApoderadoService);
    const http = TestBed.inject(HttpTestingController);

    let fallo = false;
    service.getPupilos().subscribe({ error: () => (fallo = true) });

    http
      .expectOne('/api/bff/v1/apoderados/pupilos')
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(fallo).toBe(true);
  });
});
