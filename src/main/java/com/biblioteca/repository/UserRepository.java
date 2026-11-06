package com.biblioteca.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.biblioteca.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // El email es el username
    boolean existsByDni(String dni);
    boolean existsByUsername(String username);
    Optional<User> findByUsername(String username);
    List<User> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(String termino, String termino2);

    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.nombres) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "u.dni LIKE CONCAT('%', :q, '%')")
    List<User> buscarUsuarios(@Param("q") String q);
}
