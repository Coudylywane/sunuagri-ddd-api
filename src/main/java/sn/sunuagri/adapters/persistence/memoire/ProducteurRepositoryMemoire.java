package sn.sunuagri.adapters.persistence.memoire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import sn.sunuagri.domaine.commun.Telephone;
import sn.sunuagri.domaine.producteur.Producteur;
import sn.sunuagri.domaine.producteur.ProducteurRepository;

/**
 * Adaptateur de persistance en mémoire. Choisi pour ce rendu (ADR-002) :
 * les règles métier restent testables sans base, et un adaptateur JPA peut
 * remplacer cette classe sans toucher au domaine ni aux services.
 */
@Repository
public class ProducteurRepositoryMemoire implements ProducteurRepository {

    private final Map<String, Producteur> parId = new ConcurrentHashMap<>();

    @Override
    public void sauvegarder(Producteur producteur) {
        parId.put(producteur.id(), producteur);
    }

    @Override
    public Optional<Producteur> parId(String id) {
        return Optional.ofNullable(parId.get(id));
    }

    @Override
    public Optional<Producteur> parTelephone(Telephone telephone) {
        return parId.values().stream()
                .filter(p -> p.telephone().equals(telephone))
                .findFirst();
    }

    @Override
    public Optional<Producteur> parParcelleId(String parcelleId) {
        return parId.values().stream()
                .filter(p -> p.parcelle(parcelleId).isPresent())
                .findFirst();
    }

    @Override
    public List<Producteur> tous() {
        List<Producteur> resultat = new ArrayList<>(parId.values());
        resultat.sort(Comparator.comparing(Producteur::dateEnregistrement)
                .thenComparing(Producteur::nom)
                .thenComparing(Producteur::prenom));
        return resultat;
    }
}
