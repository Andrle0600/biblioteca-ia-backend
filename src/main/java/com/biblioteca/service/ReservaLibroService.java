package com.biblioteca.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.EstadoEjemplar;
import com.biblioteca.model.EstadoReservaLibro;
import com.biblioteca.model.Reserva;
import com.biblioteca.model.User;
import com.biblioteca.repository.EjemplarRepository;
import com.biblioteca.repository.ReservaLibroRepository;
import com.biblioteca.repository.UserRepository;

@Service
public class ReservaLibroService {

    @Autowired
    private ReservaLibroRepository reservaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EjemplarRepository ejemplarRepository;

    public List<Reserva> listarReservas() {
        return reservaRepository.findAll();
    }

    public Reserva guardarReserva(Reserva reserva) {
        return reservaRepository.save(reserva);
    }

    public Reserva crearReserva(Long usuarioId, Long ejemplarId, LocalDate fechaRecojo) {
    User usuario = userRepository.findById(usuarioId).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    Ejemplar ejemplar = ejemplarRepository.findById(ejemplarId).orElseThrow(() -> new RuntimeException("Ejemplar no encontrado"));

    ejemplar.setEstado(EstadoEjemplar.RESERVADO);
    ejemplarRepository.save(ejemplar);

    Reserva reserva = new Reserva();
    reserva.setUser(usuario);
    reserva.setEjemplar(ejemplar);
    LocalDateTime fechaInicio = fechaRecojo.atStartOfDay();
    reserva.setFechaReserva(fechaInicio);
    reserva.setFechaEstimadaDevolucion(fechaInicio.plusDays(14));
    reserva.setFechaRealDevolucion(null);
    reserva.setEstado(EstadoReservaLibro.PENDIENTE);

    return reservaRepository.save(reserva);
}

    public List<Reserva> listarReservasPendientes() {
        return reservaRepository.findByFechaRealDevolucionIsNull();
    }

    public List<Reserva> listarReservasRetrasadas() {
        return reservaRepository.buscarReservasRetrasadas();
    }

    public List<Reserva> listarReservasRecientes() {
        return reservaRepository.findTop10ByOrderByFechaReservaDesc();
    }

    public Optional<Reserva> pendientesPorCodigo(String codigo){
        return reservaRepository.findByEjemplarCodigoEjemplarAndEstadoIn(codigo, List.of(EstadoReservaLibro.PENDIENTE, EstadoReservaLibro.CON_RETRASO));
    }

    public Reserva devolverReserva(Reserva reservaActualizada) {
        reservaActualizada.setEstado(EstadoReservaLibro.DEVUELTO);
        Ejemplar ejemplarReserva=reservaActualizada.getEjemplar();
        ejemplarReserva.setEstado(EstadoEjemplar.DISPONIBLE);
        ejemplarRepository.save(ejemplarReserva);
        return reservaRepository.save(reservaActualizada);
    }
}
