package com.biblioteca.presentacion.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.biblioteca.negocio.service.ChatbotService;
import com.biblioteca.persistencia.entity.ChatInteraccion;
import com.biblioteca.persistencia.entity.User;
import com.biblioteca.persistencia.repository.UserRepository;
import com.biblioteca.presentacion.dto.ChatRequest;
import com.biblioteca.presentacion.dto.ChatResponse;

@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200", "https://andrle0600.github.io"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private UserRepository userRepository;

    /** Envía un mensaje al chatbot y devuelve su respuesta con los libros sugeridos. */
    @PostMapping("/mensaje")
    public ResponseEntity<?> enviarMensaje(@RequestBody ChatRequest request, Authentication authentication) {
        try {
            User usuario = usuarioActual(authentication);
            String mensaje = request.getMensaje() == null ? "" : request.getMensaje().trim();
            if (mensaje.isBlank()) {
                return ResponseEntity.badRequest().body("El mensaje no puede estar vacío");
            }
            ChatResponse respuesta = chatbotService.conversar(usuario, mensaje);
            return ResponseEntity.ok(respuesta);
        } catch (IllegalStateException e) {
            // Errores de la API de Gemini (configuración, HTTP o respuesta vacía)
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error del asistente de IA: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error procesando el mensaje: " + e.getMessage());
        }
    }

    /** Historial de chat del usuario autenticado. */
    @GetMapping("/historial")
    public ResponseEntity<?> historial(Authentication authentication,
                                        @RequestParam(name = "limite", defaultValue = "30") int limite) {
        try {
            User usuario = usuarioActual(authentication);
            List<ChatInteraccion> historial = chatbotService.obtenerHistorial(usuario, Math.min(Math.max(limite, 1), 100));
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error obteniendo el historial: " + e.getMessage());
        }
    }

    /** Regenera los embeddings de todos los libros del catálogo (solo ADMIN). */
    @PostMapping("/embeddings/regenerar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> regenerarEmbeddings() {
        try {
            int actualizados = chatbotService.regenerarEmbeddingsLibros();
            return ResponseEntity.ok("Embeddings regenerados: " + actualizados + " libros");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Error regenerando embeddings: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error regenerando embeddings: " + e.getMessage());
        }
    }

    private User usuarioActual(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }
}
