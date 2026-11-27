package com.biblioteca.persistencia.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.biblioteca.persistencia.entity.ChatInteraccion;
import com.biblioteca.persistencia.entity.User;

public interface ChatInteraccionRepository extends JpaRepository<ChatInteraccion, Long> {

    List<ChatInteraccion> findByUser(User user, Pageable pageable);

    List<ChatInteraccion> findByUserOrderByFechaHoraAsc(User user);
}
