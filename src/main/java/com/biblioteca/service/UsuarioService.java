package com.biblioteca.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.biblioteca.model.User;
import com.biblioteca.repository.UserRepository;

@Service
public class UsuarioService {

    @Autowired
    private UserRepository userRepository;

    public List<User> listarUsuarios() {
        return userRepository.findAll();
    }

    public Optional<User> buscarPorId(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public Optional<User> cambiarEstado(Long id, boolean enabled) {
        return userRepository.findById(id).map(user -> {
            user.setEnabled(enabled);
            return userRepository.save(user);
        });
    }

    public List<User> buscarUsuarios(String termino) {
        if (termino == null || termino.trim().isEmpty()) {
            return listarUsuarios();
        }
        return userRepository.buscarUsuarios(termino.trim());
    }
}
