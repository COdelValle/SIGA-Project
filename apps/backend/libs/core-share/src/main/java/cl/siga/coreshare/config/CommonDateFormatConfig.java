package cl.siga.coreshare.config;

import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

@AutoConfiguration 
public class CommonDateFormatConfig implements WebMvcConfigurer {

    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    // 1. Aplica el formato a JSON (@RequestBody / @ResponseBody - Jackson)
    @Bean 
    public Jackson2ObjectMapperBuilderCustomizer jsonDateCustomizer() {
        return builder -> {
            builder.serializers(new LocalDateSerializer(DATE_FORMATTER));
            builder.deserializers(new LocalDateDeserializer(DATE_FORMATTER));
        };
    }

    // 2. Aplica el formato a parámetros de URL (@RequestParam / @PathVariable)
    @Override
    public void addFormatters(FormatterRegistry registry) {
        DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
        registrar.setDateFormatter(DATE_FORMATTER);
        registrar.registerFormatters(registry);
    }
}