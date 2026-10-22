package com.bigfito.elastiflix.core.catalog;

/**
 * Lo que pasó con un lote escrito por el alias en modo append: cuántos ítems aceptó Elasticsearch y cuántos
 * rechazó.
 *
 * <p>Un {@code _bulk} puede quedar a medias: los aceptados ya están en el catálogo aunque otros ítems del mismo lote
 * fallen. Por eso se cuentan los dos.</p>
 *
 * @param accepted ítems escritos
 * @param rejected ítems con error
 */
public record AppendResult(int accepted, int rejected) {

    /**
     * Dice si algún ítem del lote falló.
     *
     * @return {@code true} si hubo al menos un rechazo
     */
    public boolean hasRejections() {
        return rejected > 0;
    }
}
