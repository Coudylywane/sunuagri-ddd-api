package sn.sunuagri.adapters.persistence.memoire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import sn.sunuagri.domaine.campagne.Campagne;
import sn.sunuagri.domaine.campagne.CampagneRepository;
import sn.sunuagri.domaine.campagne.Saison;

@Repository
public class CampagneRepositoryMemoire implements CampagneRepository {

    private final Map<String, Campagne> parId = new ConcurrentHashMap<>();

    @Override
    public void sauvegarder(Campagne campagne) {
        parId.put(campagne.id(), campagne);
    }

    @Override
    public Optional<Campagne> parId(String id) {
        return Optional.ofNullable(parId.get(id));
    }

    @Override
    public Optional<Campagne> parSaisonEtAnnee(Saison saison, int annee) {
        return parId.values().stream()
                .filter(c -> c.saison() == saison && c.annee() == annee)
                .findFirst();
    }

    @Override
    public Optional<Campagne> parCultureId(String cultureId) {
        return parId.values().stream()
                .filter(c -> c.culture(cultureId).isPresent())
                .findFirst();
    }

    @Override
    public List<Campagne> toutes() {
        List<Campagne> resultat = new ArrayList<>(parId.values());
        resultat.sort(Comparator.comparingInt(Campagne::annee).thenComparing(c -> c.saison().name()));
        return resultat;
    }
}
