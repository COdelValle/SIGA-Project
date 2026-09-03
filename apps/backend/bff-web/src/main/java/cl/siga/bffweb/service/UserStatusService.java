package cl.siga.bffweb.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
public class UserStatusService {

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;

    public UserStatusService(StringRedisTemplate redisTemplate, RestTemplate restTemplate) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
    }

    public String obtenerEstadoUsuario(String providerId) {
        String cacheKey = "user:status:" + providerId;

        // 1. Intentar leer desde Redis
        String estado = redisTemplate.opsForValue().get(cacheKey);

        if (estado != null) {
            return estado; // Hit de caché (0-1 ms)
        }

        // 2. Cache Miss: Consultar a ms-usuarios-auth
        try {
            String url = "http://ms-usuarios-auth/api/v1/usuarios/estado/" + providerId;
            estado = restTemplate.getForObject(url, String.class);
            if (estado == null) estado = "INEXISTENTE";
        } catch (Exception e) {
            // Si el servicio falla, fallback seguro
            estado = "ACTIVO"; 
        }

        // 3. Guardar en Redis con TTL de 10 minutos
        redisTemplate.opsForValue().set(cacheKey, estado, Duration.ofMinutes(10));

        return estado;
    }
}