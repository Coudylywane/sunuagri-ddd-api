package sn.sunuagri.adapters.persistence.memoire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import sn.sunuagri.domaine.commercialisation.Commande;
import sn.sunuagri.domaine.commercialisation.CommandeRepository;

@Repository
public class CommandeRepositoryMemoire implements CommandeRepository {

    private final Map<String, Commande> parId = new ConcurrentHashMap<>();

    @Override
    public void sauvegarder(Commande commande) {
        parId.put(commande.id(), commande);
    }

    @Override
    public Optional<Commande> parId(String id) {
        return Optional.ofNullable(parId.get(id));
    }

    @Override
    public List<Commande> parAcheteur(String acheteurId) {
        return parId.values().stream()
                .filter(c -> c.acheteurId().equals(acheteurId))
                .sorted(Comparator.comparing(Commande::dateCreation)
                        .thenComparing(Commande::id).reversed())
                .toList();
    }

    @Override
    public List<Commande> toutes() {
        return new ArrayList<>(parId.values());
    }
}
