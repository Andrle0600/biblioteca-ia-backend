package com.biblioteca.dto;

import java.util.Date;
import java.util.Set;

import com.biblioteca.model.Role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String dni;
    private String nombres;
    private String apellidos;
    private String telefono;
    private String direccion;
    private Date fechaNacimiento;
    private String genero;
    private boolean enabled;
    private Set<Role> roles;
}
