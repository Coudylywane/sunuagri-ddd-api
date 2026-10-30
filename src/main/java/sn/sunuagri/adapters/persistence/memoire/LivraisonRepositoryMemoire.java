package sn.sunuagri.adapters.persistence.memoire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import sn.sunuagri.domaine.stock.Livraison;
import sn.sunuagri.domaine.stock.LivraisonRepository;

@Repository
public class LivraisonRepositoryMemoire implements LivraisonRepository {

    private final Map<String, Livraison> parId = new ConcurrentHashMap<>();

    @Override
    public void sauvegarder(Livraison livraison) {
        parId.put(livraison.id(), livraison);
    }

    @Override
    public Optional<Livraison> parId(String id) {
        return Optional.ofNullable(parId.get(id));
    }

    @Override
    public List<Livraison> parRecolte(String recolteId) {
        return parId.values().stream()
                .filter(l -> l.recolteId().equals(recolteId))
                .sorted(Comparator.comparing(Livraison::date))
                .toList();
    }

    @Override
    public List<Livraison> toutes() {
        return new ArrayList<>(parId.values());
    }
}
