package cl.siga.coreshare.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;

class SharedFeignFormatConfigTest {

    @Test
    void formateaFechasEnIsoParaFeign() {
        DefaultFormattingConversionService conversion = new DefaultFormattingConversionService();
        new SharedFeignFormatConfig().feignIsoDateFormatterRegistrar().registerFormatters(conversion);

        assertEquals("2026-10-06", conversion.convert(LocalDate.of(2026, 10, 6), String.class));
        assertEquals(LocalDate.of(2026, 10, 6), conversion.convert("2026-10-06", LocalDate.class));
        assertEquals("2026-10-06 15:30:00",
                conversion.convert(LocalDateTime.of(2026, 10, 6, 15, 30), String.class));
    }
}
