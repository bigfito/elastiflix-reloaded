package com.bigfito.elastiflix.ingest.job;

/**
 * Recibe el avance de una importación. No conoce la página.
 */
public interface ImportListener {

    /**
     * Avisa el paso y los conteos actuales.
     *
     * @param progress paso, lote, vectorizados y escritos
     */
    void onProgress(ImportProgress progress);
}
