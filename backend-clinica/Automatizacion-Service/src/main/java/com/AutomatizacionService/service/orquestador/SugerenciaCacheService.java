package com.AutomatizacionService.service.orquestador;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SugerenciaCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PREFIX = "sugerencia:";
    private static final String CONTEXTO = "contexto:";
    private static final String DATOS = "datos:";

    @PostConstruct
    public void testRedis() {
        System.out.println("✅ RedisTemplate inyectado correctamente: " + (redisTemplate != null));
    }

    // 🧩 --- BLOQUE PRINCIPAL DE SUGERENCIAS TEMPORALES ---
    public void guardarSugerencia(Long pacienteId, Map<String, Object> data) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(PREFIX + pacienteId, data, 30, TimeUnit.MINUTES);
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

    // 💬 --- CONTEXTO DE CONVERSACIÓN ---
    public void agregarContextoConversacion(Long pacienteId, String mensaje) {
        String key = CONTEXTO + pacienteId;
        redisTemplate.opsForList().rightPush(key, mensaje);
        redisTemplate.expire(key, 1, TimeUnit.HOURS);
    }

    public String obtenerContextoConversacion(Long pacienteId) {
        String key = CONTEXTO + pacienteId;
        List<Object> mensajes = redisTemplate.opsForList().range(key, 0, -1);
        if (mensajes == null || mensajes.isEmpty()) return "";
        return String.join("\n", mensajes.stream().map(Object::toString).toList());
    }

    // 🧠 --- NUEVO BLOQUE: DATOS CONTEXTUALES ---
    /** Guarda datos clave del contexto (por ejemplo especialidad o médico detectado) **/
    public void guardarDatoContexto(Long pacienteId, String clave, Object valor) {
        String key = DATOS + pacienteId;
        Map<String, Object> datos = (Map<String, Object>) redisTemplate.opsForValue().get(key);
        if (datos == null) datos = new HashMap<>();

        datos.put(clave, valor);
        redisTemplate.opsForValue().set(key, datos, 1, TimeUnit.HOURS);
        System.out.println("🧩 Contexto guardado [" + clave + "=" + valor + "] para paciente " + pacienteId);
    }

    /** Recupera un dato específico del contexto **/
    public Object obtenerDatoContexto(Long pacienteId, String clave) {
        String key = DATOS + pacienteId;
        Map<String, Object> datos = (Map<String, Object>) redisTemplate.opsForValue().get(key);
        return datos != null ? datos.get(clave) : null;
    }

    /** Limpia todo el contexto del paciente **/
    public void limpiarContexto(Long pacienteId) {
        redisTemplate.delete(DATOS + pacienteId);
        redisTemplate.delete(CONTEXTO + pacienteId);
        System.out.println("🧹 Contexto completo eliminado para paciente " + pacienteId);
    }

    /** Elimina un dato específico del contexto del paciente **/
    public void eliminarDatoContexto(Long pacienteId, String clave) {
        String key = DATOS + pacienteId;
        Map<String, Object> datos = (Map<String, Object>) redisTemplate.opsForValue().get(key);
        if (datos != null && datos.containsKey(clave)) {
            datos.remove(clave);
            redisTemplate.opsForValue().set(key, datos, 1, TimeUnit.HOURS);
            System.out.println("🗑️ Dato eliminado [" + clave + "] del contexto del paciente " + pacienteId);
        }
    }

}