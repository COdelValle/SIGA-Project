package cl.siga.coreshare.config;

import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;

/**
 * Estandariza las fechas que los clientes Feign envian como parametros de URL.
 *
 * <p>OpenFeign usa su propio {@code ConversionService} (no el de MVC), por lo
 * que sin esto enviaria el formato corto localizado (p. ej. {@code 6/10/26}) y
 * los servicios que esperan ISO 8601 ({@code yyyy-MM-dd}) responden 400. Se
 * registran fecha, hora y fecha-hora en ISO.</p>
 */
@AutoConfiguration(after = CommonDateFormatConfig.class)
@ConditionalOnClass(FeignFormatterRegistrar.class)
public class SharedFeignFormatConfig {

    @Bean
    @ConditionalOnMissingBean(FeignFormatterRegistrar.class)
    public FeignFormatterRegistrar feignIsoDateFormatterRegistrar() {
        return registry -> {
            DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
            registrar.setDateFormatter(CommonDateFormatConfig.DATE_FORMATTER);
            registrar.setTimeFormatter(DateTimeFormatter.ofPattern("HH:mm:ss"));
            registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            registrar.registerFormatters(registry);
        };
    }
}
