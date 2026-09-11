import { AppConfig } from './app-config.model';

export async function loadAppConfig(url = '/config.json'): Promise<AppConfig> {
  const response = await fetch(url, { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`No se pudo cargar la configuracion de la app (HTTP ${response.status})`);
  }
  return (await response.json()) as AppConfig;
}
