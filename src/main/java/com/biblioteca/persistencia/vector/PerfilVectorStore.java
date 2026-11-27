package com.biblioteca.persistencia.vector;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso a la tabla perfil_vector_usuario (embedding del perfil de lectura del usuario).
 */
@Repository
public class PerfilVectorStore {

    private final JdbcTemplate jdbc;

    public PerfilVectorStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Obtiene el vector de perfil del usuario (o null si no tiene). */
    public float[] obtenerPorUsuario(Long userId) {
        List<String> textos = jdbc.queryForList(
                "SELECT embedding::text FROM perfil_vector_usuario WHERE user_id = ? AND embedding IS NOT NULL",
                String.class, userId);
        if (textos.isEmpty()) {
            return null;
        }
        return VectorUtils.desdeTexto(textos.get(0));
    }

    /** Inserta o actualiza el vector de perfil del usuario. */
    public void guardar(Long userId, float[] embedding) {
        jdbc.update(
                "INSERT INTO perfil_vector_usuario (user_id, embedding) VALUES (?, ?::vector) "
                        + "ON CONFLICT (user_id) DO UPDATE SET embedding = EXCLUDED.embedding, fecha_actualizacion = CURRENT_TIMESTAMP",
                userId, VectorUtils.aTexto(embedding));
    }

    /** Elimina el perfil vectorial del usuario. */
    public void eliminar(Long userId) {
        jdbc.update("DELETE FROM perfil_vector_usuario WHERE user_id = ?", userId);
    }
}
