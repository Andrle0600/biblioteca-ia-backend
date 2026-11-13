package com.biblioteca.persistencia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.biblioteca.persistencia.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long>{
    
    Optional<Role> findByName(String name); // debe retornar Optional<Role>
}
