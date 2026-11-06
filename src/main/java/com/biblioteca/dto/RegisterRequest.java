package com.biblioteca.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String dni;
    private String nombres;
    private String apellidos;
    private String fechaNacimiento;
    private String genero;
    private String username;
    private String telefono;
    private String direccion;
    private String password;
}
