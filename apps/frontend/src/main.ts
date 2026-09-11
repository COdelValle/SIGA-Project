import { bootstrapApplication } from '@angular/platform-browser';
import { loadAppConfig } from '@siga/core';
import { App } from './app/app';
import { appConfig } from './app/app.config';

loadAppConfig()
  .then((config) => bootstrapApplication(App, appConfig(config)))
  .catch((error) => {
    console.error('No se pudo iniciar la aplicacion', error);
    document.body.innerHTML =
      '<p style="font-family:sans-serif;padding:2rem">No se pudo cargar la configuracion de la aplicacion.</p>';
  });
