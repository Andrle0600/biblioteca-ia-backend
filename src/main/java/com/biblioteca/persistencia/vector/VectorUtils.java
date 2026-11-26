package com.biblioteca.persistencia.vector;

import java.util.List;

/**
 * Utilidades para trabajar con vectores de embeddings (float[]),
 * incluida su representación textual de pgvector: [0.1,0.2,...]
 */
public final class VectorUtils {

    private VectorUtils() {
    }

    /** Convierte un vector a su representación textual de pgvector. */
    public static String aTexto(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }

    /** Convierte la representación textual de pgvector a float[]. */
    public static float[] desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.startsWith("[")) {
            limpio = limpio.substring(1);
        }
        if (limpio.endsWith("]")) {
            limpio = limpio.substring(0, limpio.length() - 1);
        }
        if (limpio.isBlank()) {
            return new float[0];
        }
        String[] partes = limpio.split(",");
        float[] resultado = new float[partes.length];
        for (int i = 0; i < partes.length; i++) {
            resultado[i] = Float.parseFloat(partes[i].trim());
        }
        return resultado;
    }

    /** Normaliza el vector a longitud 1 (norma L2). */
    public static float[] normalizar(float[] vector) {
        double suma = 0;
        for (float v : vector) {
            suma += (double) v * v;
        }
        double norma = Math.sqrt(suma);
        if (norma == 0) {
            return vector;
        }
        float[] resultado = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            resultado[i] = (float) (vector[i] / norma);
        }
        return resultado;
    }

    /** Promedio de varios vectores, normalizado. */
    public static float[] promedioNormalizado(List<float[]> vectores) {
        if (vectores == null || vectores.isEmpty()) {
            return null;
        }
        int dim = vectores.get(0).length;
        double[] suma = new double[dim];
        for (float[] v : vectores) {
            for (int i = 0; i < dim; i++) {
                suma[i] += v[i];
            }
        }
        float[] promedio = new float[dim];
        for (int i = 0; i < dim; i++) {
            promedio[i] = (float) (suma[i] / vectores.size());
        }
        return normalizar(promedio);
    }

    /**
     * Combina dos vectores: resultado = a * pesoA + b * (1 - pesoA), normalizado.
     * Si uno de los dos es null devuelve el otro normalizado.
     */
    public static float[] combinar(float[] a, float[] b, double pesoA) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            return normalizar(b);
        }
        if (b == null) {
            return normalizar(a);
        }
        double peso = Math.max(0, Math.min(1, pesoA));
        float[] resultado = new float[a.length];
        for (int i = 0; i < a.length; i++) {
            resultado[i] = (float) (a[i] * peso + b[i] * (1 - peso));
        }
        return normalizar(resultado);
    }
}
