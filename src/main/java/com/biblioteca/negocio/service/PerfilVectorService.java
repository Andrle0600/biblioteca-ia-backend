package com.biblioteca.negocio.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.biblioteca.persistencia.entity.Reserva;
import com.biblioteca.persistencia.entity.User;
import com.biblioteca.persistencia.repository.ReservaLibroRepository;
import com.biblioteca.persistencia.vector.LibroVectorStore;
import com.biblioteca.persistencia.vector.PerfilVectorStore;
import com.biblioteca.persistencia.vector.VectorUtils;

/**
 * Mantiene el perfil vectorial de lectura de cada usuario:
 * el promedio normalizado de los embeddings de los libros que ha reservado.
 * Se guarda en la tabla perfil_vector_usuario (pgvector).
 */
@Service
public class PerfilVectorService {

    @Autowired
    private ReservaLibroRepository reservaRepository;

    @Autowired
    private LibroVectorStore libroVectorStore;

    @Autowired
    private PerfilVectorStore perfilVectorStore;

    /** Recalcula y guarda el perfil vectorial del usuario a partir de sus reservas. */
    public void actualizarPerfil(User usuario) {
        List<Reserva> reservas = reservaRepository.findByUser(usuario);
        Set<Long> libroIds = new LinkedHashSet<>();
        for (Reserva reserva : reservas) {
            if (reserva.getEjemplar() != null && reserva.getEjemplar().getLibro() != null) {
                libroIds.add(reserva.getEjemplar().getLibro().getId());
            }
        }

        if (libroIds.isEmpty()) {
            perfilVectorStore.eliminar(usuario.getId());
            return;
        }

        List<float[]> vectores = new ArrayList<>();
        for (LibroVectorStore.LibroEmbedding embedding : libroVectorStore.obtenerEmbeddings(new ArrayList<>(libroIds))) {
            vectores.add(embedding.embedding());
        }

        if (vectores.isEmpty()) {
            // Los libros aún no tienen embeddings generados
            perfilVectorStore.eliminar(usuario.getId());
            return;
        }

        float[] perfil = VectorUtils.promedioNormalizado(vectores);
        perfilVectorStore.guardar(usuario.getId(), perfil);
    }
}
