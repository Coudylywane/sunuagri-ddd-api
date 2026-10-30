package sn.sunuagri.domaine.culture;

import java.util.List;
import java.util.Optional;

/** Port sortant de persistance des récoltes. */
public interface RecolteRepository {

    void sauvegarder(Recolte recolte);

    Optional<Recolte> parId(String id);

    List<Recolte> parCulture(String cultureId);

    List<Recolte> parProducteur(String producteurId);

    List<Recolte> toutes();
}
