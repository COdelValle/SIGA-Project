const MSAL_URL_PARAMS = [
  'state',
  'code',
  'error',
  'error_description',
  'session_state',
  'client_info',
  'ear_jwe',
];

/**
 * Quita de la URL los parametros de respuesta de MSAL (state, code, error, ...).
 * Se ejecuta en un APP_INITIALIZER, antes de que el router lea la URL, para que
 * la navegacion inicial parta de una URL limpia.
 */
export function limpiarParametrosMsal(): void {
  const url = new URL(window.location.href);
  let cambio = false;

  MSAL_URL_PARAMS.forEach((param) => {
    if (url.searchParams.has(param)) {
      url.searchParams.delete(param);
      cambio = true;
    }
  });

  const hash = url.hash.startsWith('#') ? url.hash.slice(1) : '';
  if (hash.includes('=')) {
    const hashParams = new URLSearchParams(hash);
    let hashCambio = false;
    MSAL_URL_PARAMS.forEach((param) => {
      if (hashParams.has(param)) {
        hashParams.delete(param);
        hashCambio = true;
      }
    });
    if (hashCambio) {
      const restante = hashParams.toString();
      url.hash = restante ? `#${restante}` : '';
      cambio = true;
    }
  }

  if (cambio) {
    window.history.replaceState({}, document.title, `${url.pathname}${url.search}${url.hash}`);
  }
}
