package com.biblioteca.persistencia.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Mensaje de una conversación entre el usuario y el chatbot de IA.
 * Mapea la tabla chat_interaccion (rol_mensaje: 'usuario' | 'asistente_llm').
 */
@Entity
@Table(name = "chat_interaccion")
public class ChatInteraccion {

    public static final String ROL_USUARIO = "usuario";
    public static final String ROL_ASISTENTE = "asistente_llm";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"password", "enabled", "roles", "reservas"})
    private User user;

    @Column(name = "rol_mensaje", nullable = false)
    private String rolMensaje;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    public ChatInteraccion() {
    }

    public ChatInteraccion(User user, String rolMensaje, String contenido) {
        this.user = user;
        this.rolMensaje = rolMensaje;
        this.contenido = contenido;
        this.fechaHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getRolMensaje() {
        return rolMensaje;
    }

    public void setRolMensaje(String rolMensaje) {
        this.rolMensaje = rolMensaje;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}
