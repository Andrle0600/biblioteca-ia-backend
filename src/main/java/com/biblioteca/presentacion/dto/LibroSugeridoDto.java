package com.biblioteca.presentacion.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LibroSugeridoDto {
    private Long id;
    private String titulo;
    private String autor;
    private String genero;
    private double similitud;
}
