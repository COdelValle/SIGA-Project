package cl.siga.msreportes.config;

import cl.siga.coreshare.dto.kafka.EventoAsistenciaKafka;
import cl.siga.coreshare.dto.kafka.EventoRendimientoKafka;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

@Configuration
@Slf4j
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, EventoAsistenciaKafka> asistenciaConsumerFactory() {
        return consumerFactory(EventoAsistenciaKafka.class);
    }

    @Bean
    public ConsumerFactory<String, EventoRendimientoKafka> rendimientoConsumerFactory() {
        return consumerFactory(EventoRendimientoKafka.class);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventoAsistenciaKafka>
        asistenciaKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventoAsistenciaKafka> factory = newFactory(
            asistenciaConsumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventoRendimientoKafka>
        rendimientoKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EventoRendimientoKafka> factory = newFactory(
            rendimientoConsumerFactory());
        factory.setCommonErrorHandler(errorHandler());
        return factory;
    }

    private <T> ConsumerFactory<String, T> consumerFactory(Class<T> type) {
        JsonDeserializer<T> deserializer = new JsonDeserializer<>(type);
        deserializer.addTrustedPackages("cl.siga.coreshare.dto.kafka");
        deserializer.setUseTypeHeaders(false);
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(
            properties,
            new ErrorHandlingDeserializer<>(new StringDeserializer()),
            new ErrorHandlingDeserializer<>(deserializer));
    }

    private <K, V> ConcurrentKafkaListenerContainerFactory<K, V> newFactory(
            ConsumerFactory<K, V> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<K, V> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setObservationEnabled(false);
        return factory;
    }

    private DefaultErrorHandler errorHandler() {
        return new DefaultErrorHandler((record, error) ->
            log.error("Mensaje Kafka inválido o no procesable topic={} partición={} offset={}: {}",
                record.topic(), record.partition(), record.offset(), error.getMessage()),
            new FixedBackOff(1000L, 2L));
    }
}
