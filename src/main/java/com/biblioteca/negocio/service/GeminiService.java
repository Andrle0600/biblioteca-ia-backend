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
 * Cliente de la API de Google Gemini (Generative Language API).
 * Se usa RestClient (nativo de Spring) para no agregar dependencias extra.
 */
@Service
public class GeminiService {

    public static final String TASK_RETRIEVAL_DOCUMENT = "RETRIEVAL_DOCUMENT";
    public static final String TASK_RETRIEVAL_QUERY = "RETRIEVAL_QUERY";

    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.chat.model:gemini-2.5-flash}")
    private String modeloChat;

    @Value("${gemini.embedding.model:gemini-embedding-001}")
    private String modeloEmbedding;

    @Value("${gemini.embedding.dimensions:1536}")
    private int dimensionesEmbedding;

    /** Presupuesto de "thinking" del modelo de chat. 0 = desactivado (más rápido y sin truncados). Usa -1 para omitir la configuración. */
    @Value("${gemini.thinking.budget:0}")
    private int thinkingBudget;

    /** Intentos ante errores transitorios de la API (503 alta demanda / 429 cuota). */
    @Value("${gemini.reintentos:3}")
    private int reintentos;

    private final RestClient restClient;

    public GeminiService() {
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    /** Mensaje de chat con rol de Gemini ("user" o "model"). */
    public record MensajeChat(String rol, String texto) {
    }

    /** Genera el embedding (vector) de un texto con el modelo de embeddings de Gemini. */
    public float[] generarEmbedding(String texto, String taskType) {
        validarApiKey();

        Map<String, Object> body = new HashMap<>();
        body.put("content", Map.of("parts", List.of(Map.of("text", texto))));
        if (taskType != null && !taskType.isBlank()) {
            body.put("taskType", taskType);
        }
        if (dimensionesEmbedding > 0) {
            body.put("outputDimensionality", dimensionesEmbedding);
        }

        JsonNode respuesta = postConReintentos("/models/" + modeloEmbedding + ":embedContent", body, "embeddings");

        if (respuesta == null || respuesta.path("embedding").path("values").isMissingNode()) {
            throw new IllegalStateException("La respuesta de embeddings de Gemini no contiene valores");
        }
        JsonNode valores = respuesta.path("embedding").path("values");
        float[] vector = new float[valores.size()];
        for (int i = 0; i < valores.size(); i++) {
            vector[i] = (float) valores.get(i).asDouble();
        }
        return vector;
    }

    /**
     * Genera una respuesta de chat con el modelo de Gemini.
     * El último mensaje del historial debe ser el mensaje nuevo del usuario.
     */
    public String generarRespuestaChat(String instruccionSistema, List<MensajeChat> historial) {
        validarApiKey();

        List<Map<String, Object>> contenidos = new ArrayList<>();
        for (MensajeChat mensaje : historial) {
            contenidos.add(Map.of(
                    "role", mensaje.rol(),
                    "parts", List.of(Map.of("text", mensaje.texto()))));
        }

        Map<String, Object> body = new HashMap<>();
        if (instruccionSistema != null && !instruccionSistema.isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", instruccionSistema))));
        }
        body.put("contents", contenidos);
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 1024);
        if (thinkingBudget >= 0) {
            // Los modelos "thinking" gastan tokens de razonamiento en el mismo limite:
            // con budget 0 responden mas rapido y sin respuestas truncadas.
            generationConfig.put("thinkingConfig", Map.of("thinkingBudget", thinkingBudget));
        }
        body.put("generationConfig", generationConfig);

        JsonNode respuesta = postConReintentos("/models/" + modeloChat + ":generateContent", body, "chat");

        if (respuesta == null || respuesta.path("candidates").isMissingNode() || respuesta.path("candidates").isEmpty()) {
            String motivo = respuesta == null
                    ? "sin respuesta"
                    : respuesta.path("promptFeedback").path("blockReason").asText("sin candidatos");
            throw new IllegalStateException("Gemini no generó respuesta (" + motivo + ")");
        }
        JsonNode partes = respuesta.path("candidates").get(0).path("content").path("parts");
        StringBuilder texto = new StringBuilder();
        if (partes.isArray()) {
            for (JsonNode parte : partes) {
                texto.append(parte.path("text").asText(""));
            }
        }
        if (texto.toString().isBlank()) {
            throw new IllegalStateException("Gemini generó una respuesta vacía");
        }
        return texto.toString().trim();
    }

    /**
     * Realiza un POST a la API de Gemini con reintentos automáticos ante errores
     * transitorios (503 alta demanda, 429 cuota) con espera progresiva entre intentos.
     */
    private JsonNode postConReintentos(String uri, Map<String, Object> body, String operacion) {
        RestClientResponseException ultimoError = null;
        for (int intento = 1; intento <= reintentos; intento++) {
            try {
                return restClient.post()
                        .uri(uri)
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (RestClientResponseException e) {
                int codigo = e.getStatusCode().value();
                if ((codigo == 503 || codigo == 429) && intento < reintentos) {
                    try {
                        Thread.sleep(2000L * intento);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException("Llamada a Gemini interrumpida", ie);
                    }
                    continue;
                }
                throw new IllegalStateException("Error llamando a Gemini " + operacion + " (HTTP "
                        + codigo + "): " + recortar(e.getResponseBodyAsString(), 300));
            }
        }
        throw new IllegalStateException("Error llamando a Gemini " + operacion
                + " tras " + reintentos + " intentos");
    }

    private void validarApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Falta la API key de Gemini. Configura la variable de entorno GEMINI_API_KEY "
                            + "(o la propiedad gemini.api-key).");
        }
    }

    private String recortar(String texto, int max) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= max ? texto : texto.substring(0, max) + "...";
    }
}
