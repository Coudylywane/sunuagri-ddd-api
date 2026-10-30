package sn.sunuagri.adapters.persistence.memoire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import sn.sunuagri.domaine.commercialisation.Offre;
import sn.sunuagri.domaine.commercialisation.OffreRepository;

@Repository
public class OffreRepositoryMemoire implements OffreRepository {

    private final Map<String, Offre> parId = new ConcurrentHashMap<>();

    @Override
    public void sauvegarder(Offre offre) {
        parId.put(offre.id(), offre);
    }

    @Override
    public Optional<Offre> parId(String id) {
        return Optional.ofNullable(parId.get(id));
    }

    @Override
    public List<Offre> toutes() {
        List<Offre> resultat = new ArrayList<>(parId.values());
        resultat.sort(Comparator.comparing(Offre::id));
        return resultat;
    }
}
