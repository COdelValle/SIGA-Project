package cl.siga.coreshare.format;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class RutNormalizerTest {

    @Test
    void quitaPuntosYMantieneGuion() {
        assertEquals("13789943-2", RutNormalizer.normalizar("13.789.943-2"));
    }

    @Test
    void agregaGuionCuandoFalta() {
        assertEquals("13789943-2", RutNormalizer.normalizar("137899432"));
    }

    @Test
    void pasaLaKAMayuscula() {
        assertEquals("12345678-K", RutNormalizer.normalizar("12.345.678-k"));
        assertEquals("12345678-K", RutNormalizer.normalizar("12345678k"));
    }

    @Test
    void soportaNullYVacio() {
        assertNull(RutNormalizer.normalizar(null));
        assertEquals("", RutNormalizer.normalizar("   "));
    }
}
