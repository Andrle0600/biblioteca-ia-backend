package com.biblioteca.negocio.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.JsonNode;

/**
 * Servicio de chat con un LLM a traves de una API compatible con OpenAI
 * (DeepSeek, GLM en Huawei ModelArts, OpenRouter, etc.).
 * La URL base y el modelo se configuran en application.properties o con variables
 * de entorno (LLM_BASE_URL, LLM_CHAT_MODEL). La API key va en LLM_API_KEY.
 *
 * Los embeddings de las recomendaciones se generan con Gemini (ver GeminiService),
 * ya que estos servicios no ofrecen API de embeddings.
 */
@Service
public class LlmChatService {

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.base-url:https://api.deepseek.com}")
    private String baseUrl;

    @Value("${llm.chat.model:deepseek-chat}")
    private String modeloChat;

    /** Intentos ante errores transitorios de la API (429 cuota / 502 / 503). */
    @Value("${llm.reintentos:3}")
    private int reintentos;

    private final RestClient restClient;

    public LlmChatService() {
        this.restClient = RestClient.builder().build();
    }

    /** Mensaje de chat: rol "user" o "model" (se mapea a "assistant"). */
    public record MensajeChat(String rol, String texto) {
    }

    /**
     * Genera una respuesta de chat con el LLM configurado.
     * El último mensaje del historial debe ser el mensaje nuevo del usuario.
     */
    public String generarRespuestaChat(String instruccionSistema, List<MensajeChat> historial) {
        validarApiKey();

        List<Map<String, String>> mensajes = new ArrayList<>();
        if (instruccionSistema != null && !instruccionSistema.isBlank()) {
            mensajes.add(Map.of("role", "system", "content", instruccionSistema));
        }
        for (MensajeChat mensaje : historial) {
            String rol = "model".equals(mensaje.rol()) ? "assistant" : mensaje.rol();
            mensajes.add(Map.of("role", rol, "content", mensaje.texto()));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", modeloChat);
        body.put("messages", mensajes);
        body.put("temperature", 0.7);
        // Margen amplio: algunos modelos (p. ej. glm) gastan tokens en razonamiento
        body.put("max_tokens", 2048);

        JsonNode respuesta = postConReintentos(body);

        JsonNode choices = respuesta == null ? null : respuesta.path("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty() || !choices.has(0)) {
            throw new IllegalStateException("El LLM no generó respuesta");
        }
        String texto = choices.get(0).path("message").path("content").asText(null);
        if (texto == null || texto.isBlank()) {
            throw new IllegalStateException("El LLM generó una respuesta vacía");
        }
        return texto.trim();
    }

    private JsonNode postConReintentos(Map<String, Object> body) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String uri = base + "/chat/completions";

        for (int intento = 1; intento <= reintentos; intento++) {
            try {
                return restClient.post()
                        .uri(uri)
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (RestClientResponseException e) {
                int codigo = e.getStatusCode().value();
                if ((codigo == 429 || codigo == 502 || codigo == 503) && intento < reintentos) {
                    try {
                        Thread.sleep(2000L * intento);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Llamada al LLM interrumpida", ie);
                    }
                    continue;
                }
                throw new IllegalStateException("Error llamando al LLM (HTTP "
                        + codigo + "): " + recortar(e.getResponseBodyAsString(), 300));
            }
        }
        throw new IllegalStateException("Error llamando al LLM tras " + reintentos + " intentos");
    }

    private void validarApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Falta la API key del LLM. Configura la variable de entorno LLM_API_KEY "
                            + "(o la propiedad llm.api-key).");
        }
    }

    private String recortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max) + "...";
    }
}
