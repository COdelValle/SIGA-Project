/** Copia texto al portapapeles; devuelve false si el navegador no lo permite. */
export const copiarAlPortapapeles = async (texto: string): Promise<boolean> => {
  if (!texto || !navigator.clipboard) {
    return false;
  }
  try {
    await navigator.clipboard.writeText(texto);
    return true;
  } catch {
    return false;
  }
};
