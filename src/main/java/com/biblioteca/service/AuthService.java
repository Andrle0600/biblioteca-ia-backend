package com.biblioteca.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.biblioteca.dto.AuthResponse;
import com.biblioteca.dto.LoginRequest;
import com.biblioteca.dto.RegisterRequest;
import com.biblioteca.dto.UserResponse;
import com.biblioteca.model.Role;
import com.biblioteca.model.User;
import com.biblioteca.repository.RoleRepository;
import com.biblioteca.repository.UserRepository;
import com.biblioteca.security.JwtService;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        String cleanEmail = request.getUsername() != null ? request.getUsername().trim().toLowerCase() : "";
        User user = userRepository.findByUsername(cleanEmail)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas: usuario no encontrado"));

        if (!user.isEnabled()) {
            throw new DisabledException("La cuenta de usuario se encuentra deshabilitada");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Credenciales incorrectas: contraseña incorrecta");
        }

        String token = jwtService.generateToken(user);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .dni(user.getDni())
                .nombres(user.getNombres())
                .apellidos(user.getApellidos())
                .telefono(user.getTelefono())
                .direccion(user.getDireccion())
                .fechaNacimiento(user.getFechaNacimiento())
                .genero(user.getGenero())
                .enabled(user.isEnabled())
                .roles(user.getRoles())
                .build();

        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }

    public String registrar(RegisterRequest request) {
        String cleanEmail = request.getUsername() != null ? request.getUsername().trim().toLowerCase() : "";

        if (userRepository.existsByUsername(cleanEmail)) {
            throw new IllegalArgumentException("El correo electrónico ya se encuentra registrado");
        }

        if (userRepository.existsByDni(request.getDni())) {
            throw new IllegalArgumentException("El DNI ya se encuentra registrado");
        }

        User nuevoUsuario = new User();
        nuevoUsuario.setUsername(cleanEmail);
        nuevoUsuario.setPassword(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setDni(request.getDni());
        nuevoUsuario.setNombres(request.getNombres());
        nuevoUsuario.setApellidos(request.getApellidos());
        nuevoUsuario.setTelefono(request.getTelefono());
        nuevoUsuario.setDireccion(request.getDireccion());
        nuevoUsuario.setGenero(request.getGenero());
        nuevoUsuario.setEnabled(true);

        if (request.getFechaNacimiento() != null && !request.getFechaNacimiento().isBlank()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date fecha = sdf.parse(request.getFechaNacimiento());
                nuevoUsuario.setFechaNacimiento(fecha);
            } catch (Exception e) {
                // Si falla el parseo, se deja null
            }
        }

        // Asignar rol ROLE_USER por defecto
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> {
                    Role nuevoRol = new Role();
                    nuevoRol.setName("ROLE_USER");
                    return roleRepository.save(nuevoRol);
                });

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        nuevoUsuario.setRoles(roles);

        userRepository.save(nuevoUsuario);

        return "Registro exitoso";
    }
}
