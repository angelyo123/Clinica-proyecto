package com.AutomatizacionService.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SugerenciaCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "sugerencia:";

    public void guardarSugerencia(Long pacienteId, Map<String, Object> data) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(PREFIX + pacienteId, data, 30, TimeUnit.MINUTES); // TTL 30 min
        System.out.println("💾 Sugerencia temporal guardada en Redis: " + data);
    }

    public Map<String, Object> obtenerSugerencia(Long pacienteId) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return (Map<String, Object>) ops.get(PREFIX + pacienteId);
    }

    public void eliminarSugerencia(Long pacienteId) {
        redisTemplate.delete(PREFIX + pacienteId);
        System.out.println("🗑️ Sugerencia eliminada de Redis para paciente " + pacienteId);
    }

    @PostConstruct
    public void testRedis() {
        System.out.println("✅ RedisTemplate inyectado correctamente: " + (redisTemplate != null));
    }

}