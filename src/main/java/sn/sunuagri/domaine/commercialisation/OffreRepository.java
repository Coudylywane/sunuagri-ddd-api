package sn.sunuagri.domaine.commercialisation;

import java.util.List;
import java.util.Optional;

/** Port sortant de persistance des offres. */
public interface OffreRepository {

    void sauvegarder(Offre offre);

    Optional<Offre> parId(String id);

    List<Offre> toutes();
}
