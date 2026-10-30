package sn.sunuagri.adapters.persistence.memoire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import sn.sunuagri.domaine.commercialisation.PaiementDu;
import sn.sunuagri.domaine.commercialisation.PaiementDuRepository;

@Repository
public class PaiementDuRepositoryMemoire implements PaiementDuRepository {

    private final Map<String, PaiementDu> parId = new ConcurrentHashMap<>();

    @Override
    public void sauvegarder(PaiementDu paiement) {
        parId.put(paiement.id(), paiement);
    }

    @Override
    public Optional<PaiementDu> parId(String id) {
        return Optional.ofNullable(parId.get(id));
    }

    @Override
    public List<PaiementDu> parCommande(String commandeId) {
        return parId.values().stream()
                .filter(p -> p.commandeId().equals(commandeId))
                .sorted(Comparator.comparing(PaiementDu::id))
                .toList();
    }

    @Override
    public List<PaiementDu> parProducteur(String producteurId) {
        return parId.values().stream()
                .filter(p -> p.producteurId().equals(producteurId))
                .sorted(Comparator.comparing(PaiementDu::dateCreation).reversed())
                .toList();
    }

    @Override
    public List<PaiementDu> tous() {
        return new ArrayList<>(parId.values());
    }
}
