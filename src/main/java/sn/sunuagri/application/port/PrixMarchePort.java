package sn.sunuagri.application.port;

import java.util.Optional;
import sn.sunuagri.domaine.culture.TypeCulture;
import sn.sunuagri.domaine.marche.PrixMarche;

/** Port sortant vers le service externe de prix du marché. */
public interface PrixMarchePort {

    Optional<PrixMarche> prixActuel(TypeCulture typeCulture);
}
