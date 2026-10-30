package sn.sunuagri.domaine.campagne;

import java.util.List;
import java.util.Optional;

/** Port sortant de persistance des campagnes (les cultures en font partie). */
public interface CampagneRepository {

    void sauvegarder(Campagne campagne);

    Optional<Campagne> parId(String id);

    Optional<Campagne> parSaisonEtAnnee(Saison saison, int annee);

    Optional<Campagne> parCultureId(String cultureId);

    List<Campagne> toutes();
}
