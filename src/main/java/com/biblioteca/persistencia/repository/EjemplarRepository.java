package com.biblioteca.persistencia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.biblioteca.persistencia.entity.Ejemplar;
import com.biblioteca.persistencia.entity.EstadoEjemplar;
import com.biblioteca.persistencia.entity.Libro;

public interface EjemplarRepository extends JpaRepository<Ejemplar, Long> {

    List<Ejemplar> findByLibro(Libro libro);

    List<Ejemplar> findByEstado(EstadoEjemplar estado);

    List<Ejemplar> findByLibroAndEstado(Libro libro, EstadoEjemplar estado);

    boolean existsByLibroAndEstado(Libro libro, EstadoEjemplar estado);
}
