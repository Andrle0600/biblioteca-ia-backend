package com.biblioteca.persistencia.vector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Acceso a la tabla libro_vector (embeddings de libros con pgvector).
 * Se usa JdbcTemplate porque Hibernate no soporta el tipo vector de PostgreSQL.
 */
@Repository
public class LibroVectorStore {

    private final JdbcTemplate jdbc;

    public LibroVectorStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserta o actualiza el embedding de un libro. */
    public void guardar(Long libroId, float[] embedding) {
        jdbc.update(
                "INSERT INTO libro_vector (libro_id, embedding) VALUES (?, ?::vector) "
                        + "ON CONFLICT (libro_id) DO UPDATE SET embedding = EXCLUDED.embedding, fecha_actualizacion = CURRENT_TIMESTAMP",
                libroId, VectorUtils.aTexto(embedding));
    }

    /** Obtiene el embedding de un libro (o null si no existe). */
    public float[] obtenerPorLibro(Long libroId) {
        List<String> textos = jdbc.queryForList(
                "SELECT embedding::text FROM libro_vector WHERE libro_id = ? AND embedding IS NOT NULL",
                String.class, libroId);
        if (textos.isEmpty()) {
            return null;
        }
        return VectorUtils.desdeTexto(textos.get(0));
    }

    /** Obtiene los embeddings de varios libros. */
    public List<LibroEmbedding> obtenerEmbeddings(List<Long> libroIds) {
        if (libroIds == null || libroIds.isEmpty()) {
            return List.of();
        }
        String ids = libroIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        return jdbc.query(
                "SELECT libro_id, embedding::text FROM libro_vector WHERE libro_id IN (" + ids + ") AND embedding IS NOT NULL",
                (rs, rowNum) -> new LibroEmbedding(rs.getLong(1), VectorUtils.desdeTexto(rs.getString(2))));
    }

    /**
     * Busca los libros más similares (distancia coseno) al vector de consulta.
     * - excluirIds: libros que no se deben sugerir (p. ej. ya reservados por el usuario)
     * - soloDisponibles: si es true, solo devuelve libros con al menos un ejemplar DISPONIBLE
     */
    public List<LibroSimilar> buscarSimilares(float[] consulta, int limite, List<Long> excluirIds, boolean soloDisponibles) {
        String vector = VectorUtils.aTexto(consulta);

        StringBuilder sql = new StringBuilder(
                "SELECT lv.libro_id, l.titulo, l.autor, l.genero, l.descripcion, "
                        + "1 - (lv.embedding <=> ?::vector) AS similitud "
                        + "FROM libro_vector lv JOIN libro l ON l.id = lv.libro_id "
                        + "WHERE lv.embedding IS NOT NULL ");
        List<Object> parametros = new ArrayList<>();
        parametros.add(vector);

        if (excluirIds != null && !excluirIds.isEmpty()) {
            // Los ids provienen de la base de datos (tipo Long), no hay riesgo de inyección
            String ids = excluirIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            sql.append(" AND lv.libro_id NOT IN (").append(ids).append(") ");
        }
        if (soloDisponibles) {
            sql.append(" AND EXISTS (SELECT 1 FROM ejemplar e WHERE e.libro_id = lv.libro_id AND e.estado = 'DISPONIBLE') ");
        }
        sql.append(" ORDER BY lv.embedding <=> ?::vector LIMIT ? ");
        parametros.add(vector);
        parametros.add(limite);

        return jdbc.query(sql.toString(),
                (rs, rowNum) -> new LibroSimilar(
                        rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                        rs.getDouble(6)),
                parametros.toArray());
    }

    /** Libro candidato con su similitud respecto a la consulta. */
    public record LibroSimilar(Long id, String titulo, String autor, String genero, String descripcion, double similitud) {
    }

    /** Libro con su embedding. */
    public record LibroEmbedding(Long libroId, float[] embedding) {
    }
}
