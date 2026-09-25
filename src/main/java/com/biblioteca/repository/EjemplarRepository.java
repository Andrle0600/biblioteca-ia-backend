package com.biblioteca.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.biblioteca.model.Ejemplar;
import com.biblioteca.model.EstadoEjemplar;
import com.biblioteca.model.Libro;

public interface EjemplarRepository extends JpaRepository<Ejemplar, Long> {

    List<Ejemplar> findByLibro(Libro libro);

    List<Ejemplar> findByEstado(EstadoEjemplar estado);

    List<Ejemplar> findByLibroAndEstado(Libro libro, EstadoEjemplar estado);

    boolean existsByLibroAndEstado(Libro libro, EstadoEjemplar estado);
}
