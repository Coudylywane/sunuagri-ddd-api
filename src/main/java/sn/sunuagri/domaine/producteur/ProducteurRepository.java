package sn.sunuagri.domaine.producteur;

import java.util.List;
import java.util.Optional;
import sn.sunuagri.domaine.commun.Telephone;

/** Port sortant de persistance de l'agrégat Producteur. */
public interface ProducteurRepository {

    void sauvegarder(Producteur producteur);

    Optional<Producteur> parId(String id);

    Optional<Producteur> parTelephone(Telephone telephone);

    Optional<Producteur> parParcelleId(String parcelleId);

    List<Producteur> tous();
}
