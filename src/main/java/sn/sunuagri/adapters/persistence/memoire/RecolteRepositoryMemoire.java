package sn.sunuagri.adapters.persistence.memoire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import sn.sunuagri.domaine.culture.Recolte;
import sn.sunuagri.domaine.culture.RecolteRepository;

@Repository
public class RecolteRepositoryMemoire implements RecolteRepository {

    private final Map<String, Recolte> parId = new ConcurrentHashMap<>();

    @Override
    public void sauvegarder(Recolte recolte) {
        parId.put(recolte.id(), recolte);
    }

    @Override
    public Optional<Recolte> parId(String id) {
        return Optional.ofNullable(parId.get(id));
    }

    @Override
    public List<Recolte> parCulture(String cultureId) {
        return parId.values().stream()
                .filter(r -> r.cultureId().equals(cultureId))
                .sorted(Comparator.comparing(Recolte::dateRecolte))
                .toList();
    }

    @Override
    public List<Recolte> parProducteur(String producteurId) {
        return parId.values().stream()
                .filter(r -> r.producteurId().equals(producteurId))
                .sorted(Comparator.comparing(Recolte::dateRecolte).reversed())
                .toList();
    }

    @Override
    public List<Recolte> toutes() {
        return new ArrayList<>(parId.values());
    }
}
