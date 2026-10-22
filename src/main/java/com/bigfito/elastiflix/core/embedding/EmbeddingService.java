package com.bigfito.elastiflix.core.embedding;

import java.util.List;

/**
 * Genera vectores de 1024 dimensiones para una consulta o para sinopsis.
 *
 * <p>Quien llama pasa el texto crudo. El servicio agrega el prefijo que corresponde.</p>
 */
public interface EmbeddingService {

    /**
     * Vectoriza una consulta con {@code input_type=search} y el prefijo {@code Query: }.
     *
     * @param text consulta, sin prefijo
     * @return vector de 1024 dimensiones
     */
    float[] embedQuery(String text);

    /**
     * Vectoriza sinopsis con la combinación calibrada, en lotes de 16.
     *
     * @param texts sinopsis crudas, en el orden que se quiere de vuelta
     * @return un vector de 1024 por cada texto, en el mismo orden
     */
    List<float[]> embedDocuments(List<String> texts);
}
