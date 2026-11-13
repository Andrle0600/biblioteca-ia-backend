package com.biblioteca.persistencia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.biblioteca.persistencia.entity.EstadoReservaLibro;
import com.biblioteca.persistencia.entity.Reserva;

public interface ReservaLibroRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByEstado(EstadoReservaLibro estado);

    List<Reserva> findTop10ByOrderByFechaReservaDesc();

    @Query("""
    SELECT r FROM Reserva r
    WHERE r.fechaEstimadaDevolucion < CURRENT_TIMESTAMP
    AND r.fechaRealDevolucion IS NULL
    AND r.estado = com.biblioteca.persistencia.entity.EstadoReservaLibro.PENDIENTE
    """)
    List<Reserva> buscarReservasRetrasadas();

    List<Reserva> findByFechaRealDevolucionIsNull();

    Optional<Reserva> findByEjemplarCodigoEjemplarAndEstadoIn(String codigo, List<EstadoReservaLibro> estados);


}
