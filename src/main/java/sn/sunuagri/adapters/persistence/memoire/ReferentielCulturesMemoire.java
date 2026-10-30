package sn.sunuagri.adapters.persistence.memoire;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import sn.sunuagri.domaine.culture.CultureReference;
import sn.sunuagri.domaine.culture.ReferencesParDefaut;
import sn.sunuagri.domaine.culture.ReferentielCultures;
import sn.sunuagri.domaine.culture.TypeCulture;

@Repository
public class ReferentielCulturesMemoire implements ReferentielCultures {

    private final Map<TypeCulture, CultureReference> references = new ConcurrentHashMap<>();

    public ReferentielCulturesMemoire() {
        ReferencesParDefaut.toutes().forEach(r -> references.put(r.type(), r));
    }

    @Override
    public Optional<CultureReference> parType(TypeCulture type) {
        return Optional.ofNullable(references.get(type));
    }

    @Override
    public List<CultureReference> tous() {
        List<CultureReference> resultat = new ArrayList<>(references.values());
        resultat.sort(Comparator.comparing(r -> r.type().name()));
        return resultat;
    }

    @Override
    public void sauvegarder(CultureReference reference) {
        references.put(reference.type(), reference);
    }
}
