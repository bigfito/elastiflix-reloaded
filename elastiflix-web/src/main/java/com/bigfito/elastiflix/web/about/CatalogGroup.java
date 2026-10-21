package com.bigfito.elastiflix.web.about;

import java.util.List;

/**
 * Un grupo de campos del catálogo, con el mismo título que su sección del formulario de «Cargar datos».
 *
 * @param titleKey clave del título del grupo
 * @param fields   campos del grupo, en el orden en que se muestran
 */
public record CatalogGroup(String titleKey, List<CatalogField> fields) {

    /**
     * Copia la lista para que nadie la cambie después.
     */
    public CatalogGroup {
        fields = List.copyOf(fields);
    }
}
