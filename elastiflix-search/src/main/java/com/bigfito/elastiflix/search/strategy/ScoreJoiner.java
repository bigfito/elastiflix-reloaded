package com.bigfito.elastiflix.search.strategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/**
 * Junta, para cada película de la lista RRF, su puesto y su puntuación en las otras dos.
 */
@Component
public class ScoreJoiner {

    /**
     * Recorre la lista fusionada. Si una película no está en un componente, esa puntuación queda
     * vacía y la posición dice {@code fuera de su top } más {@code k}.
     *
     * @param fused   lista (a), ya ordenada por RRF
     * @param lexical lista (b)
     * @param vector  lista (c)
     * @param k       tamaño de cada lista
     * @return la lista (a) con las puntuaciones de las otras dos
     */
    public List<RankedHit> join(List<RankedHit> fused, List<RankedHit> lexical, List<RankedHit> vector, int k) {
        Places places = new Places(index(lexical), index(vector), k);
        List<RankedHit> joined = new ArrayList<>();
        for (RankedHit hit : fused) {
            joined.add(one(hit, places));
        }
        return joined;
    }

    private static RankedHit one(RankedHit fused, Places places) {
        String id = fused.movie().id();
        return new RankedHit(
                fused.movie(),
                fused.score(),
                place(places.lexical.get(id), places.k),
                place(places.vector.get(id), places.k),
                fused.score());
    }

    private static ComponentScore place(Indexed indexed, int k) {
        if (indexed == null) {
            return new ComponentScore(null, Rrf.OUTSIDE_PREFIX + k);
        }
        return new ComponentScore(indexed.hit().score(), Integer.toString(indexed.rank()));
    }

    private static Map<String, Indexed> index(List<RankedHit> hits) {
        Map<String, Indexed> byId = new LinkedHashMap<>();
        for (int position = 0; position < hits.size(); position++) {
            RankedHit hit = hits.get(position);
            byId.put(hit.movie().id(), new Indexed(hit, position + 1));
        }
        return byId;
    }

    private record Places(Map<String, Indexed> lexical, Map<String, Indexed> vector, int k) {
    }

    private record Indexed(RankedHit hit, int rank) {
    }
}
