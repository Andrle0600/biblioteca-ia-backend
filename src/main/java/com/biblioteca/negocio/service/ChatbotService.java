package com.biblioteca.negocio.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.biblioteca.persistencia.entity.ChatInteraccion;
import com.biblioteca.persistencia.entity.Libro;
import com.biblioteca.persistencia.entity.Reserva;
import com.biblioteca.persistencia.entity.User;
import com.biblioteca.persistencia.repository.ChatInteraccionRepository;
import com.biblioteca.persistencia.repository.LibroRepository;
import com.biblioteca.persistencia.repository.ReservaLibroRepository;
import com.biblioteca.persistencia.vector.LibroVectorStore;
import com.biblioteca.persistencia.vector.PerfilVectorStore;
import com.biblioteca.persistencia.vector.VectorUtils;
import com.biblioteca.presentacion.dto.ChatResponse;
import com.biblioteca.presentacion.dto.LibroSugeridoDto;

/**
 * Chatbot de recomendación de libros.
 * Flujo: embedding del mensaje + perfil vectorial del usuario (historial de reservas)
 * -> búsqueda por similitud coseno en libro_vector (pgvector)
 * -> respuesta generada por Gemini con los candidatos como contexto
 * -> persistencia de la conversación en chat_interaccion.
 */
@Service
public class ChatbotService {

    private static final int MENSAJES_HISTORIAL_CHAT = 12;

    @Autowired
    private ChatInteraccionRepository chatRepository;

    @Autowired
    private ReservaLibroRepository reservaRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private LibroVectorStore libroVectorStore;

    @Autowired
    private PerfilVectorStore perfilVectorStore;

    @Autowired
    private GeminiService geminiService;

    /** Peso del mensaje actual frente al perfil del usuario al combinar vectores (0 a 1). */
    @Value("${chatbot.peso.mensaje:0.6}")
    private double pesoMensaje;

    /** Cantidad de libros candidatos que se recuperan por similitud. */
    @Value("${chatbot.candidatos:6}")
    private int numCandidatos;

    /**
     * Procesa un mensaje del usuario y devuelve la respuesta del chatbot
     * junto con los libros sugeridos.
     */
    public ChatResponse conversar(User usuario, String mensaje) {
        // Historial previo (sin incluir el mensaje nuevo), en orden cronológico
        List<ChatInteraccion> recientes = obtenerHistorial(usuario, MENSAJES_HISTORIAL_CHAT);

        // Libros que el usuario ya reservó: se excluyen de las sugerencias
        List<Reserva> reservas = reservaRepository.findByUser(usuario);
        Set<Long> librosYaReservados = new LinkedHashSet<>();
        List<String> titulosLeidos = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (reserva.getEjemplar() != null && reserva.getEjemplar().getLibro() != null) {
                Libro libro = reserva.getEjemplar().getLibro();
                if (librosYaReservados.add(libro.getId())) {
                    titulosLeidos.add(libro.getTitulo());
                }
            }
        }

        // Se guarda el mensaje del usuario
        chatRepository.save(new ChatInteraccion(usuario, ChatInteraccion.ROL_USUARIO, mensaje));

        // Embedding del mensaje, combinado con el perfil vectorial del usuario
        float[] vectorMensaje = geminiService.generarEmbedding(mensaje, GeminiService.TASK_RETRIEVAL_QUERY);
        float[] perfil = perfilVectorStore.obtenerPorUsuario(usuario.getId());
        float[] consulta = (perfil != null)
                ? VectorUtils.combinar(vectorMensaje, perfil, pesoMensaje)
                : vectorMensaje;

        // Candidatos: similares al mensaje+perfil, no leídos y con ejemplares disponibles
        // (con degradación progresiva si no hay resultados)
        List<LibroVectorStore.LibroSimilar> candidatos = libroVectorStore.buscarSimilares(
                consulta, numCandidatos, new ArrayList<>(librosYaReservados), true);
        if (candidatos.isEmpty()) {
            candidatos = libroVectorStore.buscarSimilares(
                    consulta, numCandidatos, new ArrayList<>(librosYaReservados), false);
        }
        if (candidatos.isEmpty()) {
            candidatos = libroVectorStore.buscarSimilares(consulta, numCandidatos, List.of(), false);
        }

        // Instrucción de sistema con el catálogo de candidatos y el historial de lecturas
        String sistema = construirInstruccionSistema(candidatos, titulosLeidos);

        // Historial para Gemini (roles user/model) + mensaje nuevo.
        // Solo se incluyen intercambios completos (usuario -> asistente): los mensajes
        // de usuario sin respuesta del asistente se omiten para no confundir al modelo
        // con roles consecutivos iguales.
        List<GeminiService.MensajeChat> historialGemini = new ArrayList<>();
        for (int i = 0; i < recientes.size(); i++) {
            ChatInteraccion actual = recientes.get(i);
            if (ChatInteraccion.ROL_USUARIO.equals(actual.getRolMensaje())
                    && i + 1 < recientes.size()
                    && ChatInteraccion.ROL_ASISTENTE.equals(recientes.get(i + 1).getRolMensaje())) {
                historialGemini.add(new GeminiService.MensajeChat("user", actual.getContenido()));
                historialGemini.add(new GeminiService.MensajeChat("model", recientes.get(i + 1).getContenido()));
            }
        }
        historialGemini.add(new GeminiService.MensajeChat("user", mensaje));

        String respuesta = geminiService.generarRespuestaChat(sistema, historialGemini);

        // Se guarda la respuesta del asistente
        chatRepository.save(new ChatInteraccion(usuario, ChatInteraccion.ROL_ASISTENTE, respuesta));

        List<LibroSugeridoDto> sugeridos = new ArrayList<>();
        for (LibroVectorStore.LibroSimilar candidato : candidatos) {
            sugeridos.add(new LibroSugeridoDto(candidato.id(), candidato.titulo(), candidato.autor(),
                    candidato.genero(), candidato.similitud()));
        }
        return new ChatResponse(respuesta, sugeridos);
    }

    /** Devuelve los últimos mensajes del chat del usuario, en orden cronológico. */
    public List<ChatInteraccion> obtenerHistorial(User usuario, int limite) {
        Pageable pageable = PageRequest.of(0, limite, Sort.by(Sort.Direction.DESC, "fechaHora"));
        List<ChatInteraccion> recientes = chatRepository.findByUser(usuario, pageable);
        List<ChatInteraccion> cronologico = new ArrayList<>(recientes);
        Collections.reverse(cronologico);
        return cronologico;
    }

    /**
     * Regenera los embeddings de todos los libros del catálogo (solo ADMIN).
     * Devuelve la cantidad de libros actualizados.
     */
    public int regenerarEmbeddingsLibros() {
        List<Libro> libros = libroRepository.findAll();
        int actualizados = 0;
        for (Libro libro : libros) {
            String texto = textoParaEmbedding(libro);
            if (texto.isBlank()) {
                continue;
            }
            float[] embedding = geminiService.generarEmbedding(texto, GeminiService.TASK_RETRIEVAL_DOCUMENT);
            libroVectorStore.guardar(libro.getId(), embedding);
            actualizados++;
        }
        return actualizados;
    }

    private String construirInstruccionSistema(List<LibroVectorStore.LibroSimilar> candidatos,
                                               List<String> titulosLeidos) {
        StringBuilder sb = new StringBuilder();
        sb.append("Eres BiblioAI, el asistente virtual de la biblioteca. Ayudas a los usuarios a encontrar su próximo libro.\n\n");
        sb.append("Reglas:\n");
        sb.append("- Responde siempre en español, con tono amable y en máximo 3 párrafos.\n");
        sb.append("- Recomienda ÚNICAMENTE libros de la lista de candidatos. Nunca inventes títulos ni autores.\n");
        sb.append("- Menciona el título y el autor, y explica brevemente por qué encaja con lo que pide el usuario y su historial.\n");
        sb.append("- Si ningún candidato encaja, dilo con honestidad y sugiere el más cercano de la lista.\n");

        sb.append("\nHistorial de lecturas del usuario: ");
        sb.append(titulosLeidos.isEmpty() ? "sin reservas previas" : String.join(", ", titulosLeidos));
        sb.append(".\n");

        sb.append("\nCatálogo de candidatos (id | título | autor | género | descripción):\n");
        for (LibroVectorStore.LibroSimilar candidato : candidatos) {
            sb.append("- [").append(candidato.id()).append("] ").append(candidato.titulo())
                    .append(" | ").append(candidato.autor() == null ? "autor desconocido" : candidato.autor())
                    .append(" | ").append(candidato.genero() == null ? "" : candidato.genero())
                    .append(" | ").append(candidato.descripcion() == null ? "" : recortar(candidato.descripcion(), 220))
                    .append('\n');
        }
        return sb.toString();
    }

    /** Texto que representa al libro para generar su embedding. */
    private String textoParaEmbedding(Libro libro) {
        StringBuilder sb = new StringBuilder();
        if (libro.getTitulo() != null) {
            sb.append(libro.getTitulo()).append(". ");
        }
        if (libro.getAutor() != null) {
            sb.append("Autor: ").append(libro.getAutor()).append(". ");
        }
        if (libro.getGenero() != null) {
            sb.append("Género: ").append(libro.getGenero()).append(". ");
        }
        if (libro.getEditorial() != null) {
            sb.append("Editorial: ").append(libro.getEditorial()).append(". ");
        }
        if (libro.getDescripcion() != null) {
            sb.append(libro.getDescripcion());
        }
        return sb.toString().trim();
    }

    private String recortar(String texto, int max) {
        if (texto == null || texto.length() <= max) {
            return texto == null ? "" : texto;
        }
        return texto.substring(0, max) + "...";
    }
}
