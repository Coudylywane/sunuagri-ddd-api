package sn.sunuagri.domaine.stock;

import java.util.List;
import java.util.Optional;
import sn.sunuagri.domaine.culture.TypeCulture;

/** Port sortant de persistance des lots de stock. */
public interface LotRepository {

    void sauvegarder(Lot lot);

    Optional<Lot> parId(String id);

    List<Lot> tous();

    List<Lot> parCultureEtQualite(TypeCulture typeCulture, Qualite qualite);

    List<Lot> parProducteur(String producteurId);
}
