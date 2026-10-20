# EP1 — Evidencia de PKCE

Fecha: 2026-10-20

Release desplegado: `2104a2668d34277a3c021046c4d1d07f3f36e135`

Tenant: `f260a804-82ac-4b57-bb01-9ed6626d71ff`

## Qué se debe demostrar

MSAL solicita `Authorization Code` con PKCE. La petición `authorize` debe mostrar:

- `response_type=code`
- `code_challenge` no vacío
- `code_challenge_method=S256`
- `redirect_uri` de SIGA
- `state` y `nonce` presentes (se pueden tapar sus valores)

No se debe mostrar el `code_verifier`, el código de autorización, tokens, cookies ni contraseñas.

## Pasos

1. Abrir una ventana **InPrivate/Incógnito** para forzar un login nuevo.
2. Abrir DevTools con `F12` → pestaña **Network**.
3. Marcar **Preserve log** y limpiar la lista.
4. Ir a `https://87hjovxzud.execute-api.us-east-1.amazonaws.com/` e iniciar sesión.
5. En Network, seleccionar la petición cuya URL empiece por:
   `https://login.microsoftonline.com/f260a804-82ac-4b57-bb01-9ed6626d71ff/oauth2/v2.0/authorize`
6. Abrir **Headers** → **Query String Parameters**.
7. Verificar los parámetros del punto anterior.
8. Tomar captura y tapar con negro: `state`, `nonce`, `code`, tokens y cualquier credencial. Dejar visibles `response_type`, `code_challenge` (o al menos su presencia) y `code_challenge_method=S256`.
9. Guardar como `docs/evidencias/EP1-PRES-PKCE/authorize.png`.

## Si no aparece la petición

- Confirmar que `Preserve log` está activo.
- Borrar los datos del sitio o usar otra ventana InPrivate.
- En el filtro de Network elegir **All**, no solo Fetch/XHR; `authorize` puede figurar como documento.
- No es necesario capturar el intercambio del token; la petición `authorize` con `code_challenge` y `S256` es la evidencia de PKCE.

## Registro

- Fecha y hora: 2026-10-20 01:10 (UTC-3)
- Navegador: Google Chrome 155, ventana InPrivate
- Commit: `2104a2668d34277a3c021046c4d1d07f3f36e135`
- Archivo: `authorize.png`
- Resultado: **OK**. La petición muestra `response_type=code`, `code_challenge=1Mhx…` y `code_challenge_method=S256`; `state`, `nonce` y `client-request-id` están tapados.
- Observaciones: el `scope` visible es del login OIDC (`openid profile offline_access`); los scopes de la API se validaron por separado con el token real de 43 scopes. No se incluyeron cookies, tokens ni `code_verifier`.
