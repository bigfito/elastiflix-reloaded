package com.bigfito.elastiflix.web.ingest;

import com.bigfito.elastiflix.ingest.job.JobView;

/**
 * Respuesta JSON del estado de un trabajo: los datos y sus textos para la página.
 *
 * @param job   copia del trabajo
 * @param texts textos con las cifras escritas como en el mockup
 */
public record JobScreen(JobView job, JobTexts texts) {
}
