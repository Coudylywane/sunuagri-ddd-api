package sn.sunuagri.domaine.culture;

import java.util.List;
import java.util.Optional;

/** Port sortant du référentiel des cultures (US-19). */
public interface ReferentielCultures {

    Optional<CultureReference> parType(TypeCulture type);

    List<CultureReference> tous();

    void sauvegarder(CultureReference reference);
}
