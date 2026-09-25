package com.biblioteca.model;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"password", "enabled", "roles", "reservas"})
    private User user;

    @ManyToOne
    @JoinColumn(name = "ejemplar_id")
    @JsonIgnoreProperties({"reservas"}) 
    private Ejemplar ejemplar;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime fechaReserva;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime fechaEstimadaDevolucion = null;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime fechaRealDevolucion = null;

    @Enumerated(EnumType.STRING)
    private EstadoReservaLibro estado = EstadoReservaLibro.PENDIENTE;

    public Reserva() {
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

    public Ejemplar getEjemplar() {
        return ejemplar;
    }

    public void setEjemplar(Ejemplar ejemplar) {
        this.ejemplar = ejemplar;
    }

    public LocalDateTime getFechaReserva() {
        return fechaReserva;
    }

    public void setFechaReserva(LocalDateTime fechaReserva) {
        this.fechaReserva = fechaReserva;
    }

    public LocalDateTime getFechaEstimadaDevolucion() {
        return fechaEstimadaDevolucion;
    }

    public void setFechaEstimadaDevolucion(LocalDateTime fechaEstimadaDevolucion) {
        this.fechaEstimadaDevolucion = fechaEstimadaDevolucion;
    }

    public LocalDateTime getFechaRealDevolucion() {
        return fechaRealDevolucion;
    }

    public void setFechaRealDevolucion(LocalDateTime fechaRealDevolucion) {
        this.fechaRealDevolucion = fechaRealDevolucion;
    }

    public EstadoReservaLibro getEstado() {
        return estado;
    }

    public void setEstado(EstadoReservaLibro estado) {
        this.estado = estado;
    }

}
