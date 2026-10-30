package sn.sunuagri.adapters.persistence.memoire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.stock.Lot;
import sn.sunuagri.domaine.stock.LotRepository;
import sn.sunuagri.domaine.stock.Qualite;

@Repository
public class LotRepositoryMemoire implements LotRepository {

    private final Map<String, Lot> parId = new ConcurrentHashMap<>();

    @Override
    public void sauvegarder(Lot lot) {
        parId.put(lot.id(), lot);
    }

    @Override
    public Optional<Lot> parId(String id) {
        return Optional.ofNullable(parId.get(id));
    }

    @Override
    public List<Lot> tous() {
        return new ArrayList<>(parId.values());
    }

    /** Du plus ancien au plus récent, pour la consommation FIFO de US-13. */
    @Override
    public List<Lot> parCultureEtQualite(TypeCulture typeCulture, Qualite qualite) {
        return parId.values().stream()
                .filter(l -> l.typeCulture() == typeCulture && l.qualite() == qualite)
                .sorted(Comparator.comparing(Lot::dateReception).thenComparing(Lot::id))
                .toList();
    }

    @Override
    public List<Lot> parProducteur(String producteurId) {
        return parId.values().stream()
                .filter(l -> l.producteurId().equals(producteurId))
                .sorted(Comparator.comparing(Lot::dateReception))
                .toList();
    }
}
